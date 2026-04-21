package com.coworking.payment.service;

import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StripePaymentService implements PaymentService{

    private final ReservationRepository reservationRepository;

    @Override
    public String createPaymentIntent(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        try{
            //multiplicar por 100 ya que stripe usa centavos
            long amount = reservation.getPrice()
                    .multiply(java.math.BigDecimal.valueOf(100))
                    .longValue();

            PaymentIntentCreateParams params =
                    PaymentIntentCreateParams.builder()
                            .setAmount(amount)
                            .setCurrency("usd")
                            .putMetadata("reservationId", reservation.getId().toString())
                            .build();

            PaymentIntent intent = PaymentIntent.create(params);

            return intent.getClientSecret();

        } catch (StripeException e) {
            throw new RuntimeException("Error creando PaymentIntent" ,e);
        }
    }
}
