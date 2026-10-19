package com.coworking.service.auth;

import com.coworking.auth.dto.AuthResponse;
import com.coworking.auth.dto.GoogleUserInfo;
import com.coworking.auth.service.GoogleAuthService;
import com.coworking.role.model.Role;
import com.coworking.role.repository.RoleRepository;
import com.coworking.security.JwtUtil;
import com.coworking.user.model.User;
import com.coworking.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    private static final String GOOGLE_URL =
            "https://www.googleapis.com/oauth2/v3/userinfo";

    private static final String ACCESS_TOKEN = "google-access-token";
    private static final String EMAIL = "google@test.com";
    private static final String USERNAME = "Google User";
    private static final String APP_TOKEN = "application-jwt";
    private static final String USER_ROLE = "ROLE_USER";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GoogleAuthService googleAuthService;

    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setName(USER_ROLE);
    }

    // --------------------------------------------------
    // NUEVO USUARIO
    // --------------------------------------------------

    @Test
    void shouldRegisterGoogleUserSuccessfully() {
        mockVerifiedGoogleUser();
        mockJwtGeneration();

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        when(roleRepository.findByName(USER_ROLE))
                .thenReturn(Optional.of(userRole));

        when(passwordEncoder.encode(anyString()))
                .thenReturn("encoded-random-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response =
                googleAuthService.authenticate(ACCESS_TOKEN, false);

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();

        assertEquals(EMAIL, savedUser.getEmail());
        assertNotNull(savedUser.getUsername());
        assertFalse(savedUser.getUsername().isBlank());
        assertEquals("encoded-random-password", savedUser.getPassword());
        assertTrue(savedUser.isEnabled());
        assertTrue(savedUser.isEmailVerified());
        assertTrue(savedUser.getRoles().contains(userRole));

        assertEquals(APP_TOKEN, response.token());
        assertEquals(savedUser.getUsername(), response.username());
        assertEquals(USER_ROLE, response.role());

        verify(passwordEncoder).encode(anyString());
        verify(jwtUtil).generateToken(
                any(UserDetails.class),
                eq(savedUser.getUsername()),
                eq(false)
        );
    }

    @Test
    void shouldRejectRegistrationWhenDefaultRoleDoesNotExist() {
        mockVerifiedGoogleUser();

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.empty());

        when(roleRepository.findByName(USER_ROLE))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> googleAuthService.authenticate(ACCESS_TOKEN, false)
        );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getStatusCode()
        );

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtUtil);
    }

    // --------------------------------------------------
    // USUARIO EXISTENTE
    // --------------------------------------------------

    @Test
    void shouldAuthenticateExistingGoogleUser() {
        mockVerifiedGoogleUser();
        mockJwtGeneration();

        User existingUser = createUser(true, true);

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(existingUser));

        AuthResponse response =
                googleAuthService.authenticate(ACCESS_TOKEN, true);

        assertEquals(APP_TOKEN, response.token());
        assertEquals(existingUser.getUsername(), response.username());
        assertEquals(USER_ROLE, response.role());

        verify(userRepository, never()).save(any(User.class));
        verify(roleRepository, never()).findByName(anyString());
        verify(passwordEncoder, never()).encode(anyString());

        verify(jwtUtil).generateToken(
                any(UserDetails.class),
                eq(existingUser.getUsername()),
                eq(true)
        );
    }

    // --------------------------------------------------
    // TOKEN DE GOOGLE INVÁLIDO
    // --------------------------------------------------

    @Test
    void shouldRejectBlankGoogleAccessToken() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> googleAuthService.authenticate(" ", false)
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );

        verifyNoInteractions(restTemplate);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void shouldRejectNullGoogleAccessToken() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> googleAuthService.authenticate(null, false)
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );

        verifyNoInteractions(restTemplate);
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldRejectWhenGoogleUserInfoRequestFails() {
        when(restTemplate.exchange(
                eq(GOOGLE_URL),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(GoogleUserInfo.class)
        )).thenThrow(new RestClientException("Google no disponible"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> googleAuthService.authenticate(ACCESS_TOKEN, false)
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(jwtUtil);
    }

    // --------------------------------------------------
    // CORREO DE GOOGLE NO VERIFICADO
    // --------------------------------------------------

    @Test
    void shouldRejectGoogleUserWithUnverifiedEmail() {
        GoogleUserInfo unverifiedUser = new GoogleUserInfo(
                "google-subject",
                EMAIL,
                USERNAME,
                false
        );

        mockGoogleUser(unverifiedUser);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> googleAuthService.authenticate(ACCESS_TOKEN, false)
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(roleRepository);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void shouldRejectGoogleUserWithoutEmail() {
        GoogleUserInfo userWithoutEmail = new GoogleUserInfo(
                "google-subject",
                null,
                USERNAME,
                true
        );

        mockGoogleUser(userWithoutEmail);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> googleAuthService.authenticate(ACCESS_TOKEN, false)
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(jwtUtil);
    }

    // --------------------------------------------------
    // CUENTA DESHABILITADA
    // --------------------------------------------------

    @Test
    void shouldRejectDisabledAccount() {
        mockVerifiedGoogleUser();

        User disabledUser = createUser(false, true);

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(disabledUser));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> googleAuthService.authenticate(ACCESS_TOKEN, false)
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(jwtUtil);
    }

    // --------------------------------------------------
    // VERIFICACIÓN DEL CORREO DE UNA CUENTA EXISTENTE
    // --------------------------------------------------

    @Test
    void shouldVerifyExistingAccountWhenGoogleConfirmsEmail() {
        mockVerifiedGoogleUser();
        mockJwtGeneration();

        // La cuenta está habilitada, pero su correo todavía no está verificado.
        User existingUser = createUser(true, false);

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(existingUser));

        when(userRepository.save(existingUser))
                .thenReturn(existingUser);

        AuthResponse response =
                googleAuthService.authenticate(ACCESS_TOKEN, false);

        assertTrue(existingUser.isEnabled());
        assertTrue(existingUser.isEmailVerified());
        assertEquals(APP_TOKEN, response.token());

        verify(userRepository).save(existingUser);

        verify(jwtUtil).generateToken(
                any(UserDetails.class),
                eq(existingUser.getUsername()),
                eq(false)
        );
    }

    // --------------------------------------------------
    // MÉTODOS AUXILIARES REUTILIZABLES
    // --------------------------------------------------

    private void mockVerifiedGoogleUser() {
        mockGoogleUser(verifiedGoogleUser());
    }

    private void mockGoogleUser(GoogleUserInfo googleUser) {
        when(restTemplate.exchange(
                eq(GOOGLE_URL),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(GoogleUserInfo.class)
        )).thenReturn(ResponseEntity.ok(googleUser));
    }

    private void mockJwtGeneration() {
        when(jwtUtil.generateToken(
                any(UserDetails.class),
                anyString(),
                anyBoolean()
        )).thenReturn(APP_TOKEN);
    }

    private GoogleUserInfo verifiedGoogleUser() {
        return new GoogleUserInfo(
                "google-subject",
                EMAIL,
                USERNAME,
                true
        );
    }

    private User createUser(
            boolean enabled,
            boolean emailVerified
    ) {
        User user = new User();

        user.setEmail(EMAIL);
        user.setUsername("existing-user");
        user.setPassword("encoded-existing-password");
        user.setEnabled(enabled);
        user.setEmailVerified(emailVerified);
        user.setRoles(new HashSet<>(Set.of(userRole)));

        return user;
    }
}