package com.coworking.admin.service;

import com.coworking.admin.dto.AdminStatsResponse;
import com.coworking.admin.dto.UserAdminResponse;
import com.coworking.payment.dto.PaymentResponse;
import com.coworking.reservation.dto.ReservationResponse;

import java.util.List;

public interface AdminService {

    // Dashboard
    AdminStatsResponse getStats();

    // Reservas
    List<ReservationResponse> getAllReservations();

    // Pagos
    List<PaymentResponse> getAllPayments();

    // Usuarios
    List<UserAdminResponse> getAllUsers();

    // Administración de reservas
    void cancelReservation(Long reservationId);
}
