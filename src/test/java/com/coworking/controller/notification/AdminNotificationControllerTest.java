package com.coworking.controller.notification;

import com.coworking.admin.notification.controller.AdminNotificationController;
import com.coworking.admin.notification.dto.NotificationResponse;
import com.coworking.admin.notification.enums.NotificationType;
import com.coworking.admin.notification.service.NotificationService;
import com.coworking.config.SecurityConfig;
import com.coworking.security.AuthenticationEntryPointImpl;
import com.coworking.security.CustomUserDetailsService;
import com.coworking.security.JwtAuthenticationFilter;
import com.coworking.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminNotificationController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class AdminNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private AuthenticationEntryPointImpl authenticationEntryPoint;

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
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void shouldGetNotificationsSuccessfully() throws Exception {

        NotificationResponse notification = new NotificationResponse(
                1L,
                NotificationType.RESERVATION_EXPIRED,
                "Reserva expirada",
                "La reserva #1 ha expirado.",
                "Reservation",
                1L,
                false,
                Instant.parse("2026-10-06T18:00:00Z")
        );

        when(notificationService.getNotifications("admin@test.com"))
                .thenReturn(List.of(notification));

        mockMvc.perform(get("/admin/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].type")
                        .value("RESERVATION_EXPIRED"))
                .andExpect(jsonPath("$[0].title")
                        .value("Reserva expirada"))
                .andExpect(jsonPath("$[0].message")
                        .value("La reserva #1 ha expirado."))
                .andExpect(jsonPath("$[0].entityType")
                        .value("Reservation"))
                .andExpect(jsonPath("$[0].entityId").value(1))
                .andExpect(jsonPath("$[0].read").value(false));

        verify(notificationService)
                .getNotifications("admin@test.com");
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void shouldMarkNotificationAsReadSuccessfully() throws Exception {

        mockMvc.perform(
                        patch("/admin/notifications/1/read")
                )
                .andExpect(status().isNoContent());

        verify(notificationService)
                .markAsRead(1L, "admin@test.com");
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void shouldMarkAllNotificationsAsReadSuccessfully() throws Exception {

        mockMvc.perform(
                        patch("/admin/notifications/read-all")
                )
                .andExpect(status().isNoContent());

        verify(notificationService)
                .markAllAsRead("admin@test.com");
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void shouldGetUnreadCountSuccessfully() throws Exception {

        when(notificationService.getUnreadCount("admin@test.com"))
                .thenReturn(3L);

        mockMvc.perform(get("/admin/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));

        verify(notificationService)
                .getUnreadCount("admin@test.com");
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void shouldRejectUserAccess() throws Exception {

        mockMvc.perform(get("/admin/notifications"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(notificationService);
    }
}