package com.coworking.payment.webhook;

import com.coworking.reservation.service.ReservationService;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    @Value("${webhook.webhook-secret}")
    private String webhookSecret;

    private final ReservationService reservationService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request) {
        String payload;
        try {
            payload = new String(request.getInputStream().readAllBytes());
        } catch (IOException e) {
            log.error("Error leyendo payload", e);
            return ResponseEntity.badRequest().body("Invalid payload");
        }

        String sigHeader = request.getHeader("Stripe-Signature");

        Event event;
        try {
            log.info("Webhook secret usado: {}", webhookSecret);
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            log.error("Error validando firma de Stripe", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Fallo la verificacion");
        }

        log.info("Webhook recibido: {}", event.getType());

        try {
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded(event);
                    break;

                default:
                    log.warn("Evento no manejado: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Error procesando evento de Stripe", e);
        }

        return ResponseEntity.ok("Recibido");
    }

    private void handlePaymentIntentSucceeded(Event event) throws EventDataObjectDeserializationException {

        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

        StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);

        if (stripeObject == null) {
            stripeObject = dataObjectDeserializer.deserializeUnsafe();
        }

        PaymentIntent paymentIntent = (PaymentIntent) stripeObject;

        if (paymentIntent.getMetadata() == null ||
                !paymentIntent.getMetadata().containsKey("reservationId")) {

            log.warn("PaymentIntent sin reservationId en metadata");
            return;
        }

        String reservationId = paymentIntent.getMetadata().get("reservationId");

        try {
            Long id = Long.parseLong(reservationId);
            reservationService.markAsPaid(id);
            log.info("Reserva {} marcada como PAID", id);

        } catch (NumberFormatException e) {
            log.error("reservationId inválido: {}", reservationId, e);
        }
    }
}