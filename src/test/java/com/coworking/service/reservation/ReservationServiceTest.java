package com.coworking.service.reservation;

import com.coworking.admin.settings.entity.SystemSettings;
import com.coworking.admin.settings.service.SystemSettingsService;
import com.coworking.exception.BadRequestException;
import com.coworking.exception.NotFoundException;
import com.coworking.reservation.dto.ReservationRequest;
import com.coworking.reservation.dto.ReservationResponse;
import com.coworking.exception.ReservationConflictException;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.enums.ReservationStatus;
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

import java.math.BigDecimal;
import java.time.*;
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

    @Mock
    private SystemSettingsService systemSettingsService;

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
        room.setPrice(BigDecimal.valueOf(10.0));

        SystemSettings settings = new SystemSettings();
        settings.setOpeningTime(java.time.LocalTime.of(7, 0));
        settings.setClosingTime(java.time.LocalTime.of(20, 0));
        settings.setMaxReservationHours(8);
        settings.setPendingExpirationMinutes(15);
        settings.setInstitutionName("Coworking Platform");

        when(systemSettingsService.getCurrentSettings())
                .thenReturn(settings);

        when(clock.instant())
                .thenReturn(Instant.parse("2025-01-01T10:00:00Z"));

        when(clock.getZone())
                .thenReturn(ZoneId.systemDefault());

        request = new ReservationRequest();
        request.setRoomId(room.getId());
        request.setStartAt(elSalvadorTime(9));
        request.setEndAt(elSalvadorTime(11));
    }

    @Test
    void createReservation_success() {

        when(roomRepository.findById(room.getId()))
                .thenReturn(Optional.of(room));

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        when(reservationRepository.findOverlappingForUpdate(
                anyLong(), any(), any())
        ).thenReturn(Collections.emptyList());

        when(reservationRepository.findByUserIdAndRoomIdAndStartAtAndEndAt(
                anyLong(), anyLong(), any(), any())
        ).thenReturn(Optional.empty());

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response =
                reservationService.createReservation(
                        user.getId(),
                        request
                );

        assertNotNull(response);
        assertEquals("Sala A", response.getRoomName());
        assertEquals("p1@email.com", response.getUsername());
        assertEquals(
                ReservationStatus.PENDING,
                response.getStatus()
        );

        verify(reservationRepository)
                .save(any(Reservation.class));
    }

    @Test
    void save_status_string() {
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.PAID);

        assertEquals(ReservationStatus.PAID, reservation.getStatus());
    }

    @Test
    void createReservation_shouldCalculatePrice() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        when(reservationRepository.findOverlappingForUpdate(
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
        assertEquals(new BigDecimal("20.00"), saved.getPrice());
        assertEquals(new BigDecimal("20.00"), response.getPrice());
    }

    @Test
    void createReservation_conflictOverlap_throwsException() {
        // Given
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        when(reservationRepository.findOverlappingForUpdate(
                anyLong(), any(), any()
        )).thenReturn(Collections.emptyList());

        when(reservationRepository.existsByRoomIdAndStartAtLessThanAndEndAtGreaterThan(
                anyLong(), any(), any()
        )).thenReturn(true);

        // When + Then
        assertThrows(ReservationConflictException.class,
                () -> reservationService.createReservation(user.getId(), request));
    }

    @Test
    void createReservation_duplicateReservation_throwsException() {
        // Given
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        when(reservationRepository.findOverlappingForUpdate(
                anyLong(), any(), any())
        ).thenReturn(Collections.emptyList());

        when(reservationRepository.findByUserIdAndRoomIdAndStartAtAndEndAt(
                anyLong(), anyLong(), any(), any())
        ).thenReturn(Optional.of(new Reservation()));

        // When + Then
        assertThrows(ReservationConflictException.class,
                () -> reservationService.createReservation(user.getId(), request));
    }

    @Test
    void createReservation_shouldCallPessimisticLock() {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        when(reservationRepository.findOverlappingForUpdate(
                anyLong(), any(), any()
        )).thenReturn(Collections.emptyList());

        when(reservationRepository.existsByRoomIdAndStartAtLessThanAndEndAtGreaterThan(
                anyLong(), any(), any()
        )).thenReturn(false);

        when(reservationRepository.findByUserIdAndRoomIdAndStartAtAndEndAt(
                anyLong(), anyLong(), any(), any()
        )).thenReturn(Optional.empty());

        when(reservationRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        reservationService.createReservation(user.getId(), request);

        verify(reservationRepository).findOverlappingForUpdate(
                eq(room.getId()), any(), any()
        );
    }

    //Validacion de horario invalido
    @Test
    void createReservation_invalidHour_throwsException() {

        // 05:00 AM en El Salvador (fuera de horario permitido)
        request.setStartAt(elSalvadorTime(5));
        request.setEndAt(elSalvadorTime(6));

        when(roomRepository.findById(room.getId()))
                .thenReturn(Optional.of(room));

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        assertThrows(
                BadRequestException.class,
                () -> reservationService.createReservation(
                        user.getId(),
                        request
                )
        );
    }

    //delete
    @Test
    void deleteReservation_success() {
        when(reservationRepository.existsById(1L)).thenReturn(true);

        reservationService.deleteReservation(1L);

        verify(reservationRepository).deleteById(1L);
    }


    //webhook paid

    @Test
    void shouldMarkReservationAsPaid() {

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setCreatedAt(
                Instant.parse("2025-01-01T09:50:00Z")
        );

        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        reservationService.markAsPaid(1L);

        assertEquals(
                ReservationStatus.PAID,
                reservation.getStatus()
        );

        verify(reservationRepository)
                .save(reservation);
    }

    @Test
    void shouldNotUpdateIfAlreadyPaid() {

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setStatus(ReservationStatus.PAID);

        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        reservationService.markAsPaid(1L);

        assertEquals(ReservationStatus.PAID, reservation.getStatus());

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldThrowIfReservationNotFound() {

        when(reservationRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> reservationService.markAsPaid(1L));
    }

    //mis reservaciones

    @Test
    void shouldReturnReservationsByEmail() {

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setUser(user);
        reservation.setRoom(room);
        reservation.setStartAt(request.getStartAt());
        reservation.setEndAt(request.getEndAt());
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setPrice(BigDecimal.valueOf(20));

        when(reservationRepository
                .findByUserEmailOrderByCreatedAtDesc("p1@email.com"))
                .thenReturn(List.of(reservation));

        List<ReservationResponse> result =
                reservationService.getMyReservations("p1@email.com");

        assertNotNull(result);
        assertEquals(1, result.size());

        ReservationResponse response = result.get(0);

        assertEquals(1L, response.getId());
        assertEquals("Sala A", response.getRoomName());
        assertEquals("p1@email.com", response.getUsername());
        assertEquals(ReservationStatus.PENDING, response.getStatus());

        verify(reservationRepository)
                .findByUserEmailOrderByCreatedAtDesc("p1@email.com");
    }

    //edge case
    @Test
    void shouldReturnEmptyListWhenUserHasNoReservations() {

        when(reservationRepository
                .findByUserEmailOrderByCreatedAtDesc("p1@email.com"))
                .thenReturn(Collections.emptyList());

        List<ReservationResponse> result =
                reservationService.getMyReservations("p1@email.com");

        assertTrue(result.isEmpty());

        verify(reservationRepository)
                .findByUserEmailOrderByCreatedAtDesc("p1@email.com");
    }

    //cancelar reservacion
    @Test
    void shouldCancelReservation() {

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setUser(user);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setStartAt(Instant.parse("2025-01-02T15:00:00Z"));

        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        reservationService.cancelReservation(1L, user.getEmail());

        assertEquals(
                ReservationStatus.CANCELLED,
                reservation.getStatus()
        );

        verify(reservationRepository).save(reservation);
    }

    //cancelar reservacion de alguien mas
    @Test
    void shouldThrowWhenCancellingAnotherUsersReservation() {

        Reservation reservation = new Reservation();

        User anotherUser = new User();
        anotherUser.setEmail("other@email.com");

        reservation.setUser(anotherUser);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setStartAt(Instant.parse("2025-01-02T15:00:00Z"));

        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        assertThrows(
                ReservationConflictException.class,
                () -> reservationService.cancelReservation(
                        1L,
                        "p1@email.com"
                )
        );

        verify(reservationRepository, never()).save(any());
    }

    //impedir cancelar iniciada
    @Test
    void shouldThrowWhenReservationAlreadyStarted() {

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setStatus(ReservationStatus.PENDING);

        reservation.setStartAt(
                Instant.parse("2024-12-31T10:00:00Z")
        );

        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        assertThrows(
                ReservationConflictException.class,
                () -> reservationService.cancelReservation(
                        1L,
                        user.getEmail()
                )
        );

        verify(reservationRepository, never()).save(any());
    }

    //impedir cancelar ya cancelada
    @Test
    void shouldThrowWhenReservationAlreadyCancelled() {

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setStartAt(
                Instant.parse("2025-01-02T15:00:00Z")
        );

        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        assertThrows(
                ReservationConflictException.class,
                () -> reservationService.cancelReservation(
                        1L,
                        user.getEmail()
                )
        );

        verify(reservationRepository, never()).save(any());
    }

    //impedir cancelar pagadas
    @Test
    void shouldThrowWhenReservationIsPaid() {

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setStatus(ReservationStatus.PAID);
        reservation.setStartAt(
                Instant.parse("2025-01-02T15:00:00Z")
        );

        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        assertThrows(
                ReservationConflictException.class,
                () -> reservationService.cancelReservation(
                        1L,
                        user.getEmail()
                )
        );

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_shouldUseDynamicOpeningTime() {

        SystemSettings settings = new SystemSettings();
        settings.setOpeningTime(LocalTime.of(8, 0));
        settings.setClosingTime(LocalTime.of(18, 0));
        settings.setMaxReservationHours(8);
        settings.setPendingExpirationMinutes(15);
        settings.setInstitutionName("Coworking Platform");

        when(systemSettingsService.getCurrentSettings())
                .thenReturn(settings);

        request.setStartAt(elSalvadorTime(8));
        request.setEndAt(elSalvadorTime(10));

        when(roomRepository.findById(room.getId()))
                .thenReturn(Optional.of(room));

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        when(reservationRepository.findOverlappingForUpdate(
                anyLong(), any(), any())
        ).thenReturn(Collections.emptyList());

        when(reservationRepository.existsByRoomIdAndStartAtLessThanAndEndAtGreaterThan(
                anyLong(), any(), any())
        ).thenReturn(false);

        when(reservationRepository.findByUserIdAndRoomIdAndStartAtAndEndAt(
                anyLong(), anyLong(), any(), any())
        ).thenReturn(Optional.empty());

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response =
                reservationService.createReservation(
                        user.getId(),
                        request
                );

        assertNotNull(response);
        assertEquals(ReservationStatus.PENDING, response.getStatus());

        verify(systemSettingsService)
                .getCurrentSettings();
    }

    @Test
    void createReservation_shouldRejectBeforeDynamicOpeningTime() {

        SystemSettings settings = new SystemSettings();
        settings.setOpeningTime(LocalTime.of(8, 0));
        settings.setClosingTime(LocalTime.of(18, 0));
        settings.setMaxReservationHours(8);
        settings.setPendingExpirationMinutes(15);
        settings.setInstitutionName("Coworking Platform");

        when(systemSettingsService.getCurrentSettings())
                .thenReturn(settings);

        request.setStartAt(elSalvadorTime(7));
        request.setEndAt(elSalvadorTime(9));

        when(roomRepository.findById(room.getId()))
                .thenReturn(Optional.of(room));

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        assertThrows(
                BadRequestException.class,
                () -> reservationService.createReservation(
                        user.getId(),
                        request
                )
        );

        verify(systemSettingsService)
                .getCurrentSettings();

        verify(reservationRepository, never())
                .save(any(Reservation.class));
    }

    @Test
    void createReservation_shouldRejectAfterDynamicClosingTime() {

        SystemSettings settings = new SystemSettings();
        settings.setOpeningTime(LocalTime.of(8, 0));
        settings.setClosingTime(LocalTime.of(18, 0));
        settings.setMaxReservationHours(8);
        settings.setPendingExpirationMinutes(15);
        settings.setInstitutionName("Coworking Platform");

        when(systemSettingsService.getCurrentSettings())
                .thenReturn(settings);

        request.setStartAt(elSalvadorTime(17));
        request.setEndAt(elSalvadorTime(19));

        when(roomRepository.findById(room.getId()))
                .thenReturn(Optional.of(room));

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        assertThrows(
                BadRequestException.class,
                () -> reservationService.createReservation(
                        user.getId(),
                        request
                )
        );

        verify(systemSettingsService)
                .getCurrentSettings();

        verify(reservationRepository, never())
                .save(any(Reservation.class));
    }

    @Test
    void createReservation_shouldUseDynamicMaxReservationHours() {

        SystemSettings settings = new SystemSettings();
        settings.setOpeningTime(LocalTime.of(7, 0));
        settings.setClosingTime(LocalTime.of(20, 0));
        settings.setMaxReservationHours(4);
        settings.setPendingExpirationMinutes(15);
        settings.setInstitutionName("Coworking Platform");

        when(systemSettingsService.getCurrentSettings())
                .thenReturn(settings);

        request.setStartAt(elSalvadorTime(9));
        request.setEndAt(elSalvadorTime(14));

        when(roomRepository.findById(room.getId()))
                .thenReturn(Optional.of(room));

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        assertThrows(
                BadRequestException.class,
                () -> reservationService.createReservation(
                        user.getId(),
                        request
                )
        );

        verify(systemSettingsService)
                .getCurrentSettings();

        verify(reservationRepository, never())
                .save(any(Reservation.class));
    }
    @Test
    void createReservation_shouldAllowReservationWithinDynamicMaxHours() {

        SystemSettings settings = new SystemSettings();
        settings.setOpeningTime(LocalTime.of(7, 0));
        settings.setClosingTime(LocalTime.of(20, 0));
        settings.setMaxReservationHours(4);
        settings.setPendingExpirationMinutes(15);
        settings.setInstitutionName("Coworking Platform");

        when(systemSettingsService.getCurrentSettings())
                .thenReturn(settings);

        request.setStartAt(elSalvadorTime(9));
        request.setEndAt(elSalvadorTime(13));

        when(roomRepository.findById(room.getId()))
                .thenReturn(Optional.of(room));

        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        when(reservationRepository.findOverlappingForUpdate(
                anyLong(), any(), any())
        ).thenReturn(Collections.emptyList());

        when(reservationRepository.existsByRoomIdAndStartAtLessThanAndEndAtGreaterThan(
                anyLong(), any(), any())
        ).thenReturn(false);

        when(reservationRepository.findByUserIdAndRoomIdAndStartAtAndEndAt(
                anyLong(), anyLong(), any(), any())
        ).thenReturn(Optional.empty());

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response =
                reservationService.createReservation(
                        user.getId(),
                        request
                );

        assertNotNull(response);
        assertEquals(ReservationStatus.PENDING, response.getStatus());

        verify(reservationRepository)
                .save(any(Reservation.class));
    }

    @Test
    void markAsPaid_shouldUseDynamicExpirationTime() {

        SystemSettings settings = new SystemSettings();
        settings.setOpeningTime(LocalTime.of(7, 0));
        settings.setClosingTime(LocalTime.of(20, 0));
        settings.setMaxReservationHours(8);
        settings.setPendingExpirationMinutes(30);
        settings.setInstitutionName("Coworking Platform");

        when(systemSettingsService.getCurrentSettings())
                .thenReturn(settings);

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setCreatedAt(
                Instant.parse("2025-01-01T09:40:00Z")
        );

        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        reservationService.markAsPaid(1L);

        assertEquals(
                ReservationStatus.PAID,
                reservation.getStatus()
        );

        verify(reservationRepository)
                .save(reservation);

        verify(systemSettingsService)
                .getCurrentSettings();
    }

    @Test
    void markAsPaid_shouldExpireUsingDynamicExpirationTime() {

        SystemSettings settings = new SystemSettings();
        settings.setOpeningTime(LocalTime.of(7, 0));
        settings.setClosingTime(LocalTime.of(20, 0));
        settings.setMaxReservationHours(8);
        settings.setPendingExpirationMinutes(15);
        settings.setInstitutionName("Coworking Platform");

        when(systemSettingsService.getCurrentSettings())
                .thenReturn(settings);

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setCreatedAt(
                Instant.parse("2025-01-01T09:40:00Z")
        );

        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        assertThrows(
                ReservationConflictException.class,
                () -> reservationService.markAsPaid(1L)
        );

        assertEquals(
                ReservationStatus.EXPIRED,
                reservation.getStatus()
        );

        verify(reservationRepository)
                .save(reservation);

        verify(systemSettingsService)
                .getCurrentSettings();
    }
}
