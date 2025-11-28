package com.coworking;

import com.coworking.dto.RoomDto;
import com.coworking.model.Room;
import com.coworking.repository.RoomRepository;
import com.coworking.service.FileStorageService;
import com.coworking.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private RoomService roomService;

    private Room room;
    private RoomDto baseRoomDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        room = new Room();
        room.setId(1L);
        room.setName("Sala 1");
        room.setDescription("Sala grande");
        room.setCapacity(10);
        room.setAvailable(true);
        room.setPrice(10.0);
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
        baseRoomDto.setPrice(10.0);
        baseRoomDto.setLocation("Ubicación Test");
        baseRoomDto.setFeatures(List.of("Wifi", "Pizarra"));
        baseRoomDto.setImageUrl("/uploads/mock-image.jpg");

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
        assertEquals("/uploads/mock-image.jpg", result.get().getImageUrl());
    }

    @Test
    void createRoom_withoutImage_savesAndReturnsDto() {
        // Mock: Simula la entidad devuelta al guardar
        Room roomToReturn = new Room();
        roomToReturn.setId(2L);
        roomToReturn.setName(baseRoomDto.getName());

        when(roomRepository.save(any(Room.class))).thenReturn(roomToReturn);

        RoomDto result = roomService.createRoom(baseRoomDto, null);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("Sala 1", result.getName());
        assertNull(result.getImageUrl());
        // Verifica que NO se llamó al servicio de almacenamiento
        verify(fileStorageService, never()).storeFile(any(MultipartFile.class));
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
        when(fileStorageService.storeFile(mockImage)).thenReturn(expectedUrl);
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
        verify(fileStorageService, times(1)).storeFile(mockImage);
    }
    @Test
    void updateRoom_updatesExistingRoom() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        RoomDto dto = new RoomDto();
        dto.setName("Sala modificada");
        dto.setDescription("Actualizada");
        dto.setCapacity(15);
        dto.setAvailable(false);
        // Mantenemos la URL existente o la omitimos si el DTO no la incluye
        dto.setImageUrl(room.getImageUrl());
        // Configurar el save para devolver la misma entidad modificada
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        Optional<RoomDto> result = roomService.updateRoom(1L, dto);

        assertTrue(result.isPresent());
        assertEquals("Sala modificada", result.get().getName());
        assertEquals(15, result.get().getCapacity());
        // Verificamos que la URL de imagen no se perdió si se incluyó en el DTO
        assertEquals("/uploads/mock-image.jpg", result.get().getImageUrl());
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

