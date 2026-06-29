package com.coworking.resources.controller.auth;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.coworking.auth.dto.AuthResponse;
import com.coworking.auth.dto.ForgotPasswordRequest;
import com.coworking.auth.repository.PasswordResetTokenRepository;
import com.coworking.auth.repository.VerificationTokenRepository;
import com.coworking.auth.service.AuthService;
import com.coworking.auth.service.GoogleAuthService;
import com.coworking.exception.BadRequestException;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.coworking.auth.controller.AuthController;
import com.coworking.auth.dto.LoginRequest;
import com.coworking.auth.dto.RegisterRequest;
import com.coworking.role.repository.RoleRepository;
import com.coworking.user.repository.UserRepository;
import com.coworking.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private VerificationTokenRepository verificationTokenRepository;

    @MockitoBean
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @MockitoBean
    private GoogleAuthService googleAuthService;

    @MockitoBean
    private AuthService authService;

    // REGISTER TESTS

    @Test
    void shouldRegisterSuccessfully() throws Exception {

        RegisterRequest request = new RegisterRequest(
                "userTest",
                "test@mail.com",
                "Aa123456!",
                true
        );

        when(authService.register(any()))
                .thenReturn("Registro exitoso. Revisa tu correo para activar tu cuenta.");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Registro exitoso. Revisa tu correo para activar tu cuenta."));
    }

    @Test
    void shouldReturn400WhenEmailAlreadyExists() throws Exception {

        RegisterRequest request = new RegisterRequest(
                "userTest",
                "test@mail.com",
                "Aa123456!",
                true
        );

        when(authService.register(any()))
                .thenThrow(new BadRequestException("Correo ya está registrado"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Correo ya está registrado"));
    }

    @Test
    void shouldReturn400WhenEmailNotVerified() throws Exception {

        LoginRequest request = new LoginRequest(
                "test@mail.com",
                "Aa123456!",
                false
        );

        when(authService.login(any()))
                .thenThrow(new BadRequestException("EMAIL_NOT_VERIFIED"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void shouldReturn400WhenEmailInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "user",
                "correo-malo",
                "12345678",
                true
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // LOGIN TESTS
    @Test
    void shouldLoginSuccessfully() throws Exception {

        LoginRequest request = new LoginRequest(
                "test@mail.com",
                "Aa123456!",
                false
        );

        AuthResponse response = new AuthResponse("token", "userTest", "ROLE_USER");

        when(authService.login(any()))
                .thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login exitoso"))
                .andExpect(jsonPath("$.data.token").value("token"));
    }


    @Test
    void shouldReturn401WhenBadCredentials() throws Exception {

        LoginRequest request = new LoginRequest(
                "test@mail.com",
                "wrongpass",
                false
        );

        when(authService.login(any()))
                .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Credenciales incorrectas"));
    }

    @Test
    void shouldReturn400WhenPasswordIsWeak() throws Exception {

        RegisterRequest request = new RegisterRequest(
                "User",
                "email@test.com",
                "12345",
                true
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldSendResetLink() throws Exception {

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@mail.com");

        when(authService.forgotPassword(any()))
                .thenReturn("Se enviará un enlace de recuperación a su correo registrado");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Se enviará un enlace de recuperación a su correo registrado"));
    }

}
