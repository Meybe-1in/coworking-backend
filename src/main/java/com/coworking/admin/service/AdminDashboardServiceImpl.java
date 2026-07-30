package com.coworking.admin.service;

import com.coworking.admin.dto.AdminStatsResponse;
import com.coworking.admin.dto.ChartPointResponse;
import com.coworking.admin.enums.ChartPeriod;
import com.coworking.admin.util.ChartDateUtils;
import com.coworking.payment.repository.PaymentRepository;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.room.repository.RoomRepository;
import com.coworking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

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

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByEnabledTrue();
        long disabledUsers = userRepository.countByEnabledFalse();

        long totalRooms = roomRepository.count();
        long availableRooms = roomRepository.countByAvailableTrue();
        long unavailableRooms = roomRepository.countByAvailableFalse();

        Instant endToday = ChartDateUtils.getEndDate();
        Instant startToday =
                LocalDate.now()
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant();

        Instant startMonth =
                ChartDateUtils.getStartDate(ChartPeriod.MONTH);

        long todayReservations = reservationRepository.countByCreatedAtBetween(startToday, endToday);
        long monthReservations = reservationRepository.countByCreatedAtBetween(startMonth, endToday);

        // Construye las métricas mostradas en el dashboard administrativo
        return AdminStatsResponse.builder()
                .totalReservations(totalReservations)
                .activeReservations(activeReservations)
                .pendingReservations(pendingReservations)
                .cancelledReservations(cancelledReservations)
                .expiredReservations(expiredReservations)

                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .disabledUsers(disabledUsers)

                .totalRooms(totalRooms)
                .availableRooms(availableRooms)
                .unavailableRooms(unavailableRooms)

                .todayReservations(todayReservations)
                .monthReservations(monthReservations)

                .totalRevenue(paymentRepository.getTotalRevenue())
                .monthlyRevenue(paymentRepository.getMonthlyRevenue())
                .build();
    }

    @Override
    public List<ChartPointResponse> getReservationsChart(
            ChartPeriod period
    ) {
        Instant startDate = ChartDateUtils.getStartDate(period);

        Instant endDate = ChartDateUtils.getEndDate();

        List<Reservation> reservations = reservationRepository
                .findByCreatedAtBetweenOrderByCreatedAtAsc(startDate, endDate);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Map<String, Long> groupedReservations =
                reservations.stream().collect(
                        Collectors.groupingBy(reservation -> reservation.getCreatedAt()
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                        .format(formatter),
                                Collectors.counting()
                        )
                );

        return groupedReservations.entrySet()
                .stream()
                .map(entry -> ChartPointResponse
                        .builder()
                        .label(entry.getKey())
                        .value(entry.getValue())
                        .build()
                ).toList();
    }
}
