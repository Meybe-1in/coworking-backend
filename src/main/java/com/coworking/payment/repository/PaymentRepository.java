package com.coworking.payment.repository;

import com.coworking.payment.enums.PaymentStatus;
import com.coworking.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.Instant;

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

    //Consultas para graficas

    @Query("""
            SELECT FUNCTION('DATE', p.paidAt), SUM(p.amount)
            FROM Payment p
            WHERE p.status = 'SUCCEEDED'
            AND p.paidAt BETWEEN :start AND :end
            GROUP BY FUNCTION('DATE', p.paidAt)
            ORDER BY FUNCTION('DATE', p.paidAt)
            """)
    List<Object[]> getRevenueGroupedByDay(
            Instant start,
            Instant end
    );

    @Query("""
            SELECT EXTRACT(MONTH FROM p.paidAt), SUM(p.amount)
            FROM Payment p
            WHERE p.status = 'SUCCEEDED'
            AND EXTRACT(YEAR FROM p.paidAt) = EXTRACT(YEAR FROM CURRENT_DATE)
            GROUP BY EXTRACT(MONTH FROM p.paidAt)
            ORDER BY EXTRACT(MONTH FROM p.paidAt)
            """)
    List<Object[]> getRevenueGroupedByMonthCurrentYear();

    List<Payment> findTop8ByStatusOrderByPaidAtDesc(
            PaymentStatus status
    );

}
