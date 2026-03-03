package com.coworking.resources.controller;

import com.coworking.controller.RoomController;
import com.coworking.dto.RoomDto;
import com.coworking.security.JwtUtil;
import com.coworking.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void available_debeRetornar400_siStartEsMayorQueEnd() throws Exception {

        mockMvc.perform(get("/api/rooms/available")
                        .param("date", "2026-01-01")
                        .param("start", "10:00")
                        .param("end", "08:00")
                        .param("people", "2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllRooms_debeRetornar200() throws Exception {

        when(roomService.getAllRooms())
                .thenReturn(List.of(new RoomDto()));

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk());
    }
}