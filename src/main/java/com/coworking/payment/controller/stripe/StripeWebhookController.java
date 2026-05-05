package com.coworking.payment.controller.stripe;


import com.coworking.reservation.service.ReservationService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class StripeWebhookController {
    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    private final ReservationService reservationService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request){
        String payload;
        try {
            payload = new String(request.getInputStream().readAllBytes());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Invalid payload");
        }

        String sigHeader = request.getHeader("Stripe-Signature");

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Fallo la verificacion");
        }

        System.out.println("Webhook recibido: " + event.getType());

        //MANEJO DE EVENTOS

        switch (event.getType()){
           /* case "checkout.session.completed":
                handleCheckoutSessionCompleted(event);
                break;
            */
            case "payment_intent.succeeded":
                handlePaymentIntentSucceeded(event);
                break;

            default:
                System.out.println("Unhandled event: " + event.getType());
        }

        return ResponseEntity.ok("Recibido");
    }

    private void handlePaymentIntentSucceeded(Event event) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        Optional<StripeObject> object = dataObjectDeserializer.getObject();

        if (object.isPresent()) {
            PaymentIntent paymentIntent =
                    (PaymentIntent) object.get();

            String reservationId = paymentIntent.getMetadata().get("reservationId");

            if (reservationId != null) {
                reservationService.markAsPaid(Long.parseLong(reservationId));
            }
        }

    }

    private void handleCheckoutSessionCompleted(Event event) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

        Optional<StripeObject> object = dataObjectDeserializer.getObject();

        if (object.isPresent()){
            Session session = (Session) object.get();
            String reservationId = session.getMetadata().get("reservationId");

            if (reservationId != null){
                reservationService.markAsPaid(Long.parseLong(reservationId));
            }
        }
    }


}
