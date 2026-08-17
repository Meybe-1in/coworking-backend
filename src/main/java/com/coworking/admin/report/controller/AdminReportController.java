package com.coworking.admin.report.controller;

import com.coworking.admin.report.dto.FinancialReportResponse;
import com.coworking.admin.report.dto.reservation.ReservationReportRequest;
import com.coworking.admin.report.dto.reservation.ReservationReportResponse;
import com.coworking.admin.report.dto.RoomUsageReportResponse;
import com.coworking.admin.report.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @PostMapping(
            value = "/reservations/pdf",
            produces = "application/pdf"
    )
    public ResponseEntity<byte[]> generateReservationReportPdf(@RequestBody ReservationReportRequest request) {
        byte[] pdf = adminReportService.generateReservationReportPdf(request);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=reservation-report.pdf"
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
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
