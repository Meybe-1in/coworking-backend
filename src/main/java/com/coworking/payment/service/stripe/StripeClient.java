package com.coworking.payment.service.stripe;

import java.math.BigDecimal;

public interface StripeClient {
    String createPaymentIntent(BigDecimal amount, Long reservationId);
}