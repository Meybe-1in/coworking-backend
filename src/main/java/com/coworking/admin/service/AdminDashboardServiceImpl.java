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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
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
    public List<ChartPointResponse> getReservationsChart(ChartPeriod period) {
        /*
         * La gráfica reutiliza un único endpoint para todos los períodos.
         *
         * WEEK  -> agrupación diaria.
         * MONTH -> agrupación diaria.
         * YEAR  -> agrupación mensual.
         *
         * Para la vista anual se utiliza una consulta GROUP BY en la base
         * de datos para evitar cargar todas las reservas en memoria.
         */

        return switch (period) {
            case WEEK, MONTH -> groupByDay(getReservations(period));
            case YEAR -> groupByMonth();
        };

    }

    @Override
    public List<ChartPointResponse> getRevenueChart(ChartPeriod period) {
        return switch (period) {
            case WEEK, MONTH -> groupRevenueByDay(period);
            case YEAR -> groupRevenueByMonth();
        };
    }

    private List<ChartPointResponse> groupByDay(List<Reservation> reservations) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        Map<String, Long> grouped = reservations
                .stream()
                .collect(Collectors.groupingBy(reservation -> reservation
                                        .getCreatedAt()
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                        .format(formatter),
                                TreeMap::new,
                                Collectors.counting()
                        )
                );

        return grouped.entrySet()
                .stream()
                .map(entry -> ChartPointResponse
                        .builder()
                        .period(entry.getKey())
                        .total(BigDecimal.valueOf(entry.getValue()))
                        .build()
                )
                .toList();
    }

    /*
     * Completa los doce meses del año para que el frontend
     * siempre reciba una serie continua, incluso cuando
     * existan meses sin reservas.
     */

    private List<ChartPointResponse> groupByMonth() {

        List<Object[]> query = reservationRepository.countReservationsByMonthCurrentYear();
        Map<Integer, Long> totals = new HashMap<>();

        query.forEach(row -> totals.put(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).longValue()
                )
        );

        List<ChartPointResponse> result = new ArrayList<>();

        Locale locale = Locale.forLanguageTag("es");

        for (int month = 1; month <= 12; month++) {

            String label = Month.of(month)
                    .getDisplayName(
                            TextStyle.FULL,
                            locale
                    );

            label = Character.toUpperCase(label.charAt(0)) + label.substring(1);

            result.add(ChartPointResponse
                    .builder()
                    .period(label)
                    .total(BigDecimal.valueOf(totals
                            .getOrDefault(month, 0L))
                    )

                    .build()
            );

        }
        return result;
    }

    private List<Reservation> getReservations(
            ChartPeriod period
    ) {

        Instant start =
                ChartDateUtils.getStartDate(period);

        Instant end =
                ChartDateUtils.getEndDate();

        return reservationRepository
                .findByCreatedAtBetweenOrderByCreatedAtAsc(
                        start,
                        end
                );
    }

    /*
     * Obtiene los ingresos del período solicitado y los agrupa por día.
     * Se utiliza para las vistas WEEK y MONTH.
     */
    private List<ChartPointResponse> groupRevenueByDay(ChartPeriod period) {
        Instant start = ChartDateUtils.getStartDate(period);
        Instant end = ChartDateUtils.getEndDate();

        List<Object[]> query = paymentRepository.getRevenueGroupedByDay(start, end);
        Map<String, BigDecimal> totals = new HashMap<>();

        query.forEach(row -> totals.put(
                row[0].toString(),
                new BigDecimal(row[1].toString())
        ));

        return fillMissingDays(start, end, totals);
    }

    private List<ChartPointResponse> fillMissingDays(Instant start, Instant end, Map<String, BigDecimal> totals) {

        List<ChartPointResponse> result = new ArrayList<>();

        LocalDate startDate = start.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endDate = end.atZone(ZoneId.systemDefault()).toLocalDate();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        while (!startDate.isAfter(endDate)) {

            String period = startDate.format(formatter);

            result.add(
                    ChartPointResponse.builder()
                            .period(period)
                            .total(totals.getOrDefault(
                                    period,
                                    BigDecimal.ZERO))
                            .build()
            );
            startDate = startDate.plusDays(1);
        }
        return result;
    }

    private List<ChartPointResponse> groupRevenueByMonth() {

        List<Object[]> query = paymentRepository.getRevenueGroupedByMonthCurrentYear();

        Map<Integer, BigDecimal> totals = new HashMap<>();

        query.forEach(row -> totals.put(
                ((Number) row[0]).intValue(),
                new BigDecimal(row[1].toString())
        ));

        List<ChartPointResponse> result = new ArrayList<>();

        Locale locale = Locale.forLanguageTag("es");

        for (int month = 1; month <= 12; month++) {

            String label = Month.of(month)
                    .getDisplayName(TextStyle.FULL, locale);

            label = Character.toUpperCase(label.charAt(0))
                    + label.substring(1);

            result.add(
                    ChartPointResponse.builder()
                            .period(label)
                            .total(
                                    totals.getOrDefault(month, BigDecimal.ZERO)
                            )
                            .build()
            );
        }
        return result;
    }
}
