package com.coworking.room.service;

import com.coworking.room.dto.RoomAvailabilityResponse;
import com.coworking.room.dto.RoomDto;
import com.coworking.reservation.model.Reservation;
import com.coworking.room.model.Room;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.room.repository.RoomRepository;
import com.coworking.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
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
    // MAPPERS

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

    private RoomAvailabilityResponse mapToAvailability(Room room, boolean available){

        RoomAvailabilityResponse dto = new RoomAvailabilityResponse();

        dto.setId(room.getId());
        dto.setName(room.getName());
        dto.setCapacity(room.getCapacity());
        dto.setPrice(room.getPrice());
        dto.setLocation(room.getLocation());
        dto.setImageUrl(room.getImageUrl());
        dto.setAvailable(available);

        return dto;
    }

    // CRUD ROOMS

    public List<RoomDto> getAllRooms(){

        return roomRepository
                .findAll(Sort.by(Sort.Direction.ASC,"capacity"))
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public Optional<RoomDto> getRoomById(Long id){

        return roomRepository
                .findById(id)
                .map(this::mapToDto);
    }

    public RoomDto createRoom(RoomDto dto, MultipartFile image){

        Room room = mapToEntity(dto);

        if(image != null && !image.isEmpty()){
            String imageUrl = storageService.upload(image);
            room.setImageUrl(imageUrl);
        }

        return mapToDto(roomRepository.save(room));
    }

    public Optional<RoomDto> updateRoom(Long id, RoomDto dto){

        return roomRepository.findById(id).map(room -> {

            room.setName(dto.getName());
            room.setDescription(dto.getDescription());
            room.setCapacity(dto.getCapacity());
            room.setAvailable(dto.isAvailable());
            room.setPrice(dto.getPrice());
            room.setImageUrl(dto.getImageUrl());
            room.setLocation(dto.getLocation());
            room.setFeatures(dto.getFeatures());

            return mapToDto(roomRepository.save(room));
        });
    }

    public boolean deleteRoom(Long id){

        if(!roomRepository.existsById(id)) return false;

        roomRepository.deleteById(id);
        return true;
    }

    // ROOM AVAILABILITY

    public List<RoomAvailabilityResponse> getRoomsAvailability(
            Instant start,
           Instant end,
            Integer people
    ){

        List<Room> rooms =
                roomRepository.findByCapacityOrderByCapacityAsc(people);

        List<Reservation> overlapping =
                reservationRepository
                        .findByStartAtLessThanAndEndAtGreaterThan(end,start);

        Set<Long> busyRoomIds =
                overlapping.stream()
                        .map(r -> r.getRoom().getId())
                        .collect(Collectors.toSet());

        return rooms.stream()
                .map(room -> {

                    boolean available = !busyRoomIds.contains(room.getId());

                    return mapToAvailability(room, available);

                })
                .toList();
    }

}