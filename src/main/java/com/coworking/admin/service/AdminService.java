package com.coworking.admin.service;

import com.coworking.admin.dto.*;
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

    //Crear usuario administrador
    UserAdminResponse createAdmin(CreateAdminRequest request);

    // Actualizar estado de usuario
    UserAdminResponse updateUserStatus(Long userId, UpdateUserStatusRequest request);
}
