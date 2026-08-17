package com.coworking.admin.report.service;

import com.coworking.admin.report.dto.FinancialReportResponse;
import com.coworking.admin.report.dto.RoomUsageReportResponse;
import com.coworking.admin.report.dto.reservation.ReservationReportItem;
import com.coworking.admin.report.dto.reservation.ReservationReportMetricResult;
import com.coworking.admin.report.dto.reservation.ReservationReportRequest;
import com.coworking.admin.report.dto.reservation.ReservationReportResponse;
import com.coworking.admin.report.enums.reservation.ReservationReportMetric;
import com.coworking.admin.report.generator.reservation.ReservationReportPdfGenerator;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {
    private final ReservationRepository reservationRepository;
    private final ReservationReportPdfGenerator reservationReportPdfGenerator;

    private static final ZoneId ZONE_ID = ZoneId.of("America/El_Salvador");


    @Override
    public ReservationReportResponse getReservationReport(ReservationReportRequest request) {

        LocalDateTime startDateTime = request.startDate().atStartOfDay();
        LocalDateTime endDateTime = request.endDate().plusDays(1).atStartOfDay();

        Instant startInstant = startDateTime.atZone(ZONE_ID).toInstant();
        Instant endInstant = endDateTime.atZone(ZONE_ID).toInstant();

        //Buscar reservas
        List<Reservation> reservations = reservationRepository
                .findReservationsOverlappingPeriod(
                        startInstant,
                        endInstant
                );
        // Mapear reservas
        List<ReservationReportItem> reportItems = reservations.stream()
                .map(this::mapToReportItem)
                .toList();

        // calcular metricas

        List<ReservationReportMetricResult> metrics = calculateMetrics(
                reservations,
                request.metrics()
        );

        return new ReservationReportResponse(
                request.startDate(),
                request.endDate(),
                metrics,
                reportItems
        );

    }

    @Override
    public byte[] generateReservationReportPdf(ReservationReportRequest request) {
        ReservationReportResponse report = getReservationReport(request);
        return reservationReportPdfGenerator.generatePdf(report);
    }

    private List<ReservationReportMetricResult> calculateMetrics(List<Reservation> reservations,
                                                                 List<ReservationReportMetric> selectedMetrics) {
        return selectedMetrics.stream()
                .map(metric -> switch (metric) {

                    case TOTAL_RESERVAS -> new ReservationReportMetricResult(
                            metric,
                            reservations.size()
                    );

                    case HORAS_RESERVADAS -> {

                        long totalMinutes = reservations.stream()
                                .mapToLong(reservation ->
                                        Duration.between(
                                                reservation.getStartAt(),
                                                reservation.getEndAt()
                                        ).toMinutes()
                                )
                                .sum();

                        double totalHours = totalMinutes / 60.0;

                        yield new ReservationReportMetricResult(
                                metric,
                                totalHours
                        );
                    }

                    case TOTAL_REVENUE -> {

                        BigDecimal totalRevenue = reservations.stream()
                                .filter(reservation ->
                                        reservation.getStatus()
                                                == ReservationStatus.PAID
                                )
                                .map(Reservation::getPrice)
                                .reduce(
                                        BigDecimal.ZERO,
                                        BigDecimal::add
                                );

                        yield new ReservationReportMetricResult(
                                metric,
                                totalRevenue
                        );
                    }

                    case PAID -> {

                        long paid = reservations.stream()
                                .filter(reservation ->
                                        reservation.getStatus()
                                                == ReservationStatus.PAID
                                )
                                .count();

                        yield new ReservationReportMetricResult(
                                metric,
                                paid
                        );
                    }
                })
                .toList();
    }

    private ReservationReportItem mapToReportItem(Reservation reservation) {
        return new ReservationReportItem(
                reservation.getId(),
                reservation.getUser().getUsername(),
                reservation.getRoom().getName(),
                reservation.getStartAt(),
                reservation.getEndAt(),
                reservation.getPrice(),
                reservation.getStatus()
        );
    }

    @Override
    public FinancialReportResponse getFinancialReport() {
        return null;
    }

    @Override
    public RoomUsageReportResponse getRoomUsageReport() {
        return null;
    }
}
