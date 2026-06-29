package com.coworking.resources.controller.admin;

import com.coworking.admin.controller.AdminController;
import com.coworking.admin.dto.AdminStatsResponse;
import com.coworking.admin.dto.CreateAdminRequest;
import com.coworking.admin.dto.UpdateUserStatusRequest;
import com.coworking.admin.dto.UserAdminResponse;
import com.coworking.admin.service.AdminService;
import com.coworking.payment.dto.PaymentResponse;
import com.coworking.payment.enums.PaymentStatus;
import com.coworking.reservation.dto.ReservationResponse;
import com.coworking.reservation.enums.ReservationStatus;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

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
    void shouldReturnAdminStats() throws Exception {

        AdminStatsResponse response = AdminStatsResponse.builder()
                .totalReservations(10)
                .activeReservations(5)
                .pendingReservations(2)
                .cancelledReservations(1)
                .expiredReservations(2)
                .totalRevenue(BigDecimal.valueOf(500))
                .monthlyRevenue(BigDecimal.valueOf(200))
                .build();

        when(adminService.getStats())
                .thenReturn(response);

        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReservations").value(10))
                .andExpect(jsonPath("$.totalRevenue").value(500));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnAllReservations() throws Exception {

        ReservationResponse reservation = new ReservationResponse();

        reservation.setId(1L);
        reservation.setRoomName("Sala Privada");
        reservation.setUsername("dayana");
        reservation.setStatus(ReservationStatus.PAID);

        when(adminService.getAllReservations())
                .thenReturn(List.of(reservation));

        mockMvc.perform(get("/admin/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomName")
                        .value("Sala Privada"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnAllPayments() throws Exception {

        PaymentResponse payment = PaymentResponse.builder()
                .id(1L)
                .roomName("Sala Premium")
                .amount(BigDecimal.valueOf(25))
                .currency("usd")
                .status(PaymentStatus.SUCCEEDED)
                .paymentMethod("Stripe")
                .paidAt(Instant.now())
                .build();

        when(adminService.getAllPayments())
                .thenReturn(List.of(payment));

        mockMvc.perform(get("/admin/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomName")
                        .value("Sala Premium"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCancelReservation() throws Exception {

        doNothing().when(adminService)
                .cancelReservation(1L);

        mockMvc.perform(
                        patch("/admin/reservations/1/cancel")
                                .with(csrf())
                )
                .andExpect(status().isOk());

        verify(adminService)
                .cancelReservation(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnAllUsers() throws Exception {
        UserAdminResponse user =
                new UserAdminResponse(
                        1L,
                        "dayana",
                        "dayana@gmail.com",
                        Set.of("ROLE_USER"),
                        true,
                        true,
                        LocalDateTime.now()
                );

        when(adminService.getAllUsers())
                .thenReturn(List.of(user));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username")
                        .value("dayana"))
                .andExpect(jsonPath("$[0].email")
                        .value("dayana@gmail.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateAdmin() throws Exception {

        CreateAdminRequest request = new CreateAdminRequest();

        request.setUsername("admin2");
        request.setEmail("admin@test.com");
        request.setPassword("Password123.");

        UserAdminResponse response =
                new UserAdminResponse(
                        10L,
                        "admin2",
                        "admin@test.com",
                        Set.of("ROLE_ADMIN"),
                        true,
                        true,
                        LocalDateTime.now()
                );

        when(adminService.createAdmin(any()))
                .thenReturn(response);

        mockMvc.perform(
                        post("/admin/users/admin")
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username")
                        .value("admin2"))
                .andExpect(jsonPath("$.email")
                        .value("admin@test.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateUserStatus() throws Exception {

        UpdateUserStatusRequest request =
                new UpdateUserStatusRequest(false);

        UserAdminResponse response =
                new UserAdminResponse(
                        1L,
                        "dayana",
                        "dayana@test.com",
                        Set.of("ROLE_USER"),
                        false,
                        false,
                        LocalDateTime.now()
                );

        when(adminService.updateUserStatus(
                eq(1L),
                any(UpdateUserStatusRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        patch("/admin/users/1/status")
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled")
                        .value(false));
    }

}