package com.coworking.admin.service;

import com.coworking.admin.dto.AdminStatsResponse;
import com.coworking.payment.dto.PaymentResponse;
import com.coworking.reservation.dto.ReservationResponse;

import java.util.List;

public interface AdminService {

    AdminStatsResponse getStats();

    List<ReservationResponse> getAllReservations();

    List<PaymentResponse> getAllPayments();

    void cancelReservation(Long reservationId);
}
