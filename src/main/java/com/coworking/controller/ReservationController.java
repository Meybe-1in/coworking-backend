package com.coworking.controller;

import com.coworking.dto.ReservationRequest;
import com.coworking.dto.ReservationResponse;
import com.coworking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservas", description = "Gestión de reservas de salas")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Crear una reserva (USER o ADMIN)")
    public ReservationResponse create(@Valid @RequestBody ReservationRequest request, Authentication auth) {
        // auth.getName() nos da el username del usuario autenticado
        Long userId = 1L; // 🔹 Temporal, luego lo obtendremos del userRepository usando auth.getName()
        return reservationService.createReservation(userId, request);
    }

    @GetMapping
    @Operation(summary = "Listar todas las reservas")
    public List<ReservationResponse> getAll() {
        return reservationService.getAllReservations();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una reserva (solo ADMIN o dueño)")
    public void delete(@PathVariable Long id) {
        reservationService.deleteReservation(id);
    }
}

