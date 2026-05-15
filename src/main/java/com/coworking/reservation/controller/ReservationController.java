package com.coworking.reservation.controller;

import com.coworking.reservation.dto.CalendarEventResponse;
import com.coworking.reservation.dto.ReservationRequest;
import com.coworking.reservation.dto.ReservationResponse;
import com.coworking.user.repository.UserRepository;
import com.coworking.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.time.Instant;
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
    public ReservationResponse create(@Valid @RequestBody ReservationRequest request, Authentication authentication) {
        // auth.getName() nos da el username del usuario autenticado
      String username = authentication.getName();
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

    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponse>> getMyReservations( Authentication authentication){
        return ResponseEntity.ok(
                reservationService.getMyReservations(
                        authentication.getName()
                )
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una reserva (solo ADMIN)")
    public void delete(@PathVariable Long id) {
        reservationService.deleteReservation(id);
    }

    ///endpoint calendario de reservas(read only)
    @GetMapping("/calendar")
    @Operation(summary = "Calendario reservas por sala")
    public List<CalendarEventResponse> getCalendar(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to
    ){
        return reservationService.getCalendar(from, to);
    }
}

