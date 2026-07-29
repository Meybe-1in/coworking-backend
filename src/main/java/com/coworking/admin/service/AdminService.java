package com.coworking.admin.service;

import com.coworking.admin.dto.*;
import com.coworking.admin.enums.ChartPeriod;
import com.coworking.payment.dto.PaymentResponse;
import com.coworking.reservation.dto.ReservationResponse;

import java.util.List;

public interface AdminService {

    // Dashboard
    AdminStatsResponse getStats();

    // Reservas
    AdminPageResponse<ReservationResponse> getReservations(
            int page,
            int size
    );

    // Pagos
    AdminPageResponse<PaymentResponse> getPayments(
            int page,
            int size
    );

    // Usuarios
    AdminPageResponse<UserAdminResponse> getUsers(
            int page,
            int size
    );

    // Administración de reservas
    void cancelReservation(Long reservationId);

    //Crear usuario administrador
    UserAdminResponse createAdmin(CreateAdminRequest request);

    // Actualizar estado de usuario
    UserAdminResponse updateUserStatus(Long userId, UpdateUserStatusRequest request);

    // Actualizar rol de usuario
    UserAdminResponse updateUserRole(Long userId, UpdateUserRoleRequest request);

    // Perfil autenticado
    AdminProfileResponse getProfile();

    // Chart
    List<ChartPointResponse> getReservationsChart(
            ChartPeriod period
    );
}
