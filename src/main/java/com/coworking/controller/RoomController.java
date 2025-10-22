package com.coworking.controller;

import com.coworking.dto.RoomDto;
import com.coworking.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.swing.plaf.SeparatorUI;
import java.util.List;

@RestController
@RequestMapping("api/rooms")
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
    //Crear
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear una nueva sala(solo ADMIN")
    public ResponseEntity<RoomDto> createRoom(@RequestBody RoomDto dto){
        return ResponseEntity.ok(roomService.createRoom(dto));
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
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar sala (solo ADMIN")
    public ResponseEntity<RoomDto> deleteRoom(@PathVariable Long id){
        return roomService.deleteRoom(id)
                ?ResponseEntity.noContent().build()
                :ResponseEntity.notFound().build();
    }





}
