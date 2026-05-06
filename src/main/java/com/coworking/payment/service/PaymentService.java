package com.coworking.payment.service;


public interface PaymentService {
    String createPaymentIntent(Long reservationId, String userEmail);
}
