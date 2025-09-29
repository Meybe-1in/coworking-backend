package com.coworking.service;

import com.coworking.dto.RoomDto;
import com.coworking.model.Room;
import com.coworking.repository.RoomRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    //
    private RoomDto mapToDto(Room room){
        RoomDto dto = new RoomDto();
        dto.setId(room.getId());
        dto.setName(room.getName());
        dto.setDescription(room.getDescription());
        dto.setCapacity(room.getCapacity());
        dto.setAvailable(room.isAvailable());
        return dto;
    }

    private Room mapToEntity(RoomDto dto){
        Room room = new Room();
        room.setId(dto.getId());
        room.setName(dto.getName());
        room.setDescription(dto.getDescription());
        room.setCapacity(dto.getCapacity());
        room.setAvailable(dto.isAvailable());
        return room;
    }

    //devuelve todas las salas en formato dto 
    public List<RoomDto> getAllRooms(){
        return roomRepository.findAll().stream()
                .map(this::mapToDto) //cada entidad se convierte en DTO
                .collect(Collectors.toList());
    }

    //busca por id y convierte a Dto
    public Optional<RoomDto> getRoomById(Long id){
        return roomRepository.findById(id).map(this::mapToDto);
    }

    //crear sala
    public RoomDto createRoom(RoomDto dto){
        Room room = mapToEntity(dto); //DTO a entidad
        return mapToDto(roomRepository.save(room)); //guardarlo(DB) y devolver como dto

    }

    // Busca la sala y actualiza campos
    public Optional<RoomDto> updateRoom(Long id, RoomDto dto){
        return roomRepository.findById(id).map(existing ->{
           existing.setName(dto.getName());
           existing.setDescription(dto.getDescription());
           existing.setCapacity(dto.getCapacity());
           existing.setAvailable(dto.isAvailable());
           return mapToDto(roomRepository.save(existing));
        });
    }

    //elimino la sala si existe

    public boolean deleteRoom(Long id){
        if (roomRepository.existsById(id)){
            roomRepository.deleteById(id);
            return  true;
        }
        return false;
    }
}
