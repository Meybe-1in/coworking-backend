package com.coworking.admin.report.controller;

import com.coworking.admin.report.dto.financial.FinancialReportRequest;
import com.coworking.admin.report.dto.financial.FinancialReportResponse;
import com.coworking.admin.report.dto.reservation.ReservationReportRequest;
import com.coworking.admin.report.dto.reservation.ReservationReportResponse;
import com.coworking.admin.report.service.FinancialReportService;
import com.coworking.admin.report.service.ReservationReportService;
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

    private final FinancialReportService financialReportService;
    private final ReservationReportService reservationReportService;

    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                    GET REPORTS ENDPOINTS
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // Get reservation report
    @PostMapping("/reservations")
    public ReservationReportResponse getReservationReport(@Valid @RequestBody ReservationReportRequest request) {
        return reservationReportService.getReservationReport(request);
    }

    // Generate reservation report in PDF
    @PostMapping(
            value = "/reservations/pdf",
            produces = "application/pdf"
    )
    public ResponseEntity<byte[]> generateReservationReportPdf(@Valid @RequestBody ReservationReportRequest request) {
        byte[] pdf = reservationReportService.generateReservationReportPdf(request);
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
        byte[] csv = reservationReportService.generateReservationReportCsv(request);
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

    // Generate financial report in PDF
    @PostMapping(
            value = "/financial/pdf",
            produces = "application/pdf"
    )
    public ResponseEntity<byte[]> generateFinancialReportPdf(@Valid @RequestBody FinancialReportRequest request) {
        byte[] pdf = financialReportService.generateFinancialReportPdf(request);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=financial-report.pdf"
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // Generate financial report in CSV
    @PostMapping(
            value = "/financial/csv",
            produces = "text/csv"
    )
    public ResponseEntity<byte[]> generateFinancialReportCsv(@Valid @RequestBody FinancialReportRequest request) {
        byte[] csv = financialReportService.generateFinancialReportCsv(request);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=financial-report.csv"
                )
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
