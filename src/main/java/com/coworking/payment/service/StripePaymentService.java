package com.coworking.payment.service;

import com.coworking.exception.NotFoundException;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.model.ReservationStatus;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.payment.service.stripe.StripeClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StripePaymentService implements PaymentService {

    private final ReservationRepository reservationRepository;
    private final StripeClient stripeClient;

    @Override
    public String createPaymentIntent(Long reservationId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reserva no encontrada"));

        //Validar estado
        if (reservation.getStatus() != ReservationStatus.PENDING){
            throw new RuntimeException("La reserva ya fue pagada o cancelada");
        }

        return stripeClient.createPaymentIntent(
                reservation.getPrice(),
                reservation.getId()
        );
    }
}
