package com.coworking.admin.report.service;

import com.coworking.admin.report.dto.financial.FinancialReportRequest;
import com.coworking.admin.report.dto.financial.FinancialReportResponse;

public interface FinancialReportService {

    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                GET FINANCIAL REPORTS ENDPOINTS
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

    FinancialReportResponse getFinancialReport(
            FinancialReportRequest request
    );
}
