package com.coworking.admin.report.service;
import com.coworking.admin.report.dto.reservation.ReservationReportRequest;
import com.coworking.admin.report.dto.reservation.ReservationReportResponse;

public interface ReservationReportService {

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

}
