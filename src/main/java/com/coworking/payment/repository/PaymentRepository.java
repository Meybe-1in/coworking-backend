package com.coworking.payment.repository;

import com.coworking.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByStripePaymentIntentId(String paymentIntentId);

    boolean existsByReservationId(Long reservation);

    List<Payment> findByReservationUserEmailOrderByPaidAtDesc(String email);
}
