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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(RoomController.class)
@AutoConfigureMockMvc
public class RoomControllerSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private JwtUtil jwtUtil;


    //Usuario normal no puedde eliminar
    @Test
    @WithMockUser(roles = "USER")
    public void usuarioNoAdmin_noPuedeEliminarSala() throws Exception {

        mockMvc.perform(delete("/api/rooms/1"))
                .andExpect(status().isForbidden());
    }

    // Admin si puede eliminar
    @Test
    @WithMockUser(roles = "ADMIN")
    public void adminPuedeEliminarSala() throws Exception {

        when(roomService.deleteRoom(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/rooms/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    //crear sala si es admin
    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeCrearSala() throws Exception {

        RoomDto dto = new RoomDto();
        dto.setId(1L);
        dto.setName("Sala Nueva");

        when(roomService.createRoom(any(), any()))
                .thenReturn(dto);

        MockMultipartFile room =
                new MockMultipartFile(
                        "room",
                        "",
                        "application/json",
                        """
                                {
                                  "name":"Sala Nueva",
                                  "capacity":10
                                }
                                """.getBytes()
                );

        mockMvc.perform(
                        multipart("/api/rooms")
                                .file(room)
                                .with(csrf())
                )
                .andExpect(status().isOk());
    }

    //actualizar sala si es admin
    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeActualizarSala() throws Exception {

        RoomDto dto = new RoomDto();
        dto.setId(1L);
        dto.setName("Sala Editada");

        when(roomService.updateRoom(eq(1L), any(RoomDto.class), any()))
                .thenReturn(Optional.of(dto));

        MockMultipartFile room =
                new MockMultipartFile(
                        "room",
                        "",
                        "application/json",
                        """
                                {
                                  "name":"Sala Editada"
                                }
                                """.getBytes()
                );

        mockMvc.perform(
                        multipart("/api/rooms/1")
                                .file(room)
                                .with(csrf())
                                .with(request -> {
                                    request.setMethod("PUT");
                                    return request;
                                })
                )
                .andExpect(status().isOk());
    }

}
