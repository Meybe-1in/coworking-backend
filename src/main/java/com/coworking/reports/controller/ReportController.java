package com.coworking.reports.controller;

import com.coworking.reports.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/reservations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportReservationsCsv(){
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=reservations.csv"
                )
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(
                        reportService.exportReservationsCsv()
                );
    }

    @GetMapping("/payments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportPaymentsCsv(){
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=payments.csv"
                )
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(
                        reportService.exportPaymentsCsv()
                );
    }
}
