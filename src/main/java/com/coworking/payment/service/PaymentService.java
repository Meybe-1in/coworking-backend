package com.coworking.payment.service;

import com.stripe.model.PaymentIntent;

public interface PaymentService {
    String createPaymentIntent(Long reservationId, String userEmail);

    void registerSuccessfulPayment(PaymentIntent paymentIntent);
}
