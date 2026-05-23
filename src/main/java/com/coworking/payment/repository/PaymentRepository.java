package com.coworking.payment.repository;

import com.coworking.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByStripePaymentIntentId(String paymentIntentId);

    boolean existsByReservationId(Long reservation);

    List<Payment> findByReservationUserEmailOrderByPaidAtDesc(String email);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.status = 'SUCCEEDED'
            """)
    BigDecimal getTotalRevenue();

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.status = 'SUCCEEDED'
            AND MONTH(p.paidAt) = MONTH(CURRENT_DATE)
            AND YEAR(p.paidAt) = YEAR(CURRENT_DATE)
            """)
    BigDecimal getMonthlyRevenue();


}
