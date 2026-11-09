
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
class AdminBootstrapInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminBootstrapInitializer initializer;

    private final String username = "production-admin";
    private final String email = "admin@example.com";
    private final String password = "secure-password";

    private Role adminRole;

    @BeforeEach
    void setUp() {
        initializer = new AdminBootstrapInitializer(
                userRepository,
                roleRepository,
                passwordEncoder,
                username,
                email,
                password
        );

        adminRole = new Role("ROLE_ADMIN");
    }

    @Test
    void shouldCreateAdminSuccessfully() {
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(roleRepository.findByName("ROLE_ADMIN"))
                .thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode(password))
                .thenReturn("encoded-password");

        initializer.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();

        assertEquals(username, savedUser.getUsername());
        assertEquals(email, savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPassword());
        assertTrue(savedUser.isEnabled());
        assertTrue(savedUser.isEmailVerified());
        assertTrue(savedUser.getRoles().contains(adminRole));

        verify(passwordEncoder).encode(password);
    }

    @Test
    void shouldNotCreateAdminWhenUsernameAlreadyExists() {
        when(userRepository.existsByUsername(username)).thenReturn(true);
        when(userRepository.existsByEmail(email)).thenReturn(false);

        initializer.run();

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(roleRepository, passwordEncoder);
    }

    @Test
    void shouldNotCreateAdminWhenEmailAlreadyExists() {
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        initializer.run();

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(roleRepository, passwordEncoder);
    }

    @Test
    void shouldFailWhenAdminRoleDoesNotExist() {
        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(roleRepository.findByName("ROLE_ADMIN"))
                .thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> initializer.run()
        );

        assertTrue(exception.getMessage().contains("ROLE_ADMIN"));
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldFailWhenUsernameIsMissing() {
        AdminBootstrapInitializer invalidInitializer =
                new AdminBootstrapInitializer(
                        userRepository,
                        roleRepository,
                        passwordEncoder,
                        "",
                        email,
                        password
                );

        assertThrows(
                IllegalStateException.class,
                () -> invalidInitializer.run()
        );

        verifyNoInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void shouldFailWhenEmailIsMissing() {
        AdminBootstrapInitializer invalidInitializer =
                new AdminBootstrapInitializer(
                        userRepository,
                        roleRepository,
                        passwordEncoder,
                        username,
                        "",
                        password
                );

        assertThrows(
                IllegalStateException.class,
                () -> invalidInitializer.run()
        );

        verifyNoInteractions(userRepository, roleRepository, passwordEncoder);
    }

    @Test
    void shouldFailWhenPasswordIsMissing() {
        AdminBootstrapInitializer invalidInitializer =
                new AdminBootstrapInitializer(
                        userRepository,
                        roleRepository,
                        passwordEncoder,
                        username,
                        email,
                        ""
                );

        assertThrows(
                IllegalStateException.class,
                () -> invalidInitializer.run()
        );

        verifyNoInteractions(userRepository, roleRepository, passwordEncoder);
    }
}
