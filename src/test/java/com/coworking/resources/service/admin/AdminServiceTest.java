package com.coworking.resources.service.admin;

import com.coworking.admin.dto.AdminStatsResponse;
import com.coworking.admin.dto.CreateAdminRequest;
import com.coworking.admin.dto.UpdateUserStatusRequest;
import com.coworking.admin.dto.UserAdminResponse;
import com.coworking.admin.service.AdminServiceImpl;
import com.coworking.exception.BadRequestException;
import com.coworking.exception.NotFoundException;
import com.coworking.payment.repository.PaymentRepository;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.role.model.Role;
import com.coworking.role.repository.RoleRepository;
import com.coworking.user.model.User;
import com.coworking.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void shouldReturnAdminStats() {

        when(reservationRepository.count()).thenReturn(10L);

        when(reservationRepository.countByStatus(ReservationStatus.PAID))
                .thenReturn(5L);

        when(reservationRepository.countByStatus(ReservationStatus.PENDING))
                .thenReturn(2L);

        when(reservationRepository.countByStatus(ReservationStatus.CANCELLED))
                .thenReturn(1L);

        when(reservationRepository.countByStatus(ReservationStatus.EXPIRED))
                .thenReturn(2L);

        when(paymentRepository.getTotalRevenue())
                .thenReturn(BigDecimal.valueOf(1000));

        when(paymentRepository.getMonthlyRevenue())
                .thenReturn(BigDecimal.valueOf(300));

        AdminStatsResponse response = adminService.getStats();

        assertEquals(10L, response.totalReservations());
        assertEquals(5L, response.activeReservations());
        assertEquals(BigDecimal.valueOf(1000), response.totalRevenue());
    }

    // Cancel reservation

    @Test
    void shouldCancelReservation() {
        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setStatus(ReservationStatus.PAID);

        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(reservation));

        adminService.cancelReservation(1L);

        assertEquals(
                ReservationStatus.CANCELLED,
                reservation.getStatus()
        );

        verify(reservationRepository).save(reservation);
    }

    //lista de usuarios
    @Test
    void shouldReturnAllUsers() {

        // Arrange
        Role role = new Role();
        role.setName("ROLE_USER");

        LocalDateTime creationTime = LocalDateTime.now();

        User user = new User();
        user.setId(1L);
        user.setUsername("dayana");
        user.setEmail("dayana@gmail.com");
        user.setEnabled(true);
        user.setRoles(Set.of(role));
        user.setCreatedAt(creationTime);

        when(userRepository.findAll())
                .thenReturn(List.of(user));

        // Act
        List<UserAdminResponse> result =
                adminService.getAllUsers();

        // Assert
        assertEquals(1, result.size());

        assertEquals(
                "dayana",
                result.getFirst().getUsername()
        );

        assertEquals(
                "dayana@gmail.com",
                result.getFirst().getEmail()
        );

        assertTrue(
                result.getFirst()
                        .getRoles()
                        .contains("ROLE_USER")
        );

        assertTrue(
                result.getFirst()
                        .isEnabled()
        );

        assertEquals(
                creationTime, result.getFirst()
                        .getCreatedAt()
        );

        verify(userRepository).findAll();
    }

    //create admin
    @Test
    void shouldCreateAdminSuccessfully() {

        // Verifica que un administrador pueda ser creado correctamente
        // cuando username y email no existen previamente.

        CreateAdminRequest request = new CreateAdminRequest();

        request.setUsername("admin");
        request.setEmail("admin@test.com");
        request.setPassword("Password123.");

        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");

        when(userRepository.existsByUsername("admin"))
                .thenReturn(false);

        when(userRepository.existsByEmail("admin@test.com"))
                .thenReturn(false);

        when(roleRepository.findByName("ROLE_ADMIN"))
                .thenReturn(Optional.of(adminRole));

        when(passwordEncoder.encode("Password123."))
                .thenReturn("encodedPassword");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("admin");
        savedUser.setEmail("admin@test.com");
        savedUser.setEnabled(true);
        savedUser.setRoles(Set.of(adminRole));
        savedUser.setCreatedAt(LocalDateTime.now());

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        UserAdminResponse response =
                adminService.createAdmin(request);

        assertEquals("admin", response.getUsername());
        assertEquals("admin@test.com", response.getEmail());

        assertTrue(
                response.getRoles()
                        .contains("ROLE_ADMIN")
        );

        verify(userRepository).save(any(User.class));
    }

    //usuarios mismo username
    @Test
    void shouldThrowExceptionWhenUsernameAlreadyExists() {

        // Verifica que el sistema rechace la creación
        // cuando el nombre de usuario ya existe.

        CreateAdminRequest request = new CreateAdminRequest();

        request.setUsername("admin");
        request.setEmail("nuevo@test.com");
        request.setPassword("Password123.");

        when(userRepository.existsByUsername("admin"))
                .thenReturn(true);

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> adminService.createAdmin(request)
                );

        assertEquals(
                "El nombre de usuario ya existe",
                exception.getMessage()
        );

        verify(userRepository).existsByUsername("admin");
    }

    //email duplicado
    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {

        // Verifica que el sistema rechace la creación
        // cuando el correo ya está registrado.

        CreateAdminRequest request = new CreateAdminRequest();

        request.setUsername("admin");
        request.setEmail("admin@test.com");
        request.setPassword("Password123.");

        when(userRepository.existsByUsername("admin"))
                .thenReturn(false);

        when(userRepository.existsByEmail("admin@test.com"))
                .thenReturn(true);

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> adminService.createAdmin(request)
                );

        assertEquals(
                "El correo ya está registrado",
                exception.getMessage()
        );

        verify(userRepository).existsByEmail("admin@test.com");
    }

    //rol inexistente
    @Test
    void shouldThrowExceptionWhenAdminRoleDoesNotExist() {

        // Verifica que se lance una excepción
        // si el rol ROLE_ADMIN no existe.

        CreateAdminRequest request = new CreateAdminRequest();

        request.setUsername("admin");
        request.setEmail("admin@test.com");
        request.setPassword("Password123.");

        when(userRepository.existsByUsername("admin"))
                .thenReturn(false);

        when(userRepository.existsByEmail("admin@test.com"))
                .thenReturn(false);

        when(roleRepository.findByName("ROLE_ADMIN"))
                .thenReturn(Optional.empty());

        NotFoundException exception =
                assertThrows(
                        NotFoundException.class,
                        () -> adminService.createAdmin(request)
                );

        assertEquals(
                "Rol ADMIN no encontrado",
                exception.getMessage()
        );
    }

    @Test
    void shouldEnableUserSuccessfully() {

        User user = new User();
        user.setId(1L);
        user.setUsername("dayana");
        user.setEmail("dayana@test.com");
        user.setEnabled(false);

        UpdateUserStatusRequest request =
                new UpdateUserStatusRequest(true);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        UserAdminResponse response =
                adminService.updateUserStatus(1L, request);

        assertTrue(response.isEnabled());

        verify(userRepository).save(user);
    }

    @Test
    void shouldDisableUserSuccessfully() {

        User user = new User();
        user.setId(1L);
        user.setUsername("dayana");
        user.setEmail("dayana@test.com");
        user.setEnabled(true);

        UpdateUserStatusRequest request =
                new UpdateUserStatusRequest(false);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        UserAdminResponse response =
                adminService.updateUserStatus(1L, request);

        assertFalse(response.isEnabled());

        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowWhenUserNotFound() {

        UpdateUserStatusRequest request =
                new UpdateUserStatusRequest(false);

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        NotFoundException exception =
                assertThrows(
                        NotFoundException.class,
                        () -> adminService.updateUserStatus(
                                1L,
                                request
                        )
                );

        assertEquals(
                "Usuario no encontrado",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenStatusIsNull() {

        User user = new User();
        user.setId(1L);

        UpdateUserStatusRequest request =
                new UpdateUserStatusRequest(null);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> adminService.updateUserStatus(
                                1L,
                                request
                        )
                );

        assertEquals(
                "El estado del usuario debe ser true o false",
                exception.getMessage()
        );
    }
}
