package com.coworking.admin.report.controller;

import com.coworking.admin.report.dto.financial.FinancialReportRequest;
import com.coworking.admin.report.dto.financial.FinancialReportResponse;
import com.coworking.admin.report.dto.reservation.ReservationReportRequest;
import com.coworking.admin.report.dto.reservation.ReservationReportResponse;
import com.coworking.admin.report.dto.RoomUsageReportResponse;
import com.coworking.admin.report.service.AdminReportService;
import com.coworking.admin.report.service.FinancialReportService;
import jakarta.validation.Valid;
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
    private final FinancialReportService financialReportService;

    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                    GET REPORTS ENDPOINTS
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // Get reservation report
    @PostMapping("/reservations")
    public ReservationReportResponse getReservationReport(@Valid @RequestBody ReservationReportRequest request) {
        return adminReportService.getReservationReport(request);
    }

    // Generate reservation report in PDF
    @PostMapping(
            value = "/reservations/pdf",
            produces = "application/pdf"
    )
    public ResponseEntity<byte[]> generateReservationReportPdf(@Valid @RequestBody ReservationReportRequest request) {
        byte[] pdf = adminReportService.generateReservationReportPdf(request);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=reservation-report.pdf"
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // Generate reservation report in CSV
    @PostMapping(
            value = "/reservations/csv",
            produces = "text/csv"
    )
    public ResponseEntity<byte[]> generateReservationReportCsv(@Valid @RequestBody ReservationReportRequest request) {
        byte[] csv = adminReportService.generateReservationReportCsv(request);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=reservation-report.csv"
                )
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }



    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                 GET FINANCIAL REPORTS ENDPOINTS
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

    @PostMapping("/financial")
    public ResponseEntity<FinancialReportResponse> getFinancialReport(
            @Valid @RequestBody FinancialReportRequest request) {
        return ResponseEntity.ok(
                financialReportService.getFinancialReport(request)
        );
    }
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                  GET ROOM USAGE REPORTS ENDPOINTS
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    @GetMapping("/room-usage")
    public RoomUsageReportResponse getRoomUsageReport() {
        return adminReportService.getRoomUsageReport();
    }
}
