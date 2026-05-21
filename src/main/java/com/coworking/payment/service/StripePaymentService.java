package com.coworking.payment.service;

import com.coworking.exception.BadRequestException;
import com.coworking.exception.NotFoundException;
import com.coworking.payment.client.StripeClient;
import com.coworking.payment.dto.PaymentResponse;
import com.coworking.payment.enums.PaymentStatus;
import com.coworking.payment.model.Payment;
import com.coworking.payment.repository.PaymentRepository;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.repository.ReservationRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StripePaymentService implements PaymentService {

    private final ReservationRepository reservationRepository;
    private final StripeClient stripeClient;
    private final PaymentRepository paymentRepository;

    private PaymentResponse mapToResponse(Payment payment){
        return PaymentResponse.builder()
                .id(payment.getId())
                .reservationId(payment.getReservation().getId())
                .roomName(
                        payment.getReservation().getRoom() != null
                                ? payment.getReservation().getRoom().getName()
                                :null
                        )
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paymentMethod("stripe")
                .paidAt(payment.getPaidAt())
                .build();
    }

    @Override
    public String createPaymentIntent(Long reservationId, String userEmail) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reserva no encontrada"));

        // validar cliente
        if (!reservation.getUser().getEmail().equals(userEmail)) {
            throw new BadRequestException("No autorizado para pagar esta reserva");
        }

        // validar estado
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BadRequestException("La reserva ya fue procesada");
        }

        try {

            // evitar múltiples PaymentIntent
            if (reservation.getStripePaymentIntentId() != null) {

                PaymentIntent existing =
                        PaymentIntent.retrieve(
                                reservation.getStripePaymentIntentId()
                        );

                return existing.getClientSecret();
            }

            PaymentIntent paymentIntent = stripeClient.createPaymentIntent(
                    reservation.getPrice(),
                    reservation.getId()
            );

            reservation.setStripePaymentIntentId(paymentIntent.getId());

            reservationRepository.save(reservation);

            return paymentIntent.getClientSecret();

        } catch (StripeException e) {
            throw new RuntimeException("Error procesando pago", e);
        }
    }

    @Override
    @Transactional
    public void registerSuccessfulPayment(PaymentIntent paymentIntent) {
        Long reservationId = Long.parseLong(
                paymentIntent.getMetadata().get("reservationId")
        );

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reserva no encontrada"));

        // evitar duplicados webhook
        if (paymentRepository.findByStripePaymentIntentId(paymentIntent.getId()).isPresent()) {
            return;
        }

        Payment payment = new Payment();

        payment.setReservation(reservation);

        payment.setStripePaymentIntentId(paymentIntent.getId());

        payment.setAmount(
                BigDecimal.valueOf(paymentIntent.getAmount())
                        .divide(BigDecimal.valueOf(100))
        );

        payment.setCurrency(paymentIntent.getCurrency());

        payment.setPaidAt(Instant.now());

        payment.setStatus(PaymentStatus.SUCCEEDED);
        paymentRepository.save(payment);
        reservation.setStatus(ReservationStatus.PAID);
        reservationRepository.save(reservation);

    }

    @Override
    public List<PaymentResponse> getMyPayments(String email) {
        return paymentRepository
                .findByReservationUserEmailOrderByPaidAtDesc(email)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
}