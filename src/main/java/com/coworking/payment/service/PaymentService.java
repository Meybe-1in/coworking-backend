package com.coworking.payment.service;

import com.coworking.payment.dto.PaymentResponse;
import com.stripe.model.PaymentIntent;

import java.util.List;

public interface PaymentService {
    String createPaymentIntent(Long reservationId, String userEmail);

    void registerSuccessfulPayment(PaymentIntent paymentIntent);
    List<PaymentResponse> getMyPayments(String email);

}
