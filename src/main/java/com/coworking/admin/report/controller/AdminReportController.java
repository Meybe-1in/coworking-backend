package com.coworking.admin.report.controller;

import com.coworking.admin.report.dto.FinancialReportResponse;
import com.coworking.admin.report.dto.ReservationReportResponse;
import com.coworking.admin.report.dto.RoomUsageReportResponse;
import com.coworking.admin.report.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/report")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping("/reservations")
    public ReservationReportResponse getReservationReport() {
        return adminReportService.getReservationReport();
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
