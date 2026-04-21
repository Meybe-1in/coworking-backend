package com.coworking.resources.service.payment;

import com.coworking.payment.service.PaymentService;
import com.coworking.payment.service.StripePaymentService;
import com.coworking.payment.service.stripe.StripeClient;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StripePaymentServiceTest {

    private ReservationRepository reservationRepository;
    private StripeClient stripeClient;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        reservationRepository = Mockito.mock(ReservationRepository.class);
        stripeClient = Mockito.mock(StripeClient.class);

        paymentService = new StripePaymentService(
                reservationRepository,
                stripeClient
        );
    }

    @Test
    void shouldCreatePaymentIntent() {

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setPrice(BigDecimal.valueOf(10));

        Mockito.when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        Mockito.when(stripeClient.createPaymentIntent(Mockito.any(), Mockito.any()))
                .thenReturn("fake_client_secret");

        String clientSecret = paymentService.createPaymentIntent(1L);

        assertNotNull(clientSecret);
    }
}