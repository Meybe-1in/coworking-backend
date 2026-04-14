package com.coworking.resources.service;

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
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private RoomService roomService;

    private Room room;
    private RoomDto baseRoomDto;

    @BeforeEach
    void setUp() {
        //MockitoAnnotations.openMocks(this);

        room = new Room();
        room.setId(1L);
        room.setName("Sala 1");
        room.setDescription("Sala grande");
        room.setCapacity(10);
        room.setAvailable(true);
        room.setPrice(new BigDecimal("10.00"));
        room.setLocation("Ubicación Test");
        room.setFeatures(List.of("Wifi", "Pizarra"));
        // URL de imagen de prueba
        room.setImageUrl("/uploads/mock-image.jpg");

        // Configuración del DTO base
        baseRoomDto = new RoomDto();
        baseRoomDto.setName("Sala 1");
        baseRoomDto.setDescription("Sala grande");
        baseRoomDto.setCapacity(10);
        baseRoomDto.setAvailable(true);
        baseRoomDto.setPrice(new BigDecimal("10.00"));
        baseRoomDto.setLocation("Ubicación Test");
        baseRoomDto.setFeatures(List.of("Wifi", "Pizarra"));
        baseRoomDto.setImageUrl("/uploads/mock-image.jpg");

    }

    @Test
    void getAllRooms_returnsList() {
        when(roomRepository.findAll(any(Sort.class))).thenReturn(List.of(room));

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
        assertEquals("/uploads/mock-image.jpg", result.get().getImageUrl());
    }

    @Test
    void createRoom_withoutImage_savesAndReturnsDto() {
        // Mock: Simula la entidad devuelta al guardar
        Room roomToReturn = new Room();
        roomToReturn.setId(2L);
        roomToReturn.setName(baseRoomDto.getName());
        roomToReturn.setPrice(baseRoomDto.getPrice());

        when(roomRepository.save(any(Room.class))).thenReturn(roomToReturn);

        RoomDto result = roomService.createRoom(baseRoomDto, null);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("Sala 1", result.getName());
        assertEquals(new BigDecimal("10.00"), result.getPrice());
        assertNull(result.getImageUrl());
        // Verifica que NO se llamó al servicio de almacenamiento
        verify(storageService, never()).upload(any(MultipartFile.class));
    }

    @Test
    void createRoom_withImage_savesImageAndReturnsDtoWithUrl() {
        MockMultipartFile mockImage = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "test data".getBytes()
        );
        String expectedUrl = "/uploads/test-unique-id.jpg";
        // Mock: El servicio de almacenamiento devuelve la URL
        when(storageService.upload(mockImage)).thenReturn(expectedUrl);
        // Mock: Simula la entidad devuelta al guardar
        Room roomSaved = new Room();
        roomSaved.setId(2L);
        roomSaved.setName(baseRoomDto.getName());
        roomSaved.setImageUrl(expectedUrl);
        when(roomRepository.save(any(Room.class))).thenReturn(roomSaved);
        // Llamada al método con la imagen
        RoomDto result = roomService.createRoom(baseRoomDto, mockImage);
        assertNotNull(result);
        assertEquals(expectedUrl, result.getImageUrl());
        // Verifica que SÍ se llamó al servicio de almacenamiento
        verify(storageService, times(1)).upload(mockImage);
    }
    @Test
    void updateRoom_updatesExistingRoom() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        RoomDto dto = new RoomDto();
        dto.setName("Sala modificada");
        dto.setDescription("Actualizada");
        dto.setCapacity(15);
        dto.setPrice(new BigDecimal("15.00"));
        dto.setAvailable(false);
        // Mantenemos la URL existente o la omitimos si el DTO no la incluye
        dto.setImageUrl(room.getImageUrl());
        // Configurar el save para devolver la misma entidad modificada
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        Optional<RoomDto> result = roomService.updateRoom(1L, dto);

        assertTrue(result.isPresent());
        assertEquals("Sala modificada", result.get().getName());
        assertEquals(15, result.get().getCapacity());
        assertEquals(new BigDecimal("15.00"), result.get().getPrice());
        // Verificamos que la URL de imagen no se perdió si se incluyó en el DTO
        assertEquals("/uploads/mock-image.jpg", result.get().getImageUrl());
    }

    @Test
    void getRoomById_notFound_returnsEmpty() {
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<RoomDto> result = roomService.getRoomById(1L);

        assertTrue(result.isEmpty());
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
    
    @Test
    void getAllRooms_debeRetornarOrdenAscendentePorCapacidad() {

        Room r1 = new Room();
        r1.setCapacity(6);

        Room r2 = new Room();
        r2.setCapacity(4);

        when(roomRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(r2, r1));

        List<RoomDto> result = roomService.getAllRooms();

        assertEquals(4, result.get(0).getCapacity());
        assertEquals(6, result.get(1).getCapacity());
    }
}

