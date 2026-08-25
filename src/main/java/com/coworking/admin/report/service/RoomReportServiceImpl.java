package com.coworking.admin.report.service;

import com.coworking.admin.report.dto.financial.FinancialReportResponse;
import com.coworking.admin.report.dto.room.RoomUsageReportItem;
import com.coworking.admin.report.dto.room.RoomUsageReportMetricResult;
import com.coworking.admin.report.dto.room.RoomUsageReportRequest;
import com.coworking.admin.report.dto.room.RoomUsageReportResponse;
import com.coworking.admin.report.enums.room.RoomUsageReportMetric;
import com.coworking.admin.report.generator.room.RoomUsageReportCsvGenerator;
import com.coworking.admin.report.generator.room.RoomUsageReportPdfGenerator;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.room.model.Room;
import com.coworking.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomReportServiceImpl implements RoomReportService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    private final RoomUsageReportPdfGenerator roomUsageReportPdfGenerator;
    private final RoomUsageReportCsvGenerator roomUsageReportCsvGenerator;

    private static final ZoneId ZONE_ID = ZoneId.of("America/El_Salvador");
    private static final double AVAILABLE_HOURS_PER_DAY = 13.0;

    @Override
    public RoomUsageReportResponse getRoomUsageReport(
            RoomUsageReportRequest request
    ) {

        LocalDate startDate = request.startDate();
        LocalDate endDate = request.endDate();

        Instant startDateTime = startDate
                .atStartOfDay(ZONE_ID)
                .toInstant();

        Instant endDateTime = endDate
                .plusDays(1)
                .atStartOfDay(ZONE_ID)
                .toInstant();

        List<Reservation> reservations =
                reservationRepository.findPaidReservationsForRoomUsageReport(
                        ReservationStatus.PAID,
                        startDateTime,
                        endDateTime
                );

        List<Room> rooms = roomRepository.findAll();

        Map<Long, List<Reservation>> reservationsByRoom =
                reservations.stream()
                        .collect(Collectors.groupingBy(
                                reservation -> reservation.getRoom().getId()
                        ));

        List<RoomUsageReportItem> roomItems = new ArrayList<>();

        long totalReservations = 0;
        double totalReservedHours = 0;
        double totalAvailableHours = 0;

        double availableHoursPerRoom =
                calculateAvailableHours(startDate, endDate);

        for (Room room : rooms) {

            List<Reservation> roomReservations =
                    reservationsByRoom.getOrDefault(
                            room.getId(),
                            List.of()
                    );

            double reservedHours = roomReservations.stream()
                    .mapToDouble(reservation ->
                            calculateEffectiveHours(
                                    reservation,
                                    startDateTime,
                                    endDateTime
                            )
                    )
                    .sum();

            long roomTotalReservations = roomReservations.size();

            double occupancyPercentage =
                    calculateOccupancyPercentage(
                            reservedHours,
                            availableHoursPerRoom
                    );

            roomItems.add(
                    RoomUsageReportItem.builder()
                            .roomName(room.getName())
                            .totalReservations(roomTotalReservations)
                            .reservedHours(round(reservedHours))
                            .occupancyPercentage(round(occupancyPercentage))
                            .build()
            );

            totalReservations += roomTotalReservations;
            totalReservedHours += reservedHours;
            totalAvailableHours += availableHoursPerRoom;
        }

        roomItems.sort(
                Comparator.comparing(
                        RoomUsageReportItem::occupancyPercentage
                ).reversed()
        );

        double totalOccupancyPercentage =
                calculateOccupancyPercentage(
                        totalReservedHours,
                        totalAvailableHours
                );

        List<RoomUsageReportMetricResult> metrics =
                buildMetrics(
                        request.metrics(),
                        totalOccupancyPercentage,
                        totalReservations,
                        totalReservedHours
                );

        return RoomUsageReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .metrics(metrics)
                .rooms(roomItems)
                .build();
    }

    private List<RoomUsageReportMetricResult> buildMetrics(
            List<RoomUsageReportMetric> selectedMetrics,
            double occupancyPercentage,
            long totalReservations,
            double reservedHours
    ) {

        List<RoomUsageReportMetricResult> metrics =
                new ArrayList<>();

        for (RoomUsageReportMetric metric : selectedMetrics) {

            switch (metric) {

                case OCCUPANCY_PERCENTAGE -> metrics.add(
                        RoomUsageReportMetricResult.builder()
                                .name(metric)
                                .value(round(occupancyPercentage))
                                .build()
                );

                case TOTAL_RESERVATIONS -> metrics.add(
                        RoomUsageReportMetricResult.builder()
                                .name(metric)
                                .value(totalReservations)
                                .build()
                );

                case RESERVED_HOURS -> metrics.add(
                        RoomUsageReportMetricResult.builder()
                                .name(metric)
                                .value(round(reservedHours))
                                .build()
                );
            }
        }

        return metrics;
    }

    private double calculateEffectiveHours(
            Reservation reservation,
            Instant reportStart,
            Instant reportEnd
    ) {

        Instant effectiveStart =
                reservation.getStartAt().isBefore(reportStart)
                        ? reportStart
                        : reservation.getStartAt();

        Instant effectiveEnd =
                reservation.getEndAt().isAfter(reportEnd)
                        ? reportEnd
                        : reservation.getEndAt();

        if (!effectiveEnd.isAfter(effectiveStart)) {
            return 0;
        }

        return Duration.between(
                effectiveStart,
                effectiveEnd
        ).toMinutes() / 60.0;
    }

    private double calculateAvailableHours(
            LocalDate startDate,
            LocalDate endDate
    ) {

        long days = startDate
                .datesUntil(endDate.plusDays(1))
                .count();

        return days * AVAILABLE_HOURS_PER_DAY;
    }

    private double calculateOccupancyPercentage(
            double reservedHours,
            double availableHours
    ) {

        if (availableHours <= 0) {
            return 0;
        }

        return (reservedHours / availableHours) * 100;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @Override
    public byte[] generateRoomUsageReportPdf(RoomUsageReportRequest request) {

        RoomUsageReportResponse report =
                getRoomUsageReport(request);

        return roomUsageReportPdfGenerator.generatePdf(report);
    }

    @Override
    public byte[] generateRoomUsageReportCsv(RoomUsageReportRequest request) {

        RoomUsageReportResponse report =
                getRoomUsageReport(request);

        return roomUsageReportCsvGenerator.generateCsv(report);
    }
}