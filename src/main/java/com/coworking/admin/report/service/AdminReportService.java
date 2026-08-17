package com.coworking.admin.report.service;

import com.coworking.admin.report.dto.FinancialReportResponse;
import com.coworking.admin.report.dto.reservation.ReservationReportRequest;
import com.coworking.admin.report.dto.reservation.ReservationReportResponse;
import com.coworking.admin.report.dto.RoomUsageReportResponse;

public interface AdminReportService {
    ReservationReportResponse getReservationReport(
            ReservationReportRequest request
    );

    byte[] generateReservationReportPdf(
            ReservationReportRequest request
    );

    FinancialReportResponse getFinancialReport();

    RoomUsageReportResponse getRoomUsageReport();
}
