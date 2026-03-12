package com.coworking.service;


import com.coworking.dto.CalendarEventResponse;
import com.coworking.dto.ReservationRequest;
import com.coworking.dto.ReservationResponse;
import com.coworking.exception.ReservationConflictException;
import com.coworking.model.Reservation;
import com.coworking.model.ReservationStatus;
import com.coworking.model.Room;
import com.coworking.model.User;
import com.coworking.repository.ReservationRepository;
import com.coworking.repository.RoomRepository;
import com.coworking.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public ReservationResponse createReservation(Long userId, ReservationRequest request){
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Sala no encontrada"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        LocalDateTime start = request.getStartAt();
        LocalDateTime end = request.getEndAt();

        validateReservationTimes(start, end);

        //verificacion de cruce de horarios

        List<Reservation> overlapping =reservationRepository
                .findByRoomIdAndStartAtLessThanAndEndAtGreaterThan(room.getId(), end, start);

        if (!overlapping.isEmpty())
            throw  new ReservationConflictException("La sala ya está reservada en ese horario");

        // verificacion de reserva duplicada
        if (reservationRepository.findByUserIdAndRoomIdAndStartAtAndEndAt(user.getId(), room.getId(), start , end).isPresent())
            throw new ReservationConflictException("Ya tienes una reserva igual");


        //crear reserva
        Reservation reservation = new Reservation();
        reservation.setRoom(room);
        reservation.setUser(user);
        reservation.setStartAt(start);
        reservation.setEndAt(end);
        reservation.setStatus(ReservationStatus.CONFIRMED);

        Reservation saved = reservationRepository.save(reservation);
        return mapToResponse(saved);
    }

    private void validateReservationTimes(LocalDateTime start, LocalDateTime end){
        LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

        // evitar horas pasadas
        if (start.isBefore(now)) {
            throw new ReservationConflictException("No puedes reservar horas pasadas");
        }

        // inicio debe ser antes que fin
        if (!start.isBefore(end)) {
            throw new ReservationConflictException("La hora de inicio debe ser anterior a la de fin");
        }

        // horario permitido
        if (start.toLocalTime().isBefore(LocalTime.of(7,0)) ||
                end.toLocalTime().isAfter(LocalTime.of(20,0))) {

            throw new ReservationConflictException("Las reservas deben estar entre 07:00 y 20:00");
        }

        // duración máxima
        if (Duration.between(start, end).toHours() > 8) {
            throw new ReservationConflictException("La duración máxima es 8 horas");
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
        return dto;
    }

    public List<ReservationResponse> getAllReservations(){
        return  reservationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void deleteReservation(Long id){
        if (!reservationRepository.existsById(id))
            throw new RuntimeException("Reserva no encontrada");

        reservationRepository.deleteById(id);
    }

    //ver eventos en calendario
    public List<CalendarEventResponse> getCalendar(
            LocalDateTime from,
            LocalDateTime to
    ){
        return reservationRepository
                .findByStartAtLessThanAndEndAtGreaterThan(to, from)
                .stream()
                .map(r -> new CalendarEventResponse(
                         r.getRoom().getName(),
                         r.getStartAt(),
                         r.getEndAt()
                ))
                .collect(Collectors.toList());
    }
}
