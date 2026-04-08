package com.coworking.resources.service;

import com.coworking.reservation.dto.ReservationRequest;
import com.coworking.reservation.dto.ReservationResponse;
import com.coworking.exception.ReservationConflictException;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.model.ReservationStatus;
import com.coworking.room.model.Room;
import com.coworking.user.model.User;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.room.repository.RoomRepository;
import com.coworking.user.repository.UserRepository;
import com.coworking.reservation.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private ReservationService reservationService;

    private User user;
    private Room room;
    private ReservationRequest request;

    private Instant elSalvadorTime(int hour) {
        return ZonedDateTime.of(
                2025, 1, 1, hour, 0, 0, 0,
                ZoneId.of("America/El_Salvador")
        ).toInstant();
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = new User();
        user.setId(1L);
        user.setEmail("p1@email.com");

        room = new Room();
        room.setId(2L);
        room.setName("Sala A");
        room.setPrice(10.0);

        when(clock.instant()).thenReturn(Instant.parse("2025-01-01T10:00:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        request = new ReservationRequest();
        request.setRoomId(room.getId());
        request.setStartAt(elSalvadorTime(9)); // 09:00 AM local
        request.setEndAt(elSalvadorTime(11));   // 11:00 AM local
    }

    @Test
    void createReservation_success() {
        // Given
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(reservationRepository.findByRoomIdAndStartAtLessThanAndEndAtGreaterThan(
                anyLong(), any(), any())
        ).thenReturn(Collections.emptyList());
        when(reservationRepository.findByUserIdAndRoomIdAndStartAtAndEndAt(
                anyLong(), anyLong(), any(), any())
        ).thenReturn(Optional.empty());

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0)); // devuelve lo guardado

        ReservationResponse response =
                reservationService.createReservation(user.getId(), request);

        Reservation saved = new Reservation();
        saved.setId(99L);
        saved.setRoom(room);
        saved.setUser(user);
        saved.setStartAt(request.getStartAt());
        saved.setEndAt(request.getEndAt());
        saved.setStatus(ReservationStatus.PENDING);
        saved.setPrice(20.0);

        assertNotNull(response);
        assertEquals("Sala A", response.getRoomName());
        assertEquals("p1@email.com", response.getUsername());
        assertEquals(ReservationStatus.PENDING, response.getStatus());

        verify(reservationRepository).save(any(Reservation.class));
    }
    @Test
    void createReservation_shouldCalculatePrice() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(reservationRepository.findByRoomIdAndStartAtLessThanAndEndAtGreaterThan(
                anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(reservationRepository.findByUserIdAndRoomIdAndStartAtAndEndAt(
                anyLong(), anyLong(), any(), any()))
                .thenReturn(Optional.empty());

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response =
                reservationService.createReservation(user.getId(), request);

        verify(reservationRepository).save(captor.capture());

        Reservation saved = captor.getValue();

        // 2 horas * 10 = 20
        assertEquals(20.0, saved.getPrice());
        assertEquals(20.0, response.getPrice());
    }

    @Test
    void createReservation_conflictOverlap_throwsException() {
        // Given
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        List<Reservation> overlapping = List.of(new Reservation());
        when(reservationRepository.findByRoomIdAndStartAtLessThanAndEndAtGreaterThan(
                anyLong(), any(), any())
        ).thenReturn(overlapping);

        // When + Then
        assertThrows(ReservationConflictException.class,
                () -> reservationService.createReservation(user.getId(), request));
    }

    @Test
    void createReservation_duplicateReservation_throwsException() {
        // Given
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(reservationRepository.findByRoomIdAndStartAtLessThanAndEndAtGreaterThan(
                anyLong(), any(), any())
        ).thenReturn(Collections.emptyList());
        when(reservationRepository.findByUserIdAndRoomIdAndStartAtAndEndAt(
                anyLong(), anyLong(), any(), any())
        ).thenReturn(Optional.of(new Reservation()));

        // When + Then
        assertThrows(ReservationConflictException.class,
                () -> reservationService.createReservation(user.getId(), request));
    }
    //Validacion de horario invalido
    @Test
    void createReservation_invalidHour_throwsException() {
        // 04:00 AM en El Salvador (fuera de horario permitido)
        request.setStartAt(elSalvadorTime(5));
        request.setEndAt(elSalvadorTime(6));

        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThrows(ReservationConflictException.class,
                () -> reservationService.createReservation(user.getId(), request));
    }

    //delete
    @Test
    void deleteReservation_success() {
        when(reservationRepository.existsById(1L)).thenReturn(true);

        reservationService.deleteReservation(1L);

        verify(reservationRepository).deleteById(1L);
    }
}
