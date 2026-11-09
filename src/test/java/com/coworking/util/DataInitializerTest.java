package com.coworking.util;

import com.coworking.role.model.Role;
import com.coworking.role.repository.RoleRepository;
import com.coworking.user.model.User;
import com.coworking.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DataInitializer dataInitializer;

    private final String adminUsername = "local-admin";
    private final String adminEmail = "local-admin@example.com";
    private final String adminPassword = "local-secure-password";

    private Role adminRole;

    @BeforeEach
    void setUp() {
        dataInitializer = new DataInitializer(
                roleRepository,
                userRepository,
                passwordEncoder,
                adminUsername,
                adminEmail,
                adminPassword
        );

        adminRole = new Role("ROLE_ADMIN");
    }

    @Test
    void shouldCreateAdminWhenUserDoesNotExist() {
        when(userRepository.existsByUsername(adminUsername)).thenReturn(false);
        when(userRepository.existsByEmail(adminEmail)).thenReturn(false);
        when(roleRepository.findByName("ROLE_ADMIN"))
                .thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode(adminPassword))
                .thenReturn("encoded-password");

        dataInitializer.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals(adminUsername, savedUser.getUsername());
        assertEquals(adminEmail, savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPassword());
        assertTrue(savedUser.isEnabled());
        assertTrue(savedUser.isEmailVerified());
        assertTrue(savedUser.getRoles().contains(adminRole));

        verify(passwordEncoder).encode(adminPassword);
    }

    @Test
    void shouldNotCreateAdminWhenUsernameAlreadyExists() {
        when(userRepository.existsByUsername(adminUsername)).thenReturn(true);
        when(userRepository.existsByEmail(adminEmail)).thenReturn(false);

        dataInitializer.run();

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
        verifyNoInteractions(roleRepository);
    }

    @Test
    void shouldNotCreateAdminWhenEmailAlreadyExists() {
        when(userRepository.existsByUsername(adminUsername)).thenReturn(false);
        when(userRepository.existsByEmail(adminEmail)).thenReturn(true);

        dataInitializer.run();

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
        verifyNoInteractions(roleRepository);
    }

    @Test
    void shouldFailWhenAdminRoleDoesNotExist() {
        when(userRepository.existsByUsername(adminUsername)).thenReturn(false);
        when(userRepository.existsByEmail(adminEmail)).thenReturn(false);
        when(roleRepository.findByName("ROLE_ADMIN"))
                .thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> dataInitializer.run()
        );

        assertTrue(exception.getMessage().contains("ROLE_ADMIN"));

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldFailWhenAdminConfigurationIsMissing() {
        DataInitializer initializerWithMissingConfiguration =
                new DataInitializer(
                        roleRepository,
                        userRepository,
                        passwordEncoder,
                        "",
                        adminEmail,
                        adminPassword
                );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                initializerWithMissingConfiguration::run
        );

        assertTrue(exception.getMessage().contains("ADMIN_USERNAME"));

        verifyNoInteractions(userRepository, roleRepository, passwordEncoder);
    }
}