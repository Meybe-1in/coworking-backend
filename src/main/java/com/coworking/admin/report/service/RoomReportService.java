package com.coworking.admin.report.service;

import com.coworking.admin.report.dto.room.RoomUsageReportRequest;
import com.coworking.admin.report.dto.room.RoomUsageReportResponse;

public interface RoomReportService {

    // Get room usage report
    RoomUsageReportResponse getRoomUsageReport(
            RoomUsageReportRequest request
    );

    // Generate room usage report in PDF
    byte[] generateRoomUsageReportPdf(
            RoomUsageReportRequest request
    );

    // Generate room usage report in CSV
    byte[] generateRoomUsageReportCsv(
            RoomUsageReportRequest request
    );
}
