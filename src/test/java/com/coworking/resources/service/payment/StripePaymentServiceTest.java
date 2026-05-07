package com.coworking.resources.service.payment;

import com.coworking.exception.NotFoundException;
import com.coworking.payment.service.PaymentService;
import com.coworking.payment.service.StripePaymentService;
import com.coworking.payment.service.stripe.StripeClient;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.model.ReservationStatus;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.user.model.User;
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

        User user = new User();
        user.setEmail("test@mail.com");

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setPrice(BigDecimal.valueOf(10));
        reservation.setUser(user);
        reservation.setStatus(ReservationStatus.PENDING);

        Mockito.when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        Mockito.when(stripeClient.createPaymentIntent(Mockito.any(), Mockito.any()))
                .thenReturn("fake_client_secret");

        String clientSecret = paymentService.createPaymentIntent(1L, "test@mail.com");

        assertNotNull(clientSecret);

        Mockito.verify(stripeClient, Mockito.times(1))
                .createPaymentIntent(BigDecimal.valueOf(10), 1L);
    }

    @Test
    void shouldThrowExceptionWhenReservationNotFound() {

        // Arrange
        Mockito.when(reservationRepository.findById(1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> {
            paymentService.createPaymentIntent(1L, "test@mail.com");
        });

        // stripe nunca debe llamarse
        Mockito.verifyNoInteractions(stripeClient);
    }

    @Test
    void shouldThrowExceptionWhenStripeFails() {

        User user = new User();
        user.setEmail("test@mail.com");

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setPrice(BigDecimal.valueOf(10));
        reservation.setUser(user);
        reservation.setStatus(ReservationStatus.PENDING);

        Mockito.when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        Mockito.when(stripeClient.createPaymentIntent(Mockito.any(), Mockito.any()))
                .thenThrow(new RuntimeException("Stripe error"));

        assertThrows(RuntimeException.class, () -> {
            paymentService.createPaymentIntent(1L, "test@mail.com");
        });

        Mockito.verify(stripeClient)
                .createPaymentIntent(BigDecimal.valueOf(10), 1L);
    }
}