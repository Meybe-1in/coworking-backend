package com.coworking.resources.service.admin;

import com.coworking.admin.dto.AdminStatsResponse;
import com.coworking.admin.dto.UserAdminResponse;
import com.coworking.admin.service.AdminServiceImpl;
import com.coworking.payment.repository.PaymentRepository;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.role.model.Role;
import com.coworking.user.model.User;
import com.coworking.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        User user = new User();
        user.setId(1L);
        user.setUsername("dayana");
        user.setEmail("dayana@gmail.com");
        user.setEnabled(true);
        user.setRoles(Set.of(role));

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

        verify(userRepository).findAll();
    }
}
