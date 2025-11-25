package com.coworking.controller;

import com.coworking.dto.ReservationRequest;
import com.coworking.dto.ReservationResponse;
import com.coworking.repository.UserRepository;
import com.coworking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Reservas", description = "Gestión de reservas de salas")
public class ReservationController {

    private final ReservationService reservationService;
    private final UserRepository userRepository;

    public ReservationController(ReservationService reservationService, UserRepository userRepository) {
        this.reservationService = reservationService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @Operation(summary = "Crear una reserva (USER o ADMIN)")
    public ReservationResponse create(@Valid @RequestBody ReservationRequest request, Authentication auth) {
        // auth.getName() nos da el username del usuario autenticado
      String username = auth.getName();
      Long userId = userRepository.findByEmail(username)
              .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username))
              .getId();

    return reservationService.createReservation(userId, request);
    }

    @GetMapping
    @Operation(summary = "Listar todas las reservas")
    public List<ReservationResponse> getAll() {
        return reservationService.getAllReservations();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una reserva (solo ADMIN)")
    public void delete(@PathVariable Long id) {
        reservationService.deleteReservation(id);
    }
}

