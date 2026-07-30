package com.coworking.admin.controller;

import com.coworking.admin.dto.AdminStatsResponse;
import com.coworking.admin.dto.ChartPointResponse;
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
    @GetMapping("/dashboard/reservations")
    public ResponseEntity<List<ChartPointResponse>>
    getReservationsChart(
            @RequestParam(defaultValue = "MONTH") ChartPeriod period
    ) {

        return ResponseEntity.ok(
                dashboardService.getReservationsChart(period)
        );
    }

}
