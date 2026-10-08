package com.coworking.room.service;

import com.coworking.admin.audit.enums.AuditAction;
import com.coworking.admin.audit.service.AuditLogService;
import com.coworking.admin.dto.AdminPageResponse;
import com.coworking.admin.notification.enums.NotificationType;
import com.coworking.admin.notification.service.NotificationService;
import com.coworking.admin.settings.entity.SystemSettings;
import com.coworking.admin.settings.service.SystemSettingsService;
import com.coworking.exception.RoomHasReservationsException;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.room.dto.RoomAvailabilityResponse;
import com.coworking.room.dto.RoomDto;
import com.coworking.reservation.model.Reservation;
import com.coworking.room.model.Room;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.room.repository.RoomRepository;
import com.coworking.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final StorageService storageService;
    private final AuditLogService auditLogService;
    private final SystemSettingsService systemSettingsService;
    private final NotificationService notificationService;
    private final Clock clock;
    // MAPPERS

    private RoomDto mapToDto(Room room) {
        RoomDto dto = new RoomDto();
        dto.setId(room.getId());
        dto.setName(room.getName());
        dto.setDescription(room.getDescription());
        dto.setCapacity(room.getCapacity());
        dto.setAvailable(room.isAvailable());
        dto.setPrice(room.getPrice());
        dto.setImageUrl(room.getImageUrl());
        dto.setLocation(room.getLocation());
        dto.setFeatures(room.getFeatures());
        return dto;
    }

    private Room mapToEntity(RoomDto dto) {
        Room room = new Room();
        room.setId(dto.getId());
        room.setName(dto.getName());
        room.setDescription(dto.getDescription());
        room.setCapacity(dto.getCapacity());
        room.setAvailable(dto.isAvailable());
        room.setPrice(dto.getPrice());
        room.setImageUrl(dto.getImageUrl());
        room.setLocation(dto.getLocation());
        room.setFeatures(dto.getFeatures());
        return room;
    }

    private RoomAvailabilityResponse mapToAvailability(Room room, boolean available, Instant nextAvailable) {

        RoomAvailabilityResponse dto = new RoomAvailabilityResponse();

        dto.setId(room.getId());
        dto.setName(room.getName());
        dto.setCapacity(room.getCapacity());
        dto.setPrice(room.getPrice());
        dto.setLocation(room.getLocation());
        dto.setImageUrl(room.getImageUrl());
        dto.setAvailable(available);
        dto.setNextAvailable(nextAvailable);

        return dto;
    }

    // CRUD ROOMS

    public AdminPageResponse<RoomDto> getAllRooms(int page, int size) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.ASC, "capacity")
        );

        Page<RoomDto> rooms =
                roomRepository
                        .findAll(pageable)
                        .map(this::mapToDto);

        return AdminPageResponse.<RoomDto>builder()
                .content(rooms.getContent())
                .page(rooms.getNumber())
                .size(rooms.getSize())
                .totalElements(rooms.getTotalElements())
                .totalPages(rooms.getTotalPages())
                .first(rooms.isFirst())
                .last(rooms.isLast())
                .build();
    }

    public Optional<RoomDto> getRoomById(Long id) {

        return roomRepository
                .findById(id)
                .map(this::mapToDto);
    }

    @Transactional
    public RoomDto createRoom(RoomDto dto, MultipartFile image) {

        Room room = mapToEntity(dto);

        if (image != null && !image.isEmpty()) {
            String imageUrl = storageService.upload(image);
            room.setImageUrl(imageUrl);
        }

        Room savedRoom = roomRepository.save(room);

        auditLogService.log(
                AuditAction.ROOM_CREATED,
                "Room",
                savedRoom.getId()
        );

        return mapToDto(savedRoom);
    }

    @Transactional
    public Optional<RoomDto> updateRoom(Long id, RoomDto dto, MultipartFile image) {

        return roomRepository.findById(id).map(room -> {

            room.setName(dto.getName());
            room.setDescription(dto.getDescription());
            room.setCapacity(dto.getCapacity());
            room.setAvailable(dto.isAvailable());
            room.setPrice(dto.getPrice());
            room.setLocation(dto.getLocation());
            room.setFeatures(dto.getFeatures());

            if (image != null && !image.isEmpty()) {
                String imageUrl = storageService.upload(image);
                room.setImageUrl(imageUrl);
            }

            Room updateRoom = roomRepository.save(room);
            auditLogService.log(
                    AuditAction.ROOM_UPDATED,
                    "Room",
                    updateRoom.getId()
            );

            return mapToDto(updateRoom);
        });
    }

    @Transactional
    public boolean deleteRoom(Long id) {

        if (!roomRepository.existsById(id)) {
            return false;
        }

        try {
            roomRepository.deleteById(id);
            roomRepository.flush();

            auditLogService.log(
                    AuditAction.ROOM_DELETED,
                    "Room",
                    id
            );

            notificationService.createNotificationForAdmins(
                    NotificationType.ROOM_DELETED,
                    "Sala eliminada",
                    "La sala #" + id + " ha sido eliminada.",
                    "Room",
                    id
            );

            return true;

        } catch (DataIntegrityViolationException ex) {
            throw new RoomHasReservationsException();
        }
    }

    // ROOM AVAILABILITY

    @Transactional(readOnly = true)
    public List<RoomAvailabilityResponse> getRoomsAvailability(
            Instant start,
            Instant end,
            Integer people
    ) {
        var settings = systemSettingsService.getCurrentSettings();

        List<Room> rooms =
                roomRepository.findByCapacityGreaterThanEqualOrderByCapacityAsc(
                        people
                );

        List<ReservationStatus> blockingStatuses = List.of(
                ReservationStatus.PENDING,
                ReservationStatus.PAID
        );

        return rooms.stream()
                .map(room -> {
                    List<Reservation> reservations =
                            reservationRepository.findRoomOverlappingReservations(
                                    room.getId(),
                                    blockingStatuses,
                                    start,
                                    end
                            );

                    List<Reservation> activeReservations =
                            reservations.stream()
                                    .filter(reservation ->
                                            isBlockingReservation(
                                                    reservation,
                                                    settings.getPendingExpirationMinutes()
                                            )
                                    )
                                    .toList();

                    boolean available = activeReservations.isEmpty();

                    Instant nextAvailable = available
                            ? null
                            : findNextAvailable(
                            room,
                            start,
                            end,
                            settings
                    );

                    return mapToAvailability(
                            room,
                            available,
                            nextAvailable
                    );
                })
                .toList();
    }

    private boolean isBlockingReservation(
            Reservation reservation,
            int pendingExpirationMinutes
    ) {
        if (reservation.getStatus() == ReservationStatus.PAID) {
            return true;
        }

        Instant expirationTime = reservation.getCreatedAt()
                .plus(Duration.ofMinutes(pendingExpirationMinutes));

        return Instant.now(clock).isBefore(expirationTime);
    }

    private Instant findNextAvailable(
            Room room,
            Instant requestedStart,
            Instant requestedEnd,
            SystemSettings settings
    ) {
        ZoneId zoneId = ZoneId.of("America/El_Salvador");

        LocalDate date = requestedStart
                .atZone(zoneId)
                .toLocalDate();

        LocalTime requestedStartTime = requestedStart
                .atZone(zoneId)
                .toLocalTime();

        Duration requestedDuration =
                Duration.between(requestedStart, requestedEnd);

        LocalTime openingTime = settings.getOpeningTime();
        LocalTime closingTime = settings.getClosingTime();

        LocalTime candidateTime = requestedStartTime;

        if (candidateTime.isBefore(openingTime)) {
            candidateTime = openingTime;
        }

        candidateTime = candidateTime
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        if (candidateTime.isBefore(requestedStartTime)) {
            candidateTime = candidateTime.plusHours(1);
        }

        while (!candidateTime.plus(requestedDuration).isAfter(closingTime)) {

            LocalDateTime candidateStartDateTime =
                    LocalDateTime.of(date, candidateTime);

            LocalDateTime candidateEndDateTime =
                    candidateStartDateTime.plus(requestedDuration);

            Instant candidateStart =
                    candidateStartDateTime
                            .atZone(zoneId)
                            .toInstant();

            Instant candidateEnd =
                    candidateEndDateTime
                            .atZone(zoneId)
                            .toInstant();

            if (!hasBlockingReservation(
                    room,
                    candidateStart,
                    candidateEnd,
                    settings
            )) {
                return candidateStart;
            }

            candidateTime = candidateTime.plusHours(1);
        }

        return null;
    }

    private boolean hasBlockingReservation(
            Room room,
            Instant start,
            Instant end,
            SystemSettings settings
    ) {
        List<Reservation> reservations =
                reservationRepository.findRoomOverlappingReservations(
                        room.getId(),
                        List.of(
                                ReservationStatus.PENDING,
                                ReservationStatus.PAID
                        ),
                        start,
                        end
                );

        return reservations.stream()
                .anyMatch(reservation ->
                        isBlockingReservation(
                                reservation,
                                settings.getPendingExpirationMinutes()
                        )
                );
    }

}