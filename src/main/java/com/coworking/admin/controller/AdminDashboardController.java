package com.coworking.admin.controller;

import com.coworking.admin.dto.AdminStatsResponse;
import com.coworking.admin.dto.ChartPointResponse;
import com.coworking.admin.dto.RecentActivityResponse;
import com.coworking.admin.dto.RoomOccupancyResponse;
import com.coworking.admin.enums.ChartPeriod;
import com.coworking.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    //                      Dashboard stats
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // Devuelve métricas generales para el dashboard administrativo
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(
                dashboardService.getStats()
        );
    }

    //                      Reservations
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // Devuelve el número de reservas por periodo
    @GetMapping("/dashboard/reservations")
    public ResponseEntity<List<ChartPointResponse>>
    getReservationsChart(
            @RequestParam(defaultValue = "MONTH") ChartPeriod period
    ) {

        return ResponseEntity.ok(
                dashboardService.getReservationsChart(period)
        );
    }

    @GetMapping("/dashboard/reservations/monthly")
    public ResponseEntity<List<ChartPointResponse>> getMonthlyReservations() {

        return ResponseEntity.ok(
                dashboardService.getReservationsChart(
                        ChartPeriod.YEAR
                )
        );
    }

    //              Revenue
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // Devuelve el ingreso por periodo

    @GetMapping("/dashboard/revenue")
    public ResponseEntity<List<ChartPointResponse>> getRevenueChart(
            @RequestParam(defaultValue = "MONTH") ChartPeriod period
    ) {

        return ResponseEntity.ok(
                dashboardService.getRevenueChart(period)
        );
    }

    //              Room Occupancy
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // Devuelve el número de reservas por habitación
    @GetMapping("/dashboard/room-occupancy")
    public ResponseEntity<List<RoomOccupancyResponse>> getRoomOccupancy() {
        return ResponseEntity.ok(
                dashboardService.getRoomOccupancy()
        );
    }

    //              Recent Activity
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // Devuelve las actividades más recientes del sistema

    @GetMapping("/dashboard/recent-activity")
    public ResponseEntity<List<RecentActivityResponse>>
    getRecentActivity() {

        return ResponseEntity.ok(
                dashboardService.getRecentActivities()
        );
    }
}
