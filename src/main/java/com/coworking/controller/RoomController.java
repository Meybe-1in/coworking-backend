package com.coworking.controller;

import com.coworking.dto.RoomAvailabilityResponse;
import com.coworking.dto.RoomDto;
import com.coworking.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("api/rooms")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Rooms", description = "Gestion de salas de coworking")
@AllArgsConstructor
public class RoomController {
    private final RoomService roomService;

    // listar
    @GetMapping
    @Operation(summary = "Listar todas las salas")
    public List<RoomDto> getAllRoom(){
        return roomService.getAllRooms();
    }

    //buscar por id
    @GetMapping("/{id}")
    @Operation(summary = "Obtener salas por id")
    public ResponseEntity<RoomDto> getRoomById(@PathVariable Long id){
        return roomService.getRoomById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    //Actualizar
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar sala existente(solo ADMIN)")
    public ResponseEntity<RoomDto> actualizarRoom(@PathVariable Long id, @RequestBody RoomDto dto){
        return roomService.updateRoom(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    //eliminar
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar sala (solo ADMIN")
    public ResponseEntity<RoomDto> deleteRoom(@PathVariable Long id){
        return roomService.deleteRoom(id)
                ?ResponseEntity.noContent().build()
                :ResponseEntity.notFound().build();
    }

    //crear
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "crear sala con imagen agregada (solo ADMIN)")

    public ResponseEntity<RoomDto> createRoom(
            @RequestPart("room") RoomDto roomDto,
            @RequestPart(value = "image", required = false) MultipartFile image){
        RoomDto savedRoom = roomService.createRoom(roomDto, image);
        return  ResponseEntity.ok(savedRoom);
    }
    @GetMapping("/availability")
    public List<RoomAvailabilityResponse> getAvailability(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant start,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant end,
            @RequestParam Integer people
    ){

        return roomService.getRoomsAvailability(start,end,people);
    }
}
