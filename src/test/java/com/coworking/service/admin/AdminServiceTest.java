package com.coworking.service.admin;

import com.coworking.admin.audit.enums.AuditAction;
import com.coworking.admin.audit.service.AuditLogService;
import com.coworking.admin.dto.*;
import com.coworking.admin.service.AdminServiceImpl;
import com.coworking.exception.BadRequestException;
import com.coworking.exception.NotFoundException;
import com.coworking.payment.repository.PaymentRepository;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.role.model.Role;
import com.coworking.role.repository.RoleRepository;
import com.coworking.room.repository.RoomRepository;
import com.coworking.user.model.User;
import com.coworking.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Mock
    private AuditLogService auditLogService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticatedUser(User user) {

        when(userRepository.findByEmail(user.getEmail()))
                .thenReturn(Optional.of(user));

        Authentication authentication =
                mock(Authentication.class);

        when(authentication.getName())
                .thenReturn(user.getEmail());

        SecurityContext securityContext =
                mock(SecurityContext.class);

        when(securityContext.getAuthentication())
                .thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
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
        user.setEmailVerified(true);
        user.setRoles(Set.of(role));
        user.setCreatedAt(creationTime);

        Page<User> page =
                new PageImpl<>(List.of(user));

        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(page);

        // Act
        AdminPageResponse<UserAdminResponse> response =
                adminService.getUsers(0, 10);

        // Assert
        assertEquals(1, response.content().size());

        UserAdminResponse result =
                response.content().getFirst();

        assertEquals("dayana", result.getUsername());

        assertEquals(
                "dayana@gmail.com",
                result.getEmail()
        );

        assertTrue(
                result.getRoles().contains("ROLE_USER")
        );

        assertTrue(result.isEnabled());

        assertTrue(result.isEmailVerified());

        assertEquals(
                creationTime,
                result.getCreatedAt()
        );

        verify(userRepository)
                .findAll(any(Pageable.class));
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
        savedUser.setEmailVerified(true);
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

        assertTrue(response.isEmailVerified());

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

        Role role = new Role();
        role.setName("ROLE_USER");

        User user = new User();
        user.setId(1L);
        user.setUsername("dayana");
        user.setEmail("dayana@test.com");
        user.setEnabled(false);
        user.setEmailVerified(true);
        user.setRoles(Set.of(role));

        UpdateUserStatusRequest request =
                new UpdateUserStatusRequest(true);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        User currentAdmin = new User();
        currentAdmin.setId(99L);
        currentAdmin.setEmail("admin@test.com");
        mockAuthenticatedUser(currentAdmin);

        UserAdminResponse response =
                adminService.updateUserStatus(1L, request);

        assertTrue(response.isEnabled());
        assertTrue(response.isEmailVerified());

        verify(userRepository).save(user);

        verify(auditLogService).log(
                AuditAction.USER_ACTIVATED,
                "User",
                1L
        );
    }

    @Test
    void shouldDisableUserSuccessfully() {

        Role role = new Role();
        role.setName("ROLE_USER");

        User user = new User();
        user.setId(1L);
        user.setUsername("dayana");
        user.setEmail("dayana@test.com");
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setRoles(Set.of(role));

        UpdateUserStatusRequest request =
                new UpdateUserStatusRequest(false);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        User currentAdmin = new User();
        currentAdmin.setId(99L);
        currentAdmin.setEmail("admin@test.com");
        mockAuthenticatedUser(currentAdmin);

        UserAdminResponse response =
                adminService.updateUserStatus(1L, request);

        assertFalse(response.isEnabled());
        assertTrue(response.isEmailVerified());

        verify(userRepository).save(user);

        verify(auditLogService).log(
                AuditAction.USER_DEACTIVATED,
                "User",
                1L
        );
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
        verify(auditLogService, never()).log(
                any(AuditAction.class),
                anyString(),
                anyLong()
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

        verify(auditLogService, never()).log(
                any(AuditAction.class),
                anyString(),
                anyLong()
        );

        assertEquals(
                "El estado del usuario debe ser true o false",
                exception.getMessage()
        );
    }

    //desactivar si es admin
    @Test
    void shouldThrowWhenAdminTriesToDisableOwnAccount() {

        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@test.com");
        admin.setEnabled(true);

        UpdateUserStatusRequest request =
                new UpdateUserStatusRequest(false);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));

        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(admin));

        Authentication authentication =
                mock(Authentication.class);

        when(authentication.getName())
                .thenReturn("admin@test.com");

        SecurityContext securityContext =
                mock(SecurityContext.class);

        when(securityContext.getAuthentication())
                .thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> adminService.updateUserStatus(
                                1L,
                                request
                        )
                );

        verify(auditLogService, never()).log(
                any(AuditAction.class),
                anyString(),
                anyLong()
        );

        assertEquals(
                "No puedes desactivar tu propia cuenta",
                exception.getMessage()
        );
    }

    @Test
    void shouldUpdateUserRoleSuccessfully() {

        Role userRole = new Role();
        userRole.setName("ROLE_USER");

        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");

        User user = new User();
        user.setId(1L);
        user.setUsername("dayana");
        user.setEmail("dayana@test.com");
        user.setRoles(new HashSet<>(Set.of(userRole)));

        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest("ROLE_ADMIN");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByName("ROLE_ADMIN"))
                .thenReturn(Optional.of(adminRole));

        User currentAdmin = new User();
        currentAdmin.setId(99L);
        currentAdmin.setEmail("admin@test.com");

        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(currentAdmin));

        Authentication authentication =
                mock(Authentication.class);

        when(authentication.getName())
                .thenReturn("admin@test.com");

        SecurityContext securityContext =
                mock(SecurityContext.class);

        when(securityContext.getAuthentication())
                .thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        UserAdminResponse response =
                adminService.updateUserRole(1L, request);

        assertTrue(
                response.getRoles().contains("ROLE_ADMIN")
        );

        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowWhenChangingRoleAndUserNotFound() {

        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest("ROLE_ADMIN");

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        NotFoundException exception =
                assertThrows(
                        NotFoundException.class,
                        () -> adminService.updateUserRole(
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
    void shouldThrowWhenRoleDoesNotExist() {

        Role userRole = new Role();
        userRole.setName("ROLE_USER");

        User user = new User();
        user.setId(1L);
        user.setRoles(Set.of(userRole));

        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest("ROLE_ADMIN");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByName("ROLE_ADMIN"))
                .thenReturn(Optional.empty());

        NotFoundException exception =
                assertThrows(
                        NotFoundException.class,
                        () -> adminService.updateUserRole(
                                1L,
                                request
                        )
                );

        assertEquals(
                "Rol no encontrado",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenUserAlreadyHasRole() {

        Role role = new Role();
        role.setName("ROLE_USER");

        User user = new User();
        user.setId(1L);
        user.setRoles(Set.of(role));

        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest("ROLE_USER");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByName("ROLE_USER"))
                .thenReturn(Optional.of(role));

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> adminService.updateUserRole(
                                1L,
                                request
                        )
                );

        assertEquals(
                "El usuario ya posee ese rol",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenRemovingLastAdmin() {

        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");

        Role userRole = new Role();
        userRole.setName("ROLE_USER");

        User user = new User();
        user.setId(1L);
        user.setRoles(Set.of(adminRole));

        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest("ROLE_USER");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByName("ROLE_USER"))
                .thenReturn(Optional.of(userRole));

        when(userRepository.countByRoles_Name("ROLE_ADMIN"))
                .thenReturn(1L);

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> adminService.updateUserRole(
                                1L,
                                request
                        )
                );

        assertEquals(
                "No se puede remover el último administrador",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenAdminRemovesOwnPrivileges() {

        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");

        Role userRole = new Role();
        userRole.setName("ROLE_USER");

        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@test.com");
        admin.setRoles(Set.of(adminRole));

        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest("ROLE_USER");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));

        when(roleRepository.findByName("ROLE_USER"))
                .thenReturn(Optional.of(userRole));

        when(userRepository.countByRoles_Name("ROLE_ADMIN"))
                .thenReturn(2L);

        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(admin));

        Authentication authentication =
                mock(Authentication.class);

        when(authentication.getName())
                .thenReturn("admin@test.com");

        SecurityContext securityContext =
                mock(SecurityContext.class);

        when(securityContext.getAuthentication())
                .thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> adminService.updateUserRole(
                                1L,
                                request
                        )
                );

        assertEquals(
                "No puedes remover tus propios privilegios administrativos",
                exception.getMessage()
        );
    }

    // editar usuario

    @Test
    void shouldUpdateUserSuccessfully() {

        Role currentRole = new Role();
        currentRole.setName("ROLE_USER");

        Role newRole = new Role();
        newRole.setName("ROLE_ADMIN");

        User user = new User();
        user.setId(1L);
        user.setUsername("dayana");
        user.setEmail("dayana@test.com");
        user.setRoles(new HashSet<>(Set.of(currentRole)));
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setCreatedAt(LocalDateTime.now());

        UpdateUserRequest request = new UpdateUserRequest(
                "dayanaUpdated",
                "dayana.updated@test.com",
                "ADMIN"
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByUsernameAndIdNot(
                "dayanaUpdated",
                1L
        )).thenReturn(false);

        when(userRepository.existsByEmailAndIdNot(
                "dayana.updated@test.com",
                1L
        )).thenReturn(false);

        when(roleRepository.findByName("ROLE_ADMIN"))
                .thenReturn(Optional.of(newRole));

        User currentAdmin = new User();
        currentAdmin.setId(99L);
        currentAdmin.setEmail("admin@test.com");

        mockAuthenticatedUser(currentAdmin);

        when(userRepository.save(user))
                .thenReturn(user);

        UserAdminResponse response =
                adminService.updateUser(1L, request);

        assertEquals(
                "dayanaUpdated",
                response.getUsername()
        );

        assertEquals(
                "dayana.updated@test.com",
                response.getEmail()
        );

        assertTrue(
                response.getRoles().contains("ROLE_ADMIN")
        );

        verify(userRepository).save(user);
    }

    @Test
    void shouldThrowWhenUpdatingUserWithDuplicateUsername() {

        Role role = new Role();
        role.setName("ROLE_USER");

        User user = new User();
        user.setId(1L);
        user.setUsername("dayana");
        user.setEmail("dayana@test.com");
        user.setRoles(new HashSet<>(Set.of(role)));

        UpdateUserRequest request = new UpdateUserRequest(
                "admin",
                "dayana@test.com",
                "USER"
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByUsernameAndIdNot(
                "admin",
                1L
        )).thenReturn(true);

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> adminService.updateUser(1L, request)
                );

        assertEquals(
                "El nombre de usuario ya existe",
                exception.getMessage()
        );

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void shouldThrowWhenUpdatingUserWithDuplicateEmail() {

        Role role = new Role();
        role.setName("ROLE_USER");

        User user = new User();
        user.setId(1L);
        user.setUsername("dayana");
        user.setEmail("dayana@test.com");
        user.setRoles(new HashSet<>(Set.of(role)));

        UpdateUserRequest request = new UpdateUserRequest(
                "dayanaUpdated",
                "admin@test.com",
                "USER"
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByUsernameAndIdNot(
                "dayanaUpdated",
                1L
        )).thenReturn(false);

        when(userRepository.existsByEmailAndIdNot(
                "admin@test.com",
                1L
        )).thenReturn(true);

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> adminService.updateUser(1L, request)
                );

        assertEquals(
                "El correo ya está registrado",
                exception.getMessage()
        );

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void shouldThrowWhenUpdatingUserAndUserNotFound() {

        UpdateUserRequest request = new UpdateUserRequest(
                "dayanaUpdated",
                "dayana.updated@test.com",
                "USER"
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        NotFoundException exception =
                assertThrows(
                        NotFoundException.class,
                        () -> adminService.updateUser(1L, request)
                );

        assertEquals(
                "Usuario no encontrado",
                exception.getMessage()
        );

        verify(userRepository, never())
                .existsByUsernameAndIdNot(any(), anyLong());

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void shouldThrowWhenUpdatingUserAndRoleDoesNotExist() {

        Role currentRole = new Role();
        currentRole.setName("ROLE_USER");

        User user = new User();
        user.setId(1L);
        user.setUsername("dayana");
        user.setEmail("dayana@test.com");
        user.setRoles(new HashSet<>(Set.of(currentRole)));

        UpdateUserRequest request = new UpdateUserRequest(
                "dayanaUpdated",
                "dayana.updated@test.com",
                "ADMIN"
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByUsernameAndIdNot(
                "dayanaUpdated",
                1L
        )).thenReturn(false);

        when(userRepository.existsByEmailAndIdNot(
                "dayana.updated@test.com",
                1L
        )).thenReturn(false);

        when(roleRepository.findByName("ROLE_ADMIN"))
                .thenReturn(Optional.empty());

        NotFoundException exception =
                assertThrows(
                        NotFoundException.class,
                        () -> adminService.updateUser(1L, request)
                );

        assertEquals(
                "Rol no encontrado",
                exception.getMessage()
        );

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void shouldThrowWhenUpdatingUserAndRemovingLastAdmin() {

        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");

        Role userRole = new Role();
        userRole.setName("ROLE_USER");

        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setEmail("admin@test.com");
        admin.setRoles(new HashSet<>(Set.of(adminRole)));

        UpdateUserRequest request = new UpdateUserRequest(
                "adminUpdated",
                "admin.updated@test.com",
                "USER"
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));

        when(userRepository.existsByUsernameAndIdNot(
                "adminUpdated",
                1L
        )).thenReturn(false);

        when(userRepository.existsByEmailAndIdNot(
                "admin.updated@test.com",
                1L
        )).thenReturn(false);

        when(roleRepository.findByName("ROLE_USER"))
                .thenReturn(Optional.of(userRole));

        when(userRepository.countByRoles_Name("ROLE_ADMIN"))
                .thenReturn(1L);

        User currentAdmin = new User();
        currentAdmin.setId(99L);
        currentAdmin.setEmail("other-admin@test.com");

        mockAuthenticatedUser(currentAdmin);

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> adminService.updateUser(1L, request)
                );

        assertEquals(
                "No se puede remover el último administrador",
                exception.getMessage()
        );

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void shouldThrowWhenAdminRemovesOwnPrivilegesWhileUpdatingUser() {

        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");

        Role userRole = new Role();
        userRole.setName("ROLE_USER");

        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setEmail("admin@test.com");
        admin.setRoles(new HashSet<>(Set.of(adminRole)));

        UpdateUserRequest request = new UpdateUserRequest(
                "adminUpdated",
                "admin.updated@test.com",
                "USER"
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(admin));

        when(userRepository.existsByUsernameAndIdNot(
                "adminUpdated",
                1L
        )).thenReturn(false);

        when(userRepository.existsByEmailAndIdNot(
                "admin.updated@test.com",
                1L
        )).thenReturn(false);

        when(roleRepository.findByName("ROLE_USER"))
                .thenReturn(Optional.of(userRole));

        when(userRepository.countByRoles_Name("ROLE_ADMIN"))
                .thenReturn(2L);

        mockAuthenticatedUser(admin);

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> adminService.updateUser(1L, request)
                );

        assertEquals(
                "No puedes remover tus propios privilegios administrativos",
                exception.getMessage()
        );

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void shouldAllowUpdatingUserWithSameUsernameAndEmail() {

        Role role = new Role();
        role.setName("ROLE_USER");

        User user = new User();
        user.setId(1L);
        user.setUsername("dayana");
        user.setEmail("dayana@test.com");
        user.setRoles(new HashSet<>(Set.of(role)));
        user.setEnabled(true);
        user.setEmailVerified(true);

        UpdateUserRequest request = new UpdateUserRequest(
                "dayana",
                "dayana@test.com",
                "USER"
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByUsernameAndIdNot(
                "dayana",
                1L
        )).thenReturn(false);

        when(userRepository.existsByEmailAndIdNot(
                "dayana@test.com",
                1L
        )).thenReturn(false);

        when(roleRepository.findByName("ROLE_USER"))
                .thenReturn(Optional.of(role));

        User currentAdmin = new User();
        currentAdmin.setId(99L);
        currentAdmin.setEmail("admin@test.com");

        mockAuthenticatedUser(currentAdmin);

        when(userRepository.save(user))
                .thenReturn(user);

        UserAdminResponse response =
                adminService.updateUser(1L, request);

        assertEquals(
                "dayana",
                response.getUsername()
        );

        assertEquals(
                "dayana@test.com",
                response.getEmail()
        );

        assertTrue(
                response.getRoles().contains("ROLE_USER")
        );

        verify(userRepository).save(user);
    }
}
