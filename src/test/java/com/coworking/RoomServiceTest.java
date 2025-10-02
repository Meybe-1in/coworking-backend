package com.coworking;

import com.coworking.dto.RoomDto;
import com.coworking.model.Room;
import com.coworking.repository.RoomRepository;
import com.coworking.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomService roomService;

    private Room room;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        room = new Room();
        room.setId(1L);
        room.setName("Sala 1");
        room.setDescription("Sala grande");
        room.setCapacity(10);
        room.setAvailable(true);
    }

    @Test
    void getAllRooms_returnsList() {
        when(roomRepository.findAll()).thenReturn(List.of(room));

        List<RoomDto> rooms = roomService.getAllRooms();

        assertEquals(1, rooms.size());
        assertEquals("Sala 1", rooms.getFirst().getName());
    }

    @Test
    void getRoomById_returnsRoomDto() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        Optional<RoomDto> result = roomService.getRoomById(1L);

        assertTrue(result.isPresent());
        assertEquals("Sala 1", result.get().getName());
    }

    @Test
    void createRoom_savesAndReturnsDto() {
        RoomDto dto = new RoomDto();
        dto.setName("Sala nueva");
        dto.setDescription("Pequeña");
        dto.setCapacity(5);
        dto.setAvailable(true);

        when(roomRepository.save(any(Room.class))).thenReturn(room);

        RoomDto result = roomService.createRoom(dto);

        assertNotNull(result);
        assertEquals("Sala 1", result.getName());
    }

    @Test
    void updateRoom_updatesExistingRoom() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        RoomDto dto = new RoomDto();
        dto.setName("Sala modificada");
        dto.setDescription("Actualizada");
        dto.setCapacity(15);
        dto.setAvailable(false);

        Optional<RoomDto> result = roomService.updateRoom(1L, dto);

        assertTrue(result.isPresent());
        assertEquals("Sala modificada", result.get().getName());
    }

    @Test
    void deleteRoom_existingRoom_returnsTrue() {
        when(roomRepository.existsById(1L)).thenReturn(true);

        boolean deleted = roomService.deleteRoom(1L);

        assertTrue(deleted);
        verify(roomRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteRoom_nonExistingRoom_returnsFalse() {
        when(roomRepository.existsById(1L)).thenReturn(false);

        boolean deleted = roomService.deleteRoom(1L);

        assertFalse(deleted);
        verify(roomRepository, never()).deleteById(anyLong());
    }
}

