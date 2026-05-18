package com.coworking.resources.service.payment;

import com.coworking.exception.NotFoundException;
import com.coworking.payment.client.StripeClient;
import com.coworking.payment.model.Payment;
import com.coworking.payment.repository.PaymentRepository;
import com.coworking.payment.service.PaymentService;
import com.coworking.payment.service.StripePaymentService;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.user.model.User;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StripePaymentServiceTest {

    private ReservationRepository reservationRepository;

    private StripeClient stripeClient;

    private PaymentRepository paymentRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {

        reservationRepository = Mockito.mock(ReservationRepository.class);

        stripeClient = Mockito.mock(StripeClient.class);

        paymentRepository = Mockito.mock(PaymentRepository.class);

        paymentService = new StripePaymentService(
                reservationRepository,
                stripeClient,
                paymentRepository
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

        PaymentIntent paymentIntent = new PaymentIntent();

        paymentIntent.setId("pi_test");

        paymentIntent.setClientSecret("fake_client_secret");

        Mockito.when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        Mockito.when(stripeClient.createPaymentIntent(Mockito.any(), Mockito.any()))
                .thenReturn(paymentIntent);

        String clientSecret =
                paymentService.createPaymentIntent(1L, "test@mail.com");

        assertNotNull(clientSecret);

        assertEquals("fake_client_secret", clientSecret);

        Mockito.verify(stripeClient, Mockito.times(1))
                .createPaymentIntent(BigDecimal.valueOf(10), 1L);
    }

    @Test
    void shouldThrowExceptionWhenReservationNotFound() {

        Mockito.when(reservationRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            paymentService.createPaymentIntent(1L, "test@mail.com");
        });

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

    @Test
    void shouldRegisterSuccessfulPayment() {

        Reservation reservation = new Reservation();

        reservation.setId(1L);

        PaymentIntent paymentIntent = new PaymentIntent();

        paymentIntent.setId("pi_123");

        paymentIntent.setAmount(1000L);

        paymentIntent.setCurrency("usd");

        HashMap<String, String> metadata = new HashMap<>();

        metadata.put("reservationId", "1");

        paymentIntent.setMetadata(metadata);

        Mockito.when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        Mockito.when(paymentRepository
                        .findByStripePaymentIntentId("pi_123"))
                .thenReturn(Optional.empty());

        paymentService.registerSuccessfulPayment(paymentIntent);

        Mockito.verify(paymentRepository)
                .save(Mockito.any(Payment.class));

        Mockito.verify(reservationRepository)
                .save(reservation);

        assertEquals(ReservationStatus.PAID, reservation.getStatus());
    }
}