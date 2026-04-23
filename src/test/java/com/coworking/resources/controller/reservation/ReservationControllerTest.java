package com.coworking.resources.controller.reservation;


import com.coworking.reservation.controller.ReservationController;
import com.coworking.reservation.dto.ReservationRequest;
import com.coworking.reservation.dto.ReservationResponse;
import com.coworking.user.model.User;
import com.coworking.user.repository.UserRepository;
import com.coworking.security.JwtAuthenticationFilter;
import com.coworking.security.JwtUtil;
import com.coworking.reservation.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@AutoConfigureMockMvc
public class ReservationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void createReservation_shouldReturn200() throws Exception {

        ReservationRequest request = new ReservationRequest();
        request.setRoomId(1L);
        request.setStartAt(Instant.parse("2025-10-01T10:00:00Z"));
        request.setEndAt(Instant.parse("2025-10-01T12:00:00Z"));

        User user = new User();
        user.setId(1L);
        user.setEmail("user@mail.com");

        when(userRepository.findByEmail("user@mail.com"))
                .thenReturn(Optional.of(user));

        when(reservationService.createReservation(eq(1L), any()))
                .thenReturn(new ReservationResponse());

        mockMvc.perform(post("/api/reservations")
                        .with(user("user@mail.com"))
                        .with(csrf())
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

}
