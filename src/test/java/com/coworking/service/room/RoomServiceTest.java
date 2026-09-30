package com.coworking.service.room;

import com.coworking.admin.audit.enums.AuditAction;
import com.coworking.admin.audit.service.AuditLogService;
import com.coworking.admin.dto.AdminPageResponse;
import com.coworking.admin.settings.entity.SystemSettings;
import com.coworking.admin.settings.service.SystemSettingsService;
import com.coworking.exception.RoomHasReservationsException;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.room.dto.RoomAvailabilityResponse;
import com.coworking.room.dto.RoomDto;
import com.coworking.room.model.Room;
import com.coworking.room.repository.RoomRepository;
import com.coworking.room.service.RoomService;
import com.coworking.storage.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private SystemSettingsService systemSettingsService;

    @Mock
    private Clock clock;

    @InjectMocks
    private RoomService roomService;

    private Room room;
    private RoomDto baseRoomDto;
    private MockMultipartFile image;

    @BeforeEach
    void setUp() {
        room = new Room();
        room.setId(1L);
        room.setName("Sala 1");
        room.setDescription("Sala grande");
        room.setCapacity(10);
        room.setAvailable(true);
        room.setPrice(new BigDecimal("10.00"));
        room.setLocation("Ubicación Test");
        room.setFeatures(List.of("Wifi", "Pizarra"));
        room.setImageUrl("/uploads/mock-image.jpg");

        baseRoomDto = new RoomDto();
        baseRoomDto.setName("Sala 1");
        baseRoomDto.setDescription("Sala grande");
        baseRoomDto.setCapacity(10);
        baseRoomDto.setAvailable(true);
        baseRoomDto.setPrice(new BigDecimal("10.00"));
        baseRoomDto.setLocation("Ubicación Test");
        baseRoomDto.setFeatures(List.of("Wifi", "Pizarra"));
        baseRoomDto.setImageUrl("/uploads/mock-image.jpg");

        image = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "contenido".getBytes()
        );
    }
    // HELPERS

    private SystemSettings defaultSettings() {
        SystemSettings settings = new SystemSettings();
        settings.setOpeningTime(LocalTime.of(7, 0));
        settings.setClosingTime(LocalTime.of(20, 0));
        settings.setPendingExpirationMinutes(15);

        return settings;
    }

    private void mockDefaultSettings() {
        when(systemSettingsService.getCurrentSettings())
                .thenReturn(defaultSettings());
    }

    private void mockRoomsByCapacity(Integer people) {
        when(roomRepository
                .findByCapacityGreaterThanEqualOrderByCapacityAsc(people))
                .thenReturn(List.of(room));
    }

    private void mockOverlappingReservations(
            Instant start,
            Instant end,
            List<Reservation> reservations
    ) {
        when(reservationRepository.findRoomOverlappingReservations(
                eq(room.getId()),
                eq(List.of(
                        ReservationStatus.PENDING,
                        ReservationStatus.PAID
                )),
                eq(start),
                eq(end)
        )).thenReturn(reservations);
    }

    private Reservation createReservation(
            ReservationStatus status,
            Instant start,
            Instant end,
            Instant createdAt
    ) {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setRoom(room);
        reservation.setStatus(status);
        reservation.setStartAt(start);
        reservation.setEndAt(end);
        reservation.setCreatedAt(createdAt);

        return reservation;
    }

    private void mockDynamicOverlappingReservations(
            Reservation reservation
    ) {
        when(reservationRepository.findRoomOverlappingReservations(
                eq(room.getId()),
                eq(List.of(
                        ReservationStatus.PENDING,
                        ReservationStatus.PAID
                )),
                any(Instant.class),
                any(Instant.class)
        )).thenAnswer(invocation -> {

            Instant candidateStart = invocation.getArgument(2);
            Instant candidateEnd = invocation.getArgument(3);

            boolean overlaps =
                    candidateStart.isBefore(reservation.getEndAt())
                            && candidateEnd.isAfter(reservation.getStartAt());

            return overlaps
                    ? List.of(reservation)
                    : List.of();
        });
    }
    // GET ALL ROOM

    @Test
    void getAllRooms_returnsList() {

        Page<Room> roomsPage =
                new PageImpl<>(List.of(room));

        when(roomRepository.findAll(any(Pageable.class)))
                .thenReturn(roomsPage);

        AdminPageResponse<RoomDto> result =
                roomService.getAllRooms(0, 10);

        assertEquals(
                1,
                result.content().size()
        );

        assertEquals(
                "Sala 1",
                result.content().getFirst().getName()
        );
    }

    @Test
    void getAllRooms_debeRetornarOrdenAscendentePorCapacidad() {

        Room r1 = new Room();
        r1.setCapacity(6);

        Room r2 = new Room();
        r2.setCapacity(4);

        Page<Room> roomsPage =
                new PageImpl<>(List.of(r2, r1));

        when(roomRepository.findAll(any(Pageable.class)))
                .thenReturn(roomsPage);

        AdminPageResponse<RoomDto> result =
                roomService.getAllRooms(0, 10);

        assertEquals(
                4,
                result.content().get(0).getCapacity()
        );

        assertEquals(
                6,
                result.content().get(1).getCapacity()
        );
    }
    // GET ROOM BY ID

    @Test
    void getRoomById_returnsRoomDto() {

        when(roomRepository.findById(1L))
                .thenReturn(Optional.of(room));

        Optional<RoomDto> result =
                roomService.getRoomById(1L);

        assertTrue(result.isPresent());
        assertEquals("Sala 1", result.get().getName());
        assertEquals(
                "/uploads/mock-image.jpg",
                result.get().getImageUrl()
        );
    }

    @Test
    void getRoomById_notFound_returnsEmpty() {

        when(roomRepository.findById(1L))
                .thenReturn(Optional.empty());

        Optional<RoomDto> result =
                roomService.getRoomById(1L);

        assertTrue(result.isEmpty());
    }
    // CREATE ROOM

    @Test
    void createRoom_withoutImage_savesAndReturnsDto() {

        Room roomToReturn = new Room();
        roomToReturn.setId(2L);
        roomToReturn.setName(baseRoomDto.getName());
        roomToReturn.setPrice(baseRoomDto.getPrice());

        when(roomRepository.save(any(Room.class)))
                .thenReturn(roomToReturn);

        RoomDto result =
                roomService.createRoom(baseRoomDto, null);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("Sala 1", result.getName());
        assertEquals(
                new BigDecimal("10.00"),
                result.getPrice()
        );
        assertNull(result.getImageUrl());

        verify(storageService, never())
                .upload(any(MultipartFile.class));

        verify(auditLogService)
                .log(
                        AuditAction.ROOM_CREATED,
                        "Room",
                        2L
                );
    }

    @Test
    void createRoom_withImage_savesImageAndReturnsDtoWithUrl() {

        MockMultipartFile mockImage = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "test data".getBytes()
        );

        String expectedUrl =
                "/uploads/test-unique-id.jpg";

        when(storageService.upload(mockImage))
                .thenReturn(expectedUrl);

        Room roomSaved = new Room();
        roomSaved.setId(2L);
        roomSaved.setName(baseRoomDto.getName());
        roomSaved.setImageUrl(expectedUrl);

        when(roomRepository.save(any(Room.class)))
                .thenReturn(roomSaved);

        RoomDto result =
                roomService.createRoom(
                        baseRoomDto,
                        mockImage
                );

        assertNotNull(result);
        assertEquals(
                expectedUrl,
                result.getImageUrl()
        );

        verify(storageService)
                .upload(mockImage);

        verify(roomRepository)
                .save(any(Room.class));

        verify(auditLogService)
                .log(
                        AuditAction.ROOM_CREATED,
                        "Room",
                        2L
                );
    }
    // ROOM MODEL

    @Test
    void shouldAdd_features() {

        Room room = new Room();
        room.setFeatures(
                List.of(
                        "Wifi",
                        "Aire acondicionado"
                )
        );

        assertEquals(
                2,
                room.getFeatures().size()
        );
    }
    // DELETE ROOM

    @Test
    void deleteRoom_existingRoom_returnsTrue() {

        when(roomRepository.existsById(1L))
                .thenReturn(true);

        boolean deleted =
                roomService.deleteRoom(1L);

        assertTrue(deleted);

        verify(roomRepository)
                .deleteById(1L);

        verify(roomRepository)
                .flush();

        verify(auditLogService)
                .log(
                        AuditAction.ROOM_DELETED,
                        "Room",
                        1L
                );
    }

    @Test
    void deleteRoom_nonExistingRoom_returnsFalse() {

        when(roomRepository.existsById(1L))
                .thenReturn(false);

        boolean deleted =
                roomService.deleteRoom(1L);

        assertFalse(deleted);

        verify(roomRepository, never())
                .deleteById(anyLong());

        verify(roomRepository, never())
                .flush();

        verify(auditLogService, never())
                .log(
                        any(AuditAction.class),
                        anyString(),
                        anyLong()
                );
    }

    @Test
    void deleteRoom_withReservations_doesNotCreateAuditLog() {

        when(roomRepository.existsById(1L))
                .thenReturn(true);

        doThrow(
                new DataIntegrityViolationException("FK constraint")
        )
                .when(roomRepository)
                .flush();

        assertThrows(
                RoomHasReservationsException.class,
                () -> roomService.deleteRoom(1L)
        );

        verify(roomRepository)
                .deleteById(1L);

        verify(roomRepository)
                .flush();

        verify(auditLogService, never())
                .log(
                        any(AuditAction.class),
                        anyString(),
                        anyLong()
                );
    }
    // UPDATE ROOM

    @Test
    void updateRoom_roomNoExiste_retornaEmpty() {

        when(roomRepository.findById(1L))
                .thenReturn(Optional.empty());

        Optional<RoomDto> result =
                roomService.updateRoom(
                        1L,
                        new RoomDto(),
                        image
                );

        assertTrue(result.isEmpty());

        verify(roomRepository, never())
                .save(any(Room.class));
    }

    @Test
    void updateRoom_updatesExistingRoom_withoutImage() {

        when(roomRepository.findById(1L))
                .thenReturn(Optional.of(room));

        when(roomRepository.save(any(Room.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        RoomDto dto = new RoomDto();
        dto.setName("Sala modificada");
        dto.setDescription("Actualizada");
        dto.setCapacity(15);
        dto.setPrice(new BigDecimal("15.00"));
        dto.setAvailable(false);
        dto.setLocation("Nueva ubicación");
        dto.setFeatures(List.of("Wifi"));

        Optional<RoomDto> result =
                roomService.updateRoom(
                        1L,
                        dto,
                        null
                );

        assertTrue(result.isPresent());

        assertEquals(
                "Sala modificada",
                result.get().getName()
        );

        assertEquals(
                15,
                result.get().getCapacity()
        );

        assertEquals(
                new BigDecimal("15.00"),
                result.get().getPrice()
        );

        verify(storageService, never())
                .upload(any());

        verify(auditLogService)
                .log(
                        AuditAction.ROOM_UPDATED,
                        "Room",
                        1L
                );
    }

    @Test
    void updateRoom_withImage_updatesImageUrl() {

        when(roomRepository.findById(1L))
                .thenReturn(Optional.of(room));

        when(storageService.upload(image))
                .thenReturn("/uploads/new-image.jpg");

        when(roomRepository.save(any(Room.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        RoomDto dto = new RoomDto();
        dto.setName("Sala modificada");
        dto.setDescription("Actualizada");
        dto.setCapacity(15);
        dto.setPrice(new BigDecimal("15.00"));
        dto.setAvailable(false);

        Optional<RoomDto> result =
                roomService.updateRoom(
                        1L,
                        dto,
                        image
                );

        assertTrue(result.isPresent());

        assertEquals(
                "/uploads/new-image.jpg",
                result.get().getImageUrl()
        );

        verify(storageService)
                .upload(image);

        verify(auditLogService)
                .log(
                        AuditAction.ROOM_UPDATED,
                        "Room",
                        1L
                );
    }
    // ROOM AVAILABILITY

    @Test
    void getRoomsAvailability_pendingReservation_blocksRoom() {

        Instant start =
                Instant.parse("2026-09-16T14:00:00Z");

        Instant end =
                Instant.parse("2026-09-16T15:00:00Z");

        mockDefaultSettings();
        mockRoomsByCapacity(10);

        when(clock.instant())
                .thenReturn(
                        Instant.parse("2026-09-16T14:30:00Z")
                );

        Reservation reservation = createReservation(
                ReservationStatus.PENDING,
                start,
                end,
                Instant.parse("2026-09-16T14:20:00Z")
        );

        mockOverlappingReservations(
                start,
                end,
                List.of(reservation)
        );

        List<RoomAvailabilityResponse> result =
                roomService.getRoomsAvailability(
                        start,
                        end,
                        10
                );

        assertEquals(1, result.size());
        assertFalse(
                result.getFirst().isAvailable()
        );
    }

    @Test
    void getRoomsAvailability_expiredPendingReservation_doesNotBlockRoom() {

        Instant start =
                Instant.parse("2026-09-16T14:00:00Z");

        Instant end =
                Instant.parse("2026-09-16T15:00:00Z");

        mockDefaultSettings();
        mockRoomsByCapacity(10);

        when(clock.instant())
                .thenReturn(
                        Instant.parse("2026-09-16T14:30:00Z")
                );

        Reservation reservation = createReservation(
                ReservationStatus.PENDING,
                start,
                end,
                Instant.parse("2026-09-16T14:00:00Z")
        );

        mockOverlappingReservations(
                start,
                end,
                List.of(reservation)
        );

        List<RoomAvailabilityResponse> result =
                roomService.getRoomsAvailability(
                        start,
                        end,
                        10
                );

        assertEquals(1, result.size());
        assertTrue(
                result.getFirst().isAvailable()
        );
    }

    @Test
    void getRoomsAvailability_paidReservation_blocksRoom() {

        Instant start =
                Instant.parse("2026-09-16T14:00:00Z");

        Instant end =
                Instant.parse("2026-09-16T15:00:00Z");

        mockDefaultSettings();
        mockRoomsByCapacity(10);

        Reservation reservation = createReservation(
                ReservationStatus.PAID,
                start,
                end,
                Instant.parse("2026-09-16T13:00:00Z")
        );

        mockOverlappingReservations(
                start,
                end,
                List.of(reservation)
        );

        List<RoomAvailabilityResponse> result =
                roomService.getRoomsAvailability(
                        start,
                        end,
                        10
                );

        assertEquals(1, result.size());
        assertFalse(
                result.getFirst().isAvailable()
        );
    }

    @Test
    void getRoomsAvailability_cancelledReservation_doesNotBlockRoom() {

        Instant start =
                Instant.parse("2026-09-16T14:00:00Z");

        Instant end =
                Instant.parse("2026-09-16T15:00:00Z");

        mockDefaultSettings();
        mockRoomsByCapacity(10);

        mockOverlappingReservations(
                start,
                end,
                List.of()
        );

        List<RoomAvailabilityResponse> result =
                roomService.getRoomsAvailability(
                        start,
                        end,
                        10
                );

        assertEquals(1, result.size());
        assertTrue(
                result.getFirst().isAvailable()
        );
    }

    @Test
    void getRoomsAvailability_expiredReservation_doesNotBlockRoom() {

        Instant start =
                Instant.parse("2026-09-16T14:00:00Z");

        Instant end =
                Instant.parse("2026-09-16T15:00:00Z");

        mockDefaultSettings();
        mockRoomsByCapacity(10);

        mockOverlappingReservations(
                start,
                end,
                List.of()
        );

        List<RoomAvailabilityResponse> result =
                roomService.getRoomsAvailability(
                        start,
                        end,
                        10
                );

        assertEquals(1, result.size());
        assertTrue(
                result.getFirst().isAvailable()
        );
    }

    @Test
    void getRoomsAvailability_shouldRequestRoomsWithCapacityGreaterThanOrEqualToPeople() {

        Instant start =
                Instant.parse("2026-09-16T14:00:00Z");

        Instant end =
                Instant.parse("2026-09-16T15:00:00Z");

        mockDefaultSettings();
        mockRoomsByCapacity(4);

        mockOverlappingReservations(
                start,
                end,
                List.of()
        );

        List<RoomAvailabilityResponse> result =
                roomService.getRoomsAvailability(
                        start,
                        end,
                        4
                );

        assertEquals(1, result.size());

        verify(roomRepository)
                .findByCapacityGreaterThanEqualOrderByCapacityAsc(4);
    }

    @Test
    void getRoomsAvailability_occupiedRoom_returnsNextAvailableTime() {

        Instant start =
                Instant.parse("2026-09-16T08:00:00Z");

        Instant end =
                Instant.parse("2026-09-16T11:00:00Z");

        mockDefaultSettings();
        mockRoomsByCapacity(10);

        Reservation reservation = createReservation(
                ReservationStatus.PAID,
                Instant.parse("2026-09-16T10:00:00Z"),
                Instant.parse("2026-09-16T13:00:00Z"),
                Instant.parse("2026-09-16T07:00:00Z")
        );

        mockDynamicOverlappingReservations(
                reservation
        );

        List<RoomAvailabilityResponse> result =
                roomService.getRoomsAvailability(
                        start,
                        end,
                        10
                );

        assertEquals(1, result.size());

        assertFalse(
                result.getFirst().isAvailable()
        );

        assertEquals(
                Instant.parse("2026-09-16T13:00:00Z"),
                result.getFirst().getNextAvailable()
        );
    }

    @Test
    void getRoomsAvailability_whenNoNextSlotExists_returnsNullNextAvailable() {

        Instant start =
                Instant.parse("2026-09-16T16:00:00Z");

        Instant end =
                Instant.parse("2026-09-16T19:00:00Z");

        mockDefaultSettings();
        mockRoomsByCapacity(10);

        /*
         * La reserva cubre todo el horario de apertura:
         *
         * 07:00 - 20:00
         *
         * Por lo tanto, no existe ningún espacio disponible
         * para otra reserva de 3 horas.
         */
        Reservation reservation = createReservation(
                ReservationStatus.PAID,
                Instant.parse("2026-09-16T13:00:00Z"),
                Instant.parse("2026-09-17T02:00:00Z"),
                Instant.parse("2026-09-16T07:00:00Z")
        );

        mockDynamicOverlappingReservations(
                reservation
        );

        List<RoomAvailabilityResponse> result =
                roomService.getRoomsAvailability(
                        start,
                        end,
                        10
                );

        assertEquals(1, result.size());

        assertFalse(
                result.getFirst().isAvailable()
        );

        assertNull(
                result.getFirst().getNextAvailable()
        );
    }
}
