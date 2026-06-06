package com.coworking.resources.controller.room;

import com.coworking.room.controller.RoomController;
import com.coworking.room.dto.RoomDto;
import com.coworking.security.JwtUtil;
import com.coworking.room.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;
    @MockitoBean
    private JwtUtil jwtUtil;

    //TODOS LOS PARAMETROS SON VALIDOS
    @Test
    void getAvailability_debeRetornar200() throws Exception {

        when(roomService.getRoomsAvailability(
                any(Instant.class),
                any(Instant.class),
                eq(4)
        )).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/rooms/availability")
                                .param("start", "2026-01-01T08:00:00Z")
                                .param("end", "2026-01-01T10:00:00Z")
                                .param("people", "4")
                )
                .andExpect(status().isOk());
    }
    //FECHA INVALIDA
    @Test
    void getAvailability_fechaInvalida_retorna400() throws Exception {

        mockMvc.perform(
                        get("/api/rooms/availability")
                                .param("start", "fecha-invalida")
                                .param("end", "2026-01-01T10:00:00Z")
                                .param("people", "4")
                )
                .andExpect(status().isBadRequest());
    }

    //FALTA PARAMETRO
    @Test
    void getAvailability_sinPeople_retorna400() throws Exception {

        mockMvc.perform(
                        get("/api/rooms/availability")
                                .param("start", "2026-01-01T08:00:00Z")
                                .param("end", "2026-01-01T10:00:00Z")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllRooms_debeRetornar200() throws Exception {

        when(roomService.getAllRooms())
                .thenReturn(List.of(new RoomDto()));

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEliminarSala_inexistente_devuelve404() throws Exception {

        when(roomService.deleteRoom(1L)).thenReturn(false);

        mockMvc.perform(delete("/api/rooms/1")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    //Validacion de id numerico
    @Test
    @WithMockUser(roles = "ADMIN")
    void eliminarSala_idInvalido_devuelve400() throws Exception {

        mockMvc.perform(delete("/api/rooms/abc")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    //obtener sala por id
    @Test
    void getRoomById_debeRetornar200() throws Exception {

        RoomDto dto = new RoomDto();
        dto.setId(1L);
        dto.setName("Sala A");

        when(roomService.getRoomById(1L))
                .thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/rooms/1"))
                .andExpect(status().isOk());
    }

    //si la sala es inexistente
    @Test
    void getRoomById_inexistente_retorna404() throws Exception {

        when(roomService.getRoomById(1L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/rooms/1"))
                .andExpect(status().isNotFound());
    }

}