package com.coworking.controller.admin;

import com.coworking.admin.controller.AdminController;
import com.coworking.admin.dto.*;
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
    void shouldReturnPagedReservations() throws Exception {

        ReservationResponse reservation = new ReservationResponse();

        reservation.setId(1L);
        reservation.setRoomName("Sala Privada");
        reservation.setUsername("dayana");
        reservation.setStatus(ReservationStatus.PAID);

        AdminPageResponse<ReservationResponse> response =
                AdminPageResponse.<ReservationResponse>builder()
                        .content(List.of(reservation))
                        .page(0)
                        .size(10)
                        .totalElements(1)
                        .totalPages(1)
                        .first(true)
                        .last(true)
                        .build();

        when(adminService.getReservations(0, 10))
                .thenReturn(response);

        mockMvc.perform(
                        get("/admin/reservations")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].roomName")
                        .value("Sala Privada"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnPagedPayments() throws Exception {

        PaymentResponse payment = PaymentResponse.builder()
                .id(1L)
                .roomName("Sala Premium")
                .amount(BigDecimal.valueOf(25))
                .currency("usd")
                .status(PaymentStatus.SUCCEEDED)
                .paymentMethod("Stripe")
                .paidAt(Instant.now())
                .build();

        AdminPageResponse<PaymentResponse> response =
                AdminPageResponse.<PaymentResponse>builder()
                        .content(List.of(payment))
                        .page(0)
                        .size(10)
                        .totalElements(1)
                        .totalPages(1)
                        .first(true)
                        .last(true)
                        .build();

        when(adminService.getPayments(0, 10))
                .thenReturn(response);

        mockMvc.perform(
                        get("/admin/payments")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].roomName")
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
    void shouldReturnPagedUsers() throws Exception {

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

        AdminPageResponse<UserAdminResponse> response =
                AdminPageResponse.<UserAdminResponse>builder()
                        .content(List.of(user))
                        .page(0)
                        .size(10)
                        .totalElements(1)
                        .totalPages(1)
                        .first(true)
                        .last(true)
                        .build();

        when(adminService.getUsers(0, 10))
                .thenReturn(response);

        mockMvc.perform(
                        get("/admin/users")
                                .param("page", "0")
                                .param("size", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username")
                        .value("dayana"))
                .andExpect(jsonPath("$.content[0].email")
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

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateUserRole() throws Exception {

        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest("ROLE_ADMIN");

        UserAdminResponse response =
                new UserAdminResponse(
                        1L,
                        "dayana",
                        "dayana@test.com",
                        Set.of("ROLE_ADMIN"),
                        true,
                        true,
                        LocalDateTime.now()
                );

        when(adminService.updateUserRole(
                eq(1L),
                any(UpdateUserRoleRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        patch("/admin/users/1/role")
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]")
                        .value("ROLE_ADMIN"));
    }
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateUser() throws Exception {

        UpdateUserRequest request =
                new UpdateUserRequest(
                        "dayanaUpdated",
                        "dayana.updated@test.com",
                        "ROLE_USER"
                );

        UserAdminResponse response =
                new UserAdminResponse(
                        1L,
                        "dayanaUpdated",
                        "dayana.updated@test.com",
                        Set.of("ROLE_USER"),
                        true,
                        true,
                        LocalDateTime.now()
                );

        when(adminService.updateUser(
                eq(1L),
                any(UpdateUserRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        put("/admin/users/1")
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username")
                        .value("dayanaUpdated"))
                .andExpect(jsonPath("$.email")
                        .value("dayana.updated@test.com"))
                .andExpect(jsonPath("$.roles[0]")
                        .value("ROLE_USER"));

        verify(adminService).updateUser(
                eq(1L),
                any(UpdateUserRequest.class)
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectUpdateUserWithBlankUsername() throws Exception {

        UpdateUserRequest request =
                new UpdateUserRequest(
                        "",
                        "dayana@test.com",
                        "ROLE_USER"
                );

        mockMvc.perform(
                        put("/admin/users/1")
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isBadRequest());

        verify(adminService, never())
                .updateUser(
                        anyLong(),
                        any(UpdateUserRequest.class)
                );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectUpdateUserWithInvalidEmail() throws Exception {

        UpdateUserRequest request =
                new UpdateUserRequest(
                        "dayanaUpdated",
                        "correo-invalido",
                        "ROLE_USER"
                );

        mockMvc.perform(
                        put("/admin/users/1")
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isBadRequest());

        verify(adminService, never())
                .updateUser(
                        anyLong(),
                        any(UpdateUserRequest.class)
                );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectUpdateUserWithBlankRole() throws Exception {

        UpdateUserRequest request =
                new UpdateUserRequest(
                        "dayanaUpdated",
                        "dayana.updated@test.com",
                        ""
                );

        mockMvc.perform(
                        put("/admin/users/1")
                                .with(csrf())
                                .contentType("application/json")
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isBadRequest());

        verify(adminService, never())
                .updateUser(
                        anyLong(),
                        any(UpdateUserRequest.class)
                );
    }

}