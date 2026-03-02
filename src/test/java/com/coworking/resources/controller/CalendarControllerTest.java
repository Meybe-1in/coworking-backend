package com.coworking.resources.controller;

import com.coworking.controller.ReservationController;
import com.coworking.dto.CalendarEventResponse;
import com.coworking.repository.UserRepository;
import com.coworking.security.JwtAuthenticationFilter;
import com.coworking.security.JwtUtil;
import com.coworking.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ReservationController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CalendarControllerTest {
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

    @Test
    void getCalendar_shouldReturnEvents() throws Exception {

        when(reservationService.getCalendar(any(), any()))
                .thenReturn(List.of(new CalendarEventResponse()));

        mockMvc.perform(get("/api/reservations/calendar")
                        .param("from", "2025-10-01T00:00:00")
                        .param("to", "2025-10-31T23:59:59"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnBadRequestWhenDateFormatIsInvalid() throws Exception {

        mockMvc.perform(get("/api/reservations/calendar")
                        .param("from", "2025-01-01") // formato incorrecto
                        .param("to", "2025-01-31"))
                .andExpect(status().isBadRequest());
    }
}
