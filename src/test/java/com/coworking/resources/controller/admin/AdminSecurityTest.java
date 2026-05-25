package com.coworking.resources.controller.admin;

import com.coworking.admin.controller.AdminController;
import com.coworking.admin.service.AdminService;
import com.coworking.security.AuthenticationEntryPointImpl;
import com.coworking.security.CustomUserDetailsService;
import com.coworking.security.JwtAuthenticationFilter;
import com.coworking.security.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/stats - ADMIN role should pass")
    void shouldAllowAdminAccess() throws Exception {

        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isOk());
    }
}
