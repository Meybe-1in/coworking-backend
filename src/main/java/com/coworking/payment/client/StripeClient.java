package com.coworking.payment.client;

import com.stripe.model.PaymentIntent;

import java.math.BigDecimal;

public interface StripeClient {
    PaymentIntent createPaymentIntent(BigDecimal amount, Long reservationId);
}