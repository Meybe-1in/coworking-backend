package com.coworking.controller.settingsAdmin;

import com.coworking.admin.settings.controller.PublicSettingsController;
import com.coworking.admin.settings.entity.SystemSettings;
import com.coworking.admin.settings.service.SystemSettingsService;
import com.coworking.security.JwtAuthenticationFilter;
import com.coworking.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicSettingsController.class)
@AutoConfigureMockMvc
class PublicSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    @WithMockUser(roles = "USER")
    void shouldReturnReservationSettingsForUser() throws Exception {

        SystemSettings settings = createSettings();

        when(systemSettingsService.getCurrentSettings())
                .thenReturn(settings);

        mockMvc.perform(
                        get("/api/settings/reservation")
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
                        .doesNotExist())
                .andExpect(jsonPath("$.institutionEmail")
                        .doesNotExist())
                .andExpect(jsonPath("$.institutionPhone")
                        .doesNotExist())
                .andExpect(jsonPath("$.institutionAddress")
                        .doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnReservationSettingsForAdmin() throws Exception {

        SystemSettings settings = createSettings();

        when(systemSettingsService.getCurrentSettings())
                .thenReturn(settings);

        mockMvc.perform(
                        get("/api/settings/reservation")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingTime")
                        .value("07:00:00"))
                .andExpect(jsonPath("$.closingTime")
                        .value("20:00:00"))
                .andExpect(jsonPath("$.maxReservationHours")
                        .value(8))
                .andExpect(jsonPath("$.pendingExpirationMinutes")
                        .value(15));
    }

    private SystemSettings createSettings() {

        SystemSettings settings = new SystemSettings();

        settings.setOpeningTime(LocalTime.of(7, 0));
        settings.setClosingTime(LocalTime.of(20, 0));
        settings.setMaxReservationHours(8);
        settings.setPendingExpirationMinutes(15);

        settings.setInstitutionName("Coworking Platform");
        settings.setInstitutionEmail("admin@coworking.com");
        settings.setInstitutionPhone("2222-2222");
        settings.setInstitutionAddress("San Miguel, El Salvador");

        return settings;
    }
}