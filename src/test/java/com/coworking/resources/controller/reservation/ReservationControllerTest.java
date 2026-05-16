package com.coworking.resources.controller.reservation;

import com.coworking.reservation.controller.ReservationController;
import com.coworking.reservation.dto.ReservationRequest;
import com.coworking.reservation.dto.ReservationResponse;
import com.coworking.reservation.service.ReservationService;
import com.coworking.security.JwtAuthenticationFilter;
import com.coworking.security.JwtUtil;
import com.coworking.user.model.User;
import com.coworking.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReservationController.class)
@AutoConfigureMockMvc
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            invocation.<jakarta.servlet.FilterChain>getArgument(2)
                    .doFilter(
                            invocation.getArgument(0),
                            invocation.getArgument(1)
                    );
            return null;
        }).when(jwtAuthenticationFilter)
                .doFilter(any(), any(), any());
    }

    @Test
    void createReservation_shouldReturn200() throws Exception {

        ReservationRequest request = new ReservationRequest();
        request.setRoomId(1L);
        request.setStartAt(Instant.parse("2025-10-01T10:00:00Z"));
        request.setEndAt(Instant.parse("2025-10-01T12:00:00Z"));

        ReservationResponse response = new ReservationResponse();
        response.setId(1L);

        User userEntity = new User();
        userEntity.setId(1L);
        userEntity.setEmail("test@mail.com");

        when(userRepository.findByEmail("test@mail.com"))
                .thenReturn(Optional.of(userEntity));

        when(reservationService.createReservation(
                eq(1L),
                any(ReservationRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post("/api/reservations")
                        .with(user("test@mail.com").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnMyReservations() throws Exception {

        ReservationResponse response = new ReservationResponse();
        response.setId(1L);

        when(reservationService.getMyReservations("test@mail.com"))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/reservations/my")
                        .with(user("test@mail.com").roles("USER")))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }

    @Test
    void shouldCancelReservation() throws Exception {

        doNothing().when(reservationService)
                .cancelReservation(eq(1L), anyString());

        mockMvc.perform(
                        patch("/api/reservations/1/cancel")
                                .with(user("test@mail.com").roles("USER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        verify(reservationService)
                .cancelReservation(eq(1L), anyString());
    }
}