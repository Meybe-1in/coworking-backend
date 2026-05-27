package com.coworking.admin.service;

import com.coworking.admin.dto.AdminStatsResponse;
import com.coworking.exception.NotFoundException;
import com.coworking.payment.dto.PaymentResponse;
import com.coworking.payment.model.Payment;
import com.coworking.payment.repository.PaymentRepository;
import com.coworking.reservation.dto.ReservationResponse;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public AdminStatsResponse getStats() {

        long totalReservations = reservationRepository.count();

        long activeReservations =
                reservationRepository.countByStatus(ReservationStatus.PAID);

        long pendingReservations =
                reservationRepository.countByStatus(ReservationStatus.PENDING);

        long cancelledReservations =
                reservationRepository.countByStatus(ReservationStatus.CANCELLED);

        long expiredReservations =
                reservationRepository.countByStatus(ReservationStatus.EXPIRED);

        return AdminStatsResponse.builder()
                .totalReservations(totalReservations)
                .activeReservations(activeReservations)
                .pendingReservations(pendingReservations)
                .cancelledReservations(cancelledReservations)
                .expiredReservations(expiredReservations)
                .totalRevenue(paymentRepository.getTotalRevenue())
                .monthlyRevenue(paymentRepository.getMonthlyRevenue())
                .build();
    }

    @Override
    public List<ReservationResponse> getAllReservations() {

        return reservationRepository.findAll()
                .stream()
                .map(this::mapReservationToResponse)
                .toList();
    }

    @Override
    public List<PaymentResponse> getAllPayments() {

        return paymentRepository.findAll()
                .stream()
                .map(this::mapPaymentToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void cancelReservation(Long reservationId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() ->
                        new NotFoundException("Reserva no encontrada")
                );
        reservation.setStatus(ReservationStatus.CANCELLED);

        reservationRepository.save(reservation);

    }

    // MAPPERS

    private ReservationResponse mapReservationToResponse(Reservation reservation) {

        ReservationResponse response = new ReservationResponse();

        response.setId(reservation.getId());
        response.setRoomName(reservation.getRoom().getName());
        response.setUsername(reservation.getUser().getUsername());
        response.setStartAt(reservation.getStartAt());
        response.setEndAt(reservation.getEndAt());
        response.setPrice(reservation.getPrice());
        response.setCreatedAt(reservation.getCreatedAt());
        response.setStatus(reservation.getStatus());

        return response;
    }

    private PaymentResponse mapPaymentToResponse(Payment payment) {

        return PaymentResponse.builder()
                .id(payment.getId())
                .reservationId(payment.getReservation().getId())
                .roomName(payment.getReservation().getRoom().getName())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .build();
    }
}