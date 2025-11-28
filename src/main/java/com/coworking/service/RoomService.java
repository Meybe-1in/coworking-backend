package com.coworking.service;

import com.coworking.dto.RoomAvailabilityResponse;
import com.coworking.dto.RoomDto;
import com.coworking.model.Reservation;
import com.coworking.model.Room;
import com.coworking.repository.ReservationRepository;
import com.coworking.repository.RoomRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.PrivateKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final FileStorageService fileStorageService;

    private RoomDto mapToDto(Room room){
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

    private Room mapToEntity(RoomDto dto){
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

    public List<RoomDto> getAllRooms(){
        return roomRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public Optional<RoomDto> getRoomById(Long id){
        return roomRepository.findById(id).map(this::mapToDto);
    }

    public RoomDto createRoom(RoomDto dto, MultipartFile image){
        Room room = mapToEntity(dto);

        if (image != null && !image.isEmpty()){
            String imageUrl = fileStorageService.storeFile(image);
            room.setImageUrl(imageUrl);
        }

        Room saved = roomRepository.save(room);
        return mapToDto(saved);
    }

    public Optional<RoomDto> updateRoom(Long id, RoomDto dto){
        return roomRepository.findById(id).map(existing -> {
            existing.setName(dto.getName());
            existing.setDescription(dto.getDescription());
            existing.setCapacity(dto.getCapacity());
            existing.setAvailable(dto.isAvailable());
            existing.setPrice(dto.getPrice());
            existing.setImageUrl(dto.getImageUrl());
            existing.setLocation(dto.getLocation());
            existing.setFeatures(dto.getFeatures());
            return mapToDto(roomRepository.save(existing));
        });
    }

    public boolean deleteRoom(Long id){
        if (!roomRepository.existsById(id)) return false;
        roomRepository.deleteById(id);
        return true;
    }

    // filtrar salas disponibles
    public List<RoomAvailabilityResponse> findAvailableRooms(
            LocalDate date,
            LocalTime start,
            LocalTime end,
            Integer people
    ){
        LocalDateTime startDT = LocalDateTime.of(date, start);
        LocalDateTime endDT = LocalDateTime.of(date, end);

        // Obtener salas con capacidad suficiente
        List<Room> rooms = roomRepository.findByCapacityGreaterThanEqual(people);

        if (people == 1) {
            List<Room> onePersonRooms = rooms.stream()
                    .filter(r -> r.getCapacity() == 1)
                    .collect(Collectors.toList());
            return onePersonRooms.stream()
                    .map(room -> {
                        RoomAvailabilityResponse r = new RoomAvailabilityResponse();
                        r.setId(room.getId());
                        r.setName(room.getName());
                        r.setCapacity(room.getCapacity());
                        r.setLocation(room.getLocation());
                        r.setImageUrl(room.getImageUrl());
                        r.setAvailable(room.isAvailable());

                        return r;
                    })
                    .collect(Collectors.toList());
        }

        // Buscar TODAS las reservas que se crucen con el rango pedido
        List<Reservation> overlappingReservations =
                reservationRepository.findByStartAtLessThanAndEndAtGreaterThan(endDT, startDT);

        // Obtener IDs de salas ocupadas
        Set<Long> busyRoomIds = overlappingReservations.stream()
                .map(r -> r.getRoom().getId())
                .collect(Collectors.toSet());

        // Filtrar salas que NO están ocupadas
        List<Room> availableRooms = rooms.stream()
                .filter(room -> !busyRoomIds.contains(room.getId()))
                .toList();

        // Convertir a DTO respuesta
        return availableRooms.stream()
                .map(room -> {
                    RoomAvailabilityResponse r = new RoomAvailabilityResponse();
                    r.setId(room.getId());
                    r.setName(room.getName());
                    r.setCapacity(room.getCapacity());
                    r.setLocation(room.getLocation());
                    r.setImageUrl(room.getImageUrl());
                    r.setAvailable(room.isAvailable());

                    return r;
                })
                .collect(Collectors.toList());
    }
}

