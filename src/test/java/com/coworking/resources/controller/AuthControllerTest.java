package com.coworking.resources.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.coworking.repository.PasswordResetTokenRepository;
import com.coworking.repository.VerificationTokenRepository;
import com.coworking.service.EmailService;
import com.coworking.service.GoogleAuthService;
import com.coworking.service.PasswordResetService;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.coworking.controller.AuthController;
import com.coworking.dto.LoginRequest;
import com.coworking.dto.RegisterRequest;
import com.coworking.model.Role;
import com.coworking.model.User;
import com.coworking.repository.RoleRepository;
import com.coworking.repository.UserRepository;
import com.coworking.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private EmailService emailService;

    @MockitoBean
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private GoogleAuthService googleAuthService;

    // REGISTER TESTS

    @Test
    void shouldRegisterSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "userTest",
                "test@mail.com",
                "Aa123456!",
                true
        );

        Role role = new Role();
        role.setName("ROLE_USER");

        Mockito.when(userRepository.findByEmail("test@mail.com"))
                .thenReturn(Optional.empty());

        Mockito.when(roleRepository.findByName("ROLE_USER"))
                .thenReturn(Optional.of(role));

        Mockito.when(passwordEncoder.encode("Aa123456!"))
                .thenReturn("encodedPass");

        Mockito.when(jwtUtil.generateToken(any(UserDetails.class), eq("userTest"), anyBoolean()))
                .thenReturn("fake-jwt");

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
                "other",
                "duplicate@mail.com",
                "12345678",
                true
        );

        Mockito.when(userRepository.findByEmail("duplicate@mail.com"))
                .thenReturn(Optional.of(new User()));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("email"))
                .andExpect(jsonPath("$.message").value("Correo ya está registrado"));
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
        LoginRequest request = new LoginRequest("login@mail.com", "123456", false);

        User user = new User();
        user.setUsername("loginUser");
        user.setEmail("login@mail.com");
        user.setEnabled(true);
        user.setRoles(Set.of(new Role("ROLE_USER")));

        Authentication authMock = Mockito.mock(Authentication.class);

        Mockito.when(authenticationManager.authenticate(any()))
                .thenReturn(authMock);

        Mockito.when(userRepository.findByEmail("login@mail.com"))
                .thenReturn(Optional.of(user));

        Mockito.when(jwtUtil.generateToken(any(UserDetails.class), eq("loginUser"), eq(false)))
                .thenReturn("jwt-login");

        UserDetails springUser =
                new org.springframework.security.core.userdetails.User(
                        "login@mail.com",
                        "encoded",
                        List.of()
                );

        Mockito.when(authMock.getPrincipal()).thenReturn(springUser);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }


    @Test
    void shouldReturn401WhenBadCredentials() throws Exception {
        LoginRequest request = new LoginRequest("wrong@mail.com", "fail", false);

        Mockito.when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad creds"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo o contraseña incorrectos"));
    }

    @Test
    void shouldReturn400WhenPasswordIsWeak() throws Exception {
        RegisterRequest request = new RegisterRequest("User", "email@test.com", "12345", true);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("password"))
                .andExpect(jsonPath("$.message").exists());
    }


}
