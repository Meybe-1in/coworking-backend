package com.coworking.payment.service;

import com.coworking.exception.BadRequestException;
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
    public String createPaymentIntent(Long reservationId, String userEmail) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reserva no encontrada"));

        //validar cliente
        if (!reservation.getUser().getEmail().equals(userEmail)) {
            throw new BadRequestException("No autorizado para pagar esta reserva");
        }

        //Validar estado
        if (reservation.getStatus() != ReservationStatus.PENDING){
            throw new BadRequestException("La reserva ya fue procesada");
        }

        return stripeClient.createPaymentIntent(
                reservation.getPrice(),
                reservation.getId()
        );
    }
}
