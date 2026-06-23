package com.coworking.admin.service;

import com.coworking.admin.dto.AdminStatsResponse;
import com.coworking.admin.dto.UserAdminResponse;
import com.coworking.exception.NotFoundException;
import com.coworking.payment.dto.PaymentResponse;
import com.coworking.payment.model.Payment;
import com.coworking.payment.repository.PaymentRepository;
import com.coworking.reservation.dto.ReservationResponse;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.role.model.Role;
import com.coworking.user.model.User;
import com.coworking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @Override
    public AdminStatsResponse getStats() {

        long totalReservations = reservationRepository.count();

        long activeReservations =
                reservationRepository.countByStatus(ReservationStatus.PAID);

        long pendingReservations =
                reservationRepository.countByStatus(ReservationStatus.PENDING);

        long cancelledReservations =
                reservationRepository.countByStatus(ReservationStatus.CANCELLED);

        long expiredReservations =
                reservationRepository.countByStatus(ReservationStatus.EXPIRED);

        // Construye las métricas mostradas en el dashboard administrativo
        return AdminStatsResponse.builder()
                .totalReservations(totalReservations)
                .activeReservations(activeReservations)
                .pendingReservations(pendingReservations)
                .cancelledReservations(cancelledReservations)
                .expiredReservations(expiredReservations)
                .totalRevenue(paymentRepository.getTotalRevenue())
                .monthlyRevenue(paymentRepository.getMonthlyRevenue())
                .build();
    }

    // Obtiene todas las reservas y las transforma a DTO de respuesta
    @Override
    public List<ReservationResponse> getAllReservations() {

        return reservationRepository.findAll()
                .stream()
                .map(this::mapReservationToResponse)
                .toList();
    }

    // Obtiene todos los pagos y los transforma a DTO de respuesta
    @Override
    public List<PaymentResponse> getAllPayments() {

        return paymentRepository.findAll()
                .stream()
                .map(this::mapPaymentToResponse)
                .toList();
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

    }

    // Obtiene todos los usuarios registrados para la vista administrativa
    public List<UserAdminResponse> getAllUsers(){
        return userRepository.findAll()
                .stream()
                .map(this::mapToUserAdminResponse)
                .toList();
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
    private UserAdminResponse mapToUserAdminResponse(User user){
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
                user.getCreatedAt()
        );
    }
}