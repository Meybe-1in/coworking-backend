package com.coworking.admin.service;

import com.coworking.admin.audit.enums.AuditAction;
import com.coworking.admin.audit.service.AuditLogService;
import com.coworking.admin.dto.*;
import com.coworking.exception.BadRequestException;
import com.coworking.exception.NotFoundException;
import com.coworking.payment.dto.PaymentResponse;
import com.coworking.payment.model.Payment;
import com.coworking.payment.repository.PaymentRepository;
import com.coworking.reservation.dto.ReservationResponse;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.role.model.Role;
import com.coworking.role.repository.RoleRepository;
import com.coworking.room.repository.RoomRepository;
import com.coworking.user.model.User;
import com.coworking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoomRepository roomRepository;
    private final AuditLogService auditLogService;
    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._-])[A-Za-z\\d@$!%*?&._-]{8,}$";

    // Obtiene todas las reservas y las transforma a DTO de respuesta
    @Override
    public AdminPageResponse<ReservationResponse> getReservations(int page, int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<ReservationResponse> reservations =
                reservationRepository
                        .findAll(pageable)
                        .map(this::mapReservationToResponse);

        return AdminPageResponse.<ReservationResponse>builder()
                .content(reservations.getContent())
                .page(reservations.getNumber())
                .size(reservations.getSize())
                .totalElements(reservations.getTotalElements())
                .totalPages(reservations.getTotalPages())
                .first(reservations.isFirst())
                .last(reservations.isLast())
                .build();
    }

    // Obtiene todos los pagos y los transforma a DTO de respuesta
    @Override
    public AdminPageResponse<PaymentResponse> getPayments(int page, int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "paidAt")
        );

        Page<PaymentResponse> payments =
                paymentRepository
                        .findAll(pageable)
                        .map(this::mapPaymentToResponse);

        return AdminPageResponse.<PaymentResponse>builder()
                .content(payments.getContent())
                .page(payments.getNumber())
                .size(payments.getSize())
                .totalElements(payments.getTotalElements())
                .totalPages(payments.getTotalPages())
                .first(payments.isFirst())
                .last(payments.isLast())
                .build();
    }

    // Cancela una reserva independientemente de su propietario
    @Override
    @Transactional
    public void cancelReservation(Long reservationId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() ->
                        new NotFoundException("Reserva no encontrada")
                );
        reservation.setStatus(ReservationStatus.CANCELLED);

        reservationRepository.save(reservation);

        auditLogService.log(
                AuditAction.RESERVATION_CANCELLED,
                "Reservation",
                reservation.getId()
        );

    }

    // Crear usuario admin
    @Override
    @Transactional
    public UserAdminResponse createAdmin(CreateAdminRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException(
                    "El nombre de usuario ya existe"
            );
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException(
                    "El correo ya está registrado"
            );
        }

        if (!request.getPassword().matches(PASSWORD_REGEX)) {
            throw new BadRequestException(
                    "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial"
            );
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() ->
                        new NotFoundException("Rol ADMIN no encontrado")
                );

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );
        user.setRoles(Set.of(adminRole));
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        return mapToUserAdminResponse(savedUser);
    }

    // Actualiza el estado principal de un usuario
    @Override
    public UserAdminResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new NotFoundException("Usuario no encontrado")
                );

        if (request.getEnabled() == null) {
            throw new BadRequestException("El estado del usuario debe ser true o false");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() ->
                        new NotFoundException("Usuario autenticado no encontrado")
                );

        if (user.isEnabled() == request.getEnabled()) {
            throw new BadRequestException("El usuario ya tiene ese estado");
        }

        if (currentUser.getId().equals(user.getId()) && !request.getEnabled()) {
            throw new BadRequestException(
                    "No puedes desactivar tu propia cuenta"
            );
        }

        user.setEnabled(request.getEnabled());
        userRepository.save(user);

        auditLogService.log(
                request.getEnabled()
                        ? AuditAction.USER_ACTIVATED
                        : AuditAction.USER_DEACTIVATED,
                "User",
                user.getId()
        );


        return mapToUserAdminResponse(user);
    }

    @Override
    public UserAdminResponse updateUserRole(Long userId, UpdateUserRoleRequest request) {
        // Buscar usuario
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new NotFoundException("Usuario no encontrado")
                );

        //validar rol
        Role newRole = roleRepository.findByName(request.role())
                .orElseThrow(() ->
                        new NotFoundException("Rol no encontrado")
                );

        //obtener rol actual
        Role currentRole = user.getRoles()
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new BadRequestException("Usuario sin rol asignado")
                );

        //validar que el rol no sea el mismo
        if (currentRole.getName().equals(newRole.getName())) {
            throw new BadRequestException("El usuario ya posee ese rol");
        }

        //Proteger ultimo admin
        if (currentRole.getName().equals("ROLE_ADMIN") && newRole.getName().equals("ROLE_USER")) {
            long admins = userRepository.countByRoles_Name("ROLE_ADMIN");
            if (admins == 1) {
                throw new BadRequestException("No se puede remover el último administrador");
            }
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String currentEmail = authentication.getName();

        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Usuario autenticado no encontrado"
                        )
                );

        if (
                currentUser.getId().equals(user.getId())
                        && currentRole.getName().equals("ROLE_ADMIN")
                        && newRole.getName().equals("ROLE_USER")
        ) {
            throw new BadRequestException(
                    "No puedes remover tus propios privilegios administrativos"
            );
        }

        // El sistema permite un único rol por usuario.
        user.getRoles().clear();
        user.getRoles().add(newRole);
        userRepository.save(user);
        return mapToUserAdminResponse(user);

    }

    @Override
    @Transactional
    public UserAdminResponse updateUser(Long userId, UpdateUserRequest request) {
        //buscar usuarios
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new NotFoundException("Usuario no encontrado")
                );
        //validar username duplicado
        if (userRepository.existsByUsernameAndIdNot(request.getUsername(), userId)) {
            throw new BadRequestException("El nombre de usuario ya existe");
        }
        //validar email duplicado
        if (userRepository.existsByEmailAndIdNot(request.getEmail(), userId)) {
            throw new BadRequestException("El correo ya está registrado");
        }
        //buscar nuevo rol
        String roleName = "ROLE_" + request.getRole();
        Role newRole = roleRepository.findByName(roleName)
                .orElseThrow(() ->
                        new NotFoundException("Rol no encontrado")
                );
        //obtener rol actual
        Role currentRole = user.getRoles()
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new BadRequestException(
                                "Usuario sin rol asignado"
                        )
                );

        //obtener administrador autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() ->
                        new NotFoundException("Usuario autenticado no encontrado")
                );
        // Proteger al último administrador
        if (
                currentRole.getName().equals("ROLE_ADMIN")
                        && newRole.getName().equals("ROLE_USER")
        ) {

            long admins =
                    userRepository.countByRoles_Name("ROLE_ADMIN");

            if (admins == 1) {
                throw new BadRequestException(
                        "No se puede remover el último administrador"
                );
            }
        }

        // Un administrador no puede quitarse sus propios privilegios
        if (
                currentUser.getId().equals(user.getId())
                        && currentRole.getName().equals("ROLE_ADMIN")
                        && newRole.getName().equals("ROLE_USER")
        ) {
            throw new BadRequestException(
                    "No puedes remover tus propios privilegios administrativos"
            );
        }

        // Actualizar username
        user.setUsername(request.getUsername());

        // Actualizar email
        user.setEmail(request.getEmail());

        // Actualizar rol
        user.getRoles().clear();
        user.getRoles().add(newRole);

        User updatedUser = userRepository.save(user);

        return mapToUserAdminResponse(updatedUser);
    }

    //Usuario para perfil autenticado
    @Override
    public AdminProfileResponse getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new NotFoundException("Usuario autenticado no encontrado")
                );

        return new AdminProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles()
                        .stream()
                        .map(r -> r.getName())
                        .collect(Collectors.toSet())
        );
    }

    // Obtiene todos los usuarios registrados para la vista administrativa
    @Override
    public AdminPageResponse<UserAdminResponse> getUsers(int page, int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<UserAdminResponse> users =
                userRepository
                        .findAll(pageable)
                        .map(this::mapToUserAdminResponse);

        return AdminPageResponse.<UserAdminResponse>builder()
                .content(users.getContent())
                .page(users.getNumber())
                .size(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .first(users.isFirst())
                .last(users.isLast())
                .build();
    }

    // MAPPERS

    // Convierte una entidad Reservation a ReservationResponse
    private ReservationResponse mapReservationToResponse(Reservation reservation) {

        ReservationResponse response = new ReservationResponse();

        response.setId(reservation.getId());
        response.setRoomName(reservation.getRoom().getName());
        response.setUsername(reservation.getUser().getUsername());
        response.setStartAt(reservation.getStartAt());
        response.setEndAt(reservation.getEndAt());
        response.setPrice(reservation.getPrice());
        response.setCreatedAt(reservation.getCreatedAt());
        response.setStatus(reservation.getStatus());

        return response;
    }

    // Convierte una entidad Payment a PaymentResponse
    private PaymentResponse mapPaymentToResponse(Payment payment) {

        return PaymentResponse.builder()
                .id(payment.getId())
                .reservationId(payment.getReservation().getId())
                .roomName(payment.getReservation().getRoom().getName())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .build();
    }

    // Convierte una entidad User a UserAdminResponse
    private UserAdminResponse mapToUserAdminResponse(User user) {
        Set<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return new UserAdminResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roles,
                user.isEnabled(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }
}