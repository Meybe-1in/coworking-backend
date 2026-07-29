package com.coworking.admin.controller;


import com.coworking.admin.dto.*;
import com.coworking.admin.service.AdminDashboardService;
import com.coworking.admin.service.AdminService;
import com.coworking.dto.common.ApiResponseDto;
import com.coworking.payment.dto.PaymentResponse;
import com.coworking.reservation.dto.ReservationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "http:/localhost:5173")
public class AdminController {

    private final AdminService adminService;
    private final AdminDashboardService dashboardService;

    //                      Dashboard stats
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // Devuelve métricas generales para el dashboard administrativo
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(
                dashboardService.getStats()
        );
    }

    //                     All reservations
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // Obtiene todas las reservas registradas en el sistema
    @GetMapping("/reservations")
    public ResponseEntity<AdminPageResponse<ReservationResponse>> getReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                adminService.getReservations(page, size)
        );
    }

    //                       All payments
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // Obtiene todos los pagos registrados
    @GetMapping("/payments")
    public ResponseEntity<AdminPageResponse<PaymentResponse>> getPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                adminService.getPayments(page, size)
        );
    }

    //                   Cancel reservation
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // Permite cancelar una reserva desde el panel administrativo
    @PatchMapping("/reservations/{id}/cancel")
    public ResponseEntity<ApiResponseDto<String>> cancelReservation(@PathVariable Long id) {
        adminService.cancelReservation(id);

        return ResponseEntity.ok(
                new ApiResponseDto<>(
                        true,
                        "Reserva cancelada correctamente",
                        null
                )
        );
    }

    //                         All Users
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // Obtiene todos los usuarios registrados para administración
    @GetMapping("/users")
    public ResponseEntity<AdminPageResponse<UserAdminResponse>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                adminService.getUsers(page, size)
        );
    }

    //                         create Admin
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // Permite a un administrador crear nuevas cuentas administrativas
    @Operation(summary = "Crear administrador", description = "Permite registrar una nueva cuenta con rol ADMIN"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Administrador creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o usuario duplicado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    @PostMapping("/users/admin")
    public ResponseEntity<UserAdminResponse> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        return ResponseEntity.ok(
                adminService.createAdmin(request)
        );
    }

    //                         Update User Status
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // permite a un administrador actualizar estado de usuario
    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Actualizar estado de usuario", description = "Activa o desactiva una cuenta de usuario")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Estado actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    })
    public ResponseEntity<UserAdminResponse> updateUserStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(
                adminService.updateUserStatus(id, request)
        );
    }

    //                         Update User Role
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // permite a un administrador actualizar rol de usuario
    @PatchMapping("/users/{id}/role")
    @Operation(
            summary = "Actualizar rol de usuario",
            description = "Asigna un nuevo rol a un usuario"
    )
    public ResponseEntity<UserAdminResponse> updateUserRole(@PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request) {
        return ResponseEntity.ok(
                adminService.updateUserRole(id, request)
        );
    }

    //                         Get Profile
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // Perfil autenticado
    @GetMapping("/profile")
    public ResponseEntity<AdminProfileResponse> getProfile() {
        return ResponseEntity.ok(
                adminService.getProfile()
        );
    }

}
