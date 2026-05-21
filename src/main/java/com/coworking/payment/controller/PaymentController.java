package com.coworking.payment.controller;

import com.coworking.payment.dto.PaymentResponse;
import com.coworking.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/intent/{reservationId}")
    public ResponseEntity<Map<String, String>> createPaymentIntent(@PathVariable Long reservationId, Authentication authentication){
        String email = authentication.getName();

        String clientSecret= paymentService.createPaymentIntent(reservationId, email);

        return ResponseEntity.ok(Map.of(
                "clientSecret", clientSecret
        ));
    }

    @GetMapping("/my-history")
    public ResponseEntity<List<PaymentResponse>> getMyPayments( Authentication authentication){
        return ResponseEntity.ok(
                paymentService.getMyPayments(
                        authentication.getName()
                )
        );
    }

}
