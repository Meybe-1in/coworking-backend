package com.coworking.admin.controller;


import com.coworking.admin.dto.*;
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

    //                      Dashboard stats
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // Devuelve métricas generales para el dashboard administrativo
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(
                adminService.getStats()
        );
    }

    //                     All reservations
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // Obtiene todas las reservas registradas en el sistema
    @GetMapping("/reservations")
    public ResponseEntity<List<ReservationResponse>> getReservations() {
        return ResponseEntity.ok(
                adminService.getAllReservations()
        );
    }

    //                       All payments
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // Obtiene todos los pagos registrados
    @GetMapping("/payments")
    public ResponseEntity<List<PaymentResponse>> getPayments() {
        return ResponseEntity.ok(
                adminService.getAllPayments()
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
    public ResponseEntity<List<UserAdminResponse>> getUsers() {
        return ResponseEntity.ok(
                adminService.getAllUsers()
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
    @PatchMapping("/user/{is}/status")
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

}
