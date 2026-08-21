package com.coworking.admin.report.service;
import com.coworking.admin.report.dto.financial.FinancialReportResponse;
import com.coworking.admin.report.dto.reservation.ReservationReportRequest;
import com.coworking.admin.report.dto.reservation.ReservationReportResponse;
import com.coworking.admin.report.dto.RoomUsageReportResponse;

public interface AdminReportService {

    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                      GET REPORTS ENDPOINTS
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    ReservationReportResponse getReservationReport(
            ReservationReportRequest request
    );

    // generated reservation report in PDF
    byte[] generateReservationReportPdf(
            ReservationReportRequest request
    );

    // generated reservation report in CSV
    byte[] generateReservationReportCsv(
            ReservationReportRequest request
    );

    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                        GET FINANCIAL REPORTS ENDPOINTS
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

    FinancialReportResponse getFinancialReport();

    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                        GET ROOM USAGE REPORTS ENDPOINTS
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

    RoomUsageReportResponse getRoomUsageReport();
}
