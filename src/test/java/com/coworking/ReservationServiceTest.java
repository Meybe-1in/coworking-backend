package com.coworking;

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
import com.coworking.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
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

    @InjectMocks
    private ReservationService reservationService;

    private User user;
    private Room room;
    private ReservationRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = new User();
        user.setId(1L);
        user.setUsername("dayana");

        room = new Room();
        room.setId(2L);
        room.setName("Sala A");

        request = new ReservationRequest();
        request.setRoomId(room.getId());
        request.setStartAt(LocalDateTime.of(2025, 10, 1, 10, 0));
        request.setEndAt(LocalDateTime.of(2025, 10, 1, 12, 0));
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

        Reservation saved = new Reservation();
        saved.setId(99L);
        saved.setRoom(room);
        saved.setUser(user);
        saved.setStartAt(request.getStartAt());
        saved.setEndAt(request.getEndAt());
        saved.setStatus(ReservationStatus.CONFIRMED);

        when(reservationRepository.save(any(Reservation.class))).thenReturn(saved);

        // When
        ReservationResponse response = reservationService.createReservation(user.getId(), request);

        // Then
        assertNotNull(response);
        assertEquals("Sala A", response.getRoomName());
        assertEquals("dayana", response.getUsername());
        assertEquals(ReservationStatus.CONFIRMED, response.getStatus());
        verify(reservationRepository, times(1)).save(any(Reservation.class));
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
}
