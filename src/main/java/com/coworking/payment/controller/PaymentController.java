package com.coworking.payment.controller;

import com.coworking.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/intent/{reservationId}")
    public String createPaymentIntent(@PathVariable Long reservationId){
        return paymentService.createPaymentIntent(reservationId);
    }
}
