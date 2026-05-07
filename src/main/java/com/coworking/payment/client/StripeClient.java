package com.coworking.payment.client;

import java.math.BigDecimal;

public interface StripeClient {
    String createPaymentIntent(BigDecimal amount, Long reservationId);
}