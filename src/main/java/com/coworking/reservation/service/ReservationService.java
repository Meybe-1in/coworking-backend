package com.coworking.reservation.service;


import com.coworking.admin.settings.entity.SystemSettings;
import com.coworking.admin.settings.service.SystemSettingsService;
import com.coworking.exception.BadRequestException;
import com.coworking.exception.ErrorCode;
import com.coworking.exception.NotFoundException;
import com.coworking.reservation.dto.CalendarEventResponse;
import com.coworking.reservation.dto.ReservationRequest;
import com.coworking.reservation.dto.ReservationResponse;
import com.coworking.exception.ReservationConflictException;
import com.coworking.reservation.enums.ErrorCodeReservation;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.room.model.Room;
import com.coworking.user.model.User;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.room.repository.RoomRepository;
import com.coworking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final Clock clock;
    private final SystemSettingsService systemSettingsService;

    @Transactional
    public ReservationResponse createReservation(Long userId, ReservationRequest request) {
        //buscar sala
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new NotFoundException("Sala no encontrada"));
        //buscar usuario
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Instant start = request.getStartAt();
        Instant end = request.getEndAt();

        SystemSettings settings =
                systemSettingsService.getCurrentSettings();

        validateReservationTimes(start, end, settings);

        // Estados que actualmente pueden bloquear el horario
        List<ReservationStatus> blockingStatuses = List.of(
                ReservationStatus.PAID,
                ReservationStatus.PENDING
        );

        // Bloqueo pesimista y búsqueda de reservas que se solapan
        List<Reservation> overlappingReservations =
                reservationRepository.findOverlappingForUpdate(
                        room.getId(),
                        start,
                        end,
                        blockingStatuses
                );

        // Una reserva PENDING solamente bloquea mientras
        // no haya superado su tiempo de expiración.
        boolean exists = overlappingReservations.stream()
                .anyMatch(reservation ->
                        isBlockingReservation(
                                reservation,
                                settings
                        )
                );

        if (exists) {
            throw new ReservationConflictException(
                    ErrorCode.RESERVATION_OVERLAP.name(),
                    "La sala ya está reservada en ese horario"
            );
        }

        // verificar reserva duplicada
        boolean duplicateReservation =
                reservationRepository
                        .findByUserIdAndRoomIdAndStartAtAndEndAtAndStatusIn(
                                user.getId(),
                                room.getId(),
                                start,
                                end,
                                blockingStatuses
                        )
                        .filter(reservation ->
                                isBlockingReservation(
                                        reservation,
                                        settings
                                )
                        )
                        .isPresent();

        if (duplicateReservation) {
            throw new ReservationConflictException(
                    ErrorCode.DUPLICATE_RESERVATION.name(),
                    "Ya tienes una reserva igual"
            );
        }

        // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
        //                     CALCULO DE PRECIO
        // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
        //duración en minutos
        long minutes = Duration.between(start, end).toMinutes();

        if (minutes <= 0) {
            throw new ReservationConflictException(
                    ErrorCode.INVALID_DURATION.name(),
                    "Duración inválida");
        }

        // convertir minutos a horas con precisión
        BigDecimal hours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        // precio total = precio por hora * horas
        BigDecimal totalPrice = room.getPrice().multiply(hours);

        // redondear a 2 decimales (dinero)
        totalPrice = totalPrice.setScale(2, RoundingMode.HALF_UP);

        //crear reserva
        Reservation reservation = new Reservation();
        reservation.setRoom(room);
        reservation.setUser(user);
        reservation.setStartAt(start);
        reservation.setEndAt(end);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setPrice(totalPrice);
        reservation.setNotes(request.getNotes());


        Reservation saved = reservationRepository.save(reservation);
        return mapToResponse(saved);
    }

    private boolean isBlockingReservation(
            Reservation reservation,
            SystemSettings settings
    ) {
        if (reservation.getStatus() == ReservationStatus.PAID) {
            return true;
        }

        if (reservation.getStatus() == ReservationStatus.PENDING) {

            Instant expirationTime = reservation.getCreatedAt()
                    .plus(
                            Duration.ofMinutes(
                                    settings.getPendingExpirationMinutes()
                            )
                    );

            return Instant.now(clock).isBefore(expirationTime);
        }

        return false;
    }

    private void validateReservationTimes(
            Instant startAt,
            Instant endAt,
            SystemSettings settings
    ) {

        Instant now = Instant.now(clock);

        if (startAt.isBefore(now)) {
            throw new BadRequestException(
                    "La hora de inicio no puede estar en el pasado"
            );
        }

        if (!startAt.isBefore(endAt)) {
            throw new BadRequestException(
                    "La hora de inicio debe ser anterior a la hora de finalización"
            );
        }

        ZoneId zoneId =
                ZoneId.of("America/El_Salvador");

        LocalTime openingTime = settings.getOpeningTime();
        LocalTime closingTime = settings.getClosingTime();
        LocalTime startLocal = startAt.atZone(zoneId).toLocalTime();
        LocalTime endLocal = endAt.atZone(zoneId).toLocalTime();

        LocalDate startDate = startAt.atZone(zoneId).toLocalDate();
        LocalDate endDate = endAt.atZone(zoneId).toLocalDate();

        if (!startDate.equals(endDate)) {
            throw new BadRequestException(
                    "La reserva debe iniciar y finalizar el mismo día"
            );
        }

        if (startLocal.isBefore(openingTime) || !startLocal.isBefore(closingTime)) {
            throw new BadRequestException(
                    "La reserva debe iniciar dentro del horario permitido"
            );
        }

        if (endLocal.isAfter(closingTime) || !endLocal.isAfter(openingTime)) {
            throw new BadRequestException(
                    "La reserva debe finalizar dentro del horario permitido"
            );
        }

        long reservationMinutes =
                Duration.between(startAt, endAt).toMinutes();

        long maxReservationMinutes =
                settings.getMaxReservationHours() * 60L;

        if (reservationMinutes > maxReservationMinutes) {
            throw new BadRequestException(
                    "La duración de la reserva supera el máximo permitido"
            );
        }
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        ReservationResponse dto = new ReservationResponse();
        dto.setId(reservation.getId());
        dto.setRoomName(reservation.getRoom().getName());
        dto.setUsername(reservation.getUser().getEmail());
        dto.setStartAt(reservation.getStartAt());
        dto.setEndAt(reservation.getEndAt());
        dto.setStatus(reservation.getStatus());
        dto.setCreatedAt(reservation.getCreatedAt());
        dto.setPrice(reservation.getPrice());
        return dto;
    }

    //Obtener todas las reservaciones
    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    //Obtener mi reserva
    public List<ReservationResponse> getMyReservations(String email) {
        return reservationRepository
                .findByUserEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    //Eliminar reservacion por id
    public void deleteReservation(Long id) {
        if (!reservationRepository.existsById(id))
            throw new RuntimeException("Reserva no encontrada");

        reservationRepository.deleteById(id);
    }

    //ver eventos en calendario
    public List<CalendarEventResponse> getCalendar(
            Instant from,
            Instant to
    ) {
        SystemSettings settings =
                systemSettingsService.getCurrentSettings();

        List<Reservation> reservations =
                reservationRepository.findCalendarReservations(
                        List.of(
                                ReservationStatus.PENDING,
                                ReservationStatus.PAID
                        ),
                        from,
                        to
                );

        return reservations.stream()
                .filter(reservation ->
                        isBlockingReservation(
                                reservation,
                                settings
                        )
                )
                .map(r -> new CalendarEventResponse(
                        r.getRoom().getName(),
                        r.getStartAt(),
                        r.getEndAt(),
                        r.getRoom().getId()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsPaid(long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservacion no encontrada"));

        if (reservation.getStatus() == ReservationStatus.PAID) {
            return; //evitar procesar
        }

        //evitar pagar reservas canceladas o expiradas
        if (reservation.getStatus() == ReservationStatus.CANCELLED ||
                reservation.getStatus() == ReservationStatus.EXPIRED) {
            throw new ReservationConflictException(
                    ErrorCodeReservation.RESERVATION_EXPIRED.name(),
                    "La reservación ya no está disponible"
            );
        }

        // validar expiración según configuración del sistema
        SystemSettings settings =
                systemSettingsService.getCurrentSettings();

        Instant expirationTime =
                reservation.getCreatedAt()
                        .plus(
                                Duration.ofMinutes(
                                        settings.getPendingExpirationMinutes()
                                )
                        );

        if (Instant.now(clock).isAfter(expirationTime)) {
            reservation.setStatus(ReservationStatus.EXPIRED);

            reservationRepository.save(reservation);

            throw new ReservationConflictException(
                    ErrorCodeReservation.RESERVATION_EXPIRED.name(),
                    "La reservación expiró"
            );
        }

        reservation.setStatus(ReservationStatus.PAID);

        reservationRepository.save(reservation);
    }

    @Transactional
    public void cancelReservation(Long reservationId, String email) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() ->
                        new NotFoundException("Reservación no encontrada")
                );

        //validar usuario

        if (!reservation.getUser().getEmail().equals(email)) {
            throw new ReservationConflictException(
                    ErrorCodeReservation.UNAUTHORIZED_ACTION.name(),
                    "No puedes cancelar esta reservación"
            );
        }

        //evitar cancelar ya cancelada
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ReservationConflictException(
                    ErrorCodeReservation.RESERVATION_ALREADY_CANCELLED.name(),
                    "La reservación ya fue cancelada"
            );
        }

        //Evitar cancelar reservaciones iniciadas
        if (reservation.getStartAt().isBefore(Instant.now(clock))) {
            throw new ReservationConflictException(
                    ErrorCodeReservation.RESERVATION_ALREADY_STARTED.name(),
                    "No puedes cancelar una reservación iniciada"
            );
        }

        //evitar cancelar reservas pagadas
        if (reservation.getStatus() == ReservationStatus.PAID) {
            throw new ReservationConflictException(
                    ErrorCodeReservation.UNAUTHORIZED_ACTION.name(),
                    "No puedes cancelar una reservación pagada"
            );
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }
}
