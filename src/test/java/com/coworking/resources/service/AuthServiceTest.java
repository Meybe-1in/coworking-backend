package com.coworking.resources.service;

import com.coworking.auth.dto.ForgotPasswordRequest;
import com.coworking.auth.dto.LoginRequest;
import com.coworking.auth.dto.RegisterRequest;
import com.coworking.auth.repository.PasswordResetTokenRepository;
import com.coworking.auth.repository.VerificationTokenRepository;
import com.coworking.auth.service.AuthService;
import com.coworking.auth.service.PasswordResetService;
import com.coworking.email.service.EmailService;
import com.coworking.exception.BadRequestException;
import com.coworking.exception.NotFoundException;
import com.coworking.role.model.Role;
import com.coworking.role.repository.RoleRepository;
import com.coworking.security.JwtUtil;
import com.coworking.user.model.User;
import com.coworking.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PasswordResetService passwordResetService;
    @Mock private VerificationTokenRepository verificationTokenRepository;
    @Mock private EmailService emailService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    // REGISTER

    @Test
    void shouldRegisterSuccessfully() {
        RegisterRequest request = new RegisterRequest(
                "user",
                "test@mail.com",
                "Aa123456!",
                true
        );

        Role role = new Role();
        role.setName("ROLE_USER");

        when(userRepository.findByEmail("test@mail.com"))
                .thenReturn(Optional.empty());

        when(roleRepository.findByName("ROLE_USER"))
                .thenReturn(Optional.of(role));

        when(passwordEncoder.encode(any()))
                .thenReturn("encoded");

        String result = authService.register(request);

        assertEquals("Registro exitoso. Revisa tu correo para activar tu cuenta.", result);
        verify(userRepository).save(any(User.class));
    }
    //LOGIN
    @Test
    void shouldThrowWhenBadCredentials() {

        LoginRequest request = new LoginRequest(
                "test@mail.com",
                "wrongpass",
                false
        );

        User user = new User();
        user.setEnabled(true);

        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.of(user));

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(request));
    }
    //ROLE
    @Test
    void shouldThrowWhenRoleNotFound() {

        RegisterRequest request = new RegisterRequest(
                "user",
                "test@mail.com",
                "Aa123456!",
                true
        );

        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.empty());

        when(roleRepository.findByName("ROLE_USER"))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> authService.register(request));
    }

    //EMAIL
    @Test
    void shouldThrowWhenEmailNotVerified() {

        LoginRequest request = new LoginRequest(
                "test@mail.com",
                "Aa123456!",
                false
        );

        User user = new User();
        user.setEnabled(false);

        when(userRepository.findByEmail(any()))
                .thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class,
                () -> authService.login(request));
    }
    @Test
    void shouldThrowWhenEmailExists() {
        RegisterRequest request = new RegisterRequest(
                "user",
                "test@mail.com",
                "Aa123456!",
                true
        );

        when(userRepository.findByEmail("test@mail.com"))
                .thenReturn(Optional.of(new User()));

        assertThrows(BadRequestException.class,
                () -> authService.register(request));
    }

    // PASSWORD

    @Test
    void shouldCallPasswordResetService() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@mail.com");

        String result = authService.forgotPassword(request);

        verify(passwordResetService)
                .sendResetLink("test@mail.com");

        assertEquals("Se enviará un enlace de recuperación a su correo registrado", result);
    }

    @Test
    void shouldThrowWhenPasswordIsWeak() {

        RegisterRequest request = new RegisterRequest(
                "user",
                "test@mail.com",
                "12345",
                true
        );

        assertThrows(BadRequestException.class,
                () -> authService.register(request));
    }

    //VALIDACIONES
    @Test
    void shouldThrowWhenTermsNotAccepted() {

        RegisterRequest request = new RegisterRequest(
                "user",
                "test@mail.com",
                "Aa123456!",
                false
        );

        assertThrows(BadRequestException.class,
                () -> authService.register(request));
    }
}
