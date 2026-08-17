package com.coworking.admin.report.controller;

import com.coworking.admin.report.dto.FinancialReportResponse;
import com.coworking.admin.report.dto.reservation.ReservationReportRequest;
import com.coworking.admin.report.dto.reservation.ReservationReportResponse;
import com.coworking.admin.report.dto.RoomUsageReportResponse;
import com.coworking.admin.report.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @PostMapping("/reservations")
    public ReservationReportResponse getReservationReport(@RequestBody ReservationReportRequest request) {
        return adminReportService.getReservationReport(request);
    }

    @GetMapping("/financial")
    public FinancialReportResponse getFinancialReport() {
        return adminReportService.getFinancialReport();
    }

    @GetMapping("/room-usage")
    public RoomUsageReportResponse getRoomUsageReport() {
        return adminReportService.getRoomUsageReport();
    }
}
