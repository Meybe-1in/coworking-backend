package com.coworking.resources.controller.reservation;

import com.coworking.security.AuthenticationEntryPointImpl;
import com.coworking.reservation.controller.ReservationController;
import com.coworking.user.repository.UserRepository;
import com.coworking.security.CustomUserDetailsService;
import com.coworking.reservation.service.ReservationService;
import com.coworking.security.JwtAuthenticationFilter;
import com.coworking.security.JwtUtil;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReservationController.class)
@AutoConfigureMockMvc
class ReservationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private AuthenticationEntryPointImpl authenticationEntryPoint;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .exceptionHandling(ex ->
                            ex.authenticationEntryPoint(
                                    (request, response, authException) ->
                                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                            )
                    )
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(HttpMethod.DELETE, "/api/reservations/**")
                            .hasRole("ADMIN")
                            .anyRequest().authenticated()
                    )
                    .build();
        }
    }

    @Test
    @DisplayName("POST /api/reservations - Unauthenticated user should return 401")
    void createReservation_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/reservations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/reservations - Authenticated USER should not return 401")
    void createReservation_authenticated_shouldNotReturn401() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is2xxSuccessful());
        // 400 por body inválido, pero NO 401
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /api/reservations/{id} - USER role should return 403")
    void deleteReservation_userRole_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/reservations/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/reservations/{id} - ADMIN role should pass security")
    void deleteReservation_adminRole_shouldPassSecurity() throws Exception {

        doNothing().when(reservationService).deleteReservation(anyLong());

        mockMvc.perform(delete("/api/reservations/1"))
                .andExpect(status().isOk());
    }
}