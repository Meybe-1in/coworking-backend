package com.coworking.controller.settingsAdmin;

import com.coworking.admin.settings.controller.SystemSettingsController;
import com.coworking.admin.settings.dto.SystemSettingsResponse;
import com.coworking.admin.settings.dto.UpdateSystemSettingsRequest;
import com.coworking.admin.settings.service.SystemSettingsService;
import com.coworking.security.JwtAuthenticationFilter;
import com.coworking.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SystemSettingsController.class)
@AutoConfigureMockMvc
class SystemSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SystemSettingsService systemSettingsService;

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
    @WithMockUser(roles = "ADMIN")
    void shouldReturnSystemSettings() throws Exception {

        SystemSettingsResponse response =
                new SystemSettingsResponse(
                        LocalTime.of(7, 0),
                        LocalTime.of(20, 0),
                        8,
                        15,
                        "Coworking Platform",
                        "contacto@coworking.com",
                        "2667-0000",
                        "San Miguel, El Salvador",
                        Instant.now()
                );

        when(systemSettingsService.getSettings())
                .thenReturn(response);

        mockMvc.perform(
                        get("/admin/settings")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingTime")
                        .value("07:00:00"))
                .andExpect(jsonPath("$.closingTime")
                        .value("20:00:00"))
                .andExpect(jsonPath("$.maxReservationHours")
                        .value(8))
                .andExpect(jsonPath("$.pendingExpirationMinutes")
                        .value(15))
                .andExpect(jsonPath("$.institutionName")
                        .value("Coworking Platform"))
                .andExpect(jsonPath("$.institutionEmail")
                        .value("contacto@coworking.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateSystemSettings() throws Exception {

        UpdateSystemSettingsRequest request =
                new UpdateSystemSettingsRequest(
                        LocalTime.of(8, 0),
                        LocalTime.of(21, 0),
                        6,
                        20,
                        "Coworking San Miguel",
                        "contacto@coworking.com",
                        "2667-0000",
                        "San Miguel, El Salvador"
                );

        SystemSettingsResponse response =
                new SystemSettingsResponse(
                        LocalTime.of(8, 0),
                        LocalTime.of(21, 0),
                        6,
                        20,
                        "Coworking San Miguel",
                        "contacto@coworking.com",
                        "2667-0000",
                        "San Miguel, El Salvador",
                        Instant.now()
                );

        when(systemSettingsService.updateSettings(any(
                UpdateSystemSettingsRequest.class
        ))).thenReturn(response);

        mockMvc.perform(
                        put("/admin/settings")
                                .with(csrf())
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingTime")
                        .value("08:00:00"))
                .andExpect(jsonPath("$.closingTime")
                        .value("21:00:00"))
                .andExpect(jsonPath("$.maxReservationHours")
                        .value(6))
                .andExpect(jsonPath("$.pendingExpirationMinutes")
                        .value(20))
                .andExpect(jsonPath("$.institutionName")
                        .value("Coworking San Miguel"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDelegateInvalidSystemSettingsToService() throws Exception {

        UpdateSystemSettingsRequest request =
                new UpdateSystemSettingsRequest(
                        LocalTime.of(20, 0),
                        LocalTime.of(7, 0),
                        8,
                        15,
                        "Coworking Platform",
                        "contacto@coworking.com",
                        "2667-0000",
                        "San Miguel, El Salvador"
                );

        when(systemSettingsService.updateSettings(any(
                UpdateSystemSettingsRequest.class
        ))).thenThrow(
                new com.coworking.exception.BadRequestException(
                        "La hora de apertura debe ser anterior a la hora de cierre"
                )
        );

        mockMvc.perform(
                        put("/admin/settings")
                                .with(csrf())
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectMissingRequiredFields() throws Exception {

        UpdateSystemSettingsRequest request =
                new UpdateSystemSettingsRequest(
                        null,
                        null,
                        null,
                        null,
                        "",
                        "correo-invalido",
                        null,
                        null
                );

        mockMvc.perform(
                        put("/admin/settings")
                                .with(csrf())
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }
}