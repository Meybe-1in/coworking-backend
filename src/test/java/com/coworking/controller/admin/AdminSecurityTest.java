package com.coworking.controller.admin;

import com.coworking.admin.controller.AdminController;
import com.coworking.admin.dto.UpdateUserRequest;
import com.coworking.admin.dto.UserAdminResponse;
import com.coworking.admin.service.AdminService;
import com.coworking.security.AuthenticationEntryPointImpl;
import com.coworking.security.CustomUserDetailsService;
import com.coworking.security.JwtAuthenticationFilter;
import com.coworking.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc
class AdminSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

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

    @BeforeEach
    void setUp() throws Exception {

        doAnswer(invocation -> {

            FilterChain filterChain = invocation.getArgument(2);

            filterChain.doFilter(
                    invocation.getArgument(0),
                    invocation.getArgument(1)
            );

            return null;

        }).when(jwtAuthenticationFilter)
                .doFilter(any(), any(), any());
    }

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
                            .requestMatchers("/admin/**")
                            .hasRole("ADMIN")
                            .anyRequest()
                            .authenticated()
                    )
                    .build();
        }
    }

    @Test
    @DisplayName("GET /admin/stats - unauthenticated should return 401")
    void shouldReturn401WhenUnauthenticated() throws Exception {

        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /admin/stats - USER role should return 403")
    void shouldReturn403WhenUserRole() throws Exception {

        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(roles = "USER")
    void userShouldNotCancelReservation() throws Exception {

        mockMvc.perform(
                        patch("/admin/reservations/1/cancel")
                                .with(csrf())
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userShouldNotAccessUsers() throws Exception {

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldAccessUsers() throws Exception {

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userShouldNotCreateAdmin() throws Exception {

        mockMvc.perform(
                        post("/admin/users/admin")
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        {
                                          "username":"admin",
                                          "email":"admin@test.com",
                                          "password":"Password123."
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userShouldNotUpdateUserStatus() throws Exception {

        mockMvc.perform(
                        patch("/admin/users/1/status")
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        {
                                          "enabled": false
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldUpdateUserStatus() throws Exception {

        mockMvc.perform(
                        patch("/admin/users/1/status")
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        {
                                          "enabled": false
                                        }
                                        """)
                )
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userShouldNotUpdateUserRole() throws Exception {

        mockMvc.perform(
                        patch("/admin/users/1/role")
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        {
                                          "role":"ROLE_ADMIN"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldUpdateUserRole() throws Exception {

        mockMvc.perform(
                        patch("/admin/users/1/role")
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        {
                                          "role":"ROLE_ADMIN"
                                        }
                                        """)
                )
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userShouldNotUpdateUser() throws Exception {

        mockMvc.perform(
                        put("/admin/users/1")
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        {
                                          "username": "dayanaUpdated",
                                          "email": "dayana.updated@test.com",
                                          "role": "ROLE_USER"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());

        verify(adminService, never())
                .updateUser(
                        anyLong(),
                        any(UpdateUserRequest.class)
                );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldUpdateUser() throws Exception {

        when(adminService.updateUser(
                eq(1L),
                any(UpdateUserRequest.class)
        )).thenReturn(
                new UserAdminResponse()
        );

        mockMvc.perform(
                        put("/admin/users/1")
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        {
                                          "username": "dayanaUpdated",
                                          "email": "dayana.updated@test.com",
                                          "role": "ROLE_USER"
                                        }
                                        """)
                )
                .andExpect(status().isOk());
        verify(adminService).updateUser(
                eq(1L),
                any(UpdateUserRequest.class)
        );

    }
}
