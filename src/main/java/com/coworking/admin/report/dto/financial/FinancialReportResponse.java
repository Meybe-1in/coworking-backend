package com.coworking.admin.report.dto.financial;

import com.coworking.admin.report.enums.financial.FinancialReportMetric;

import java.time.LocalDate;
import java.util.Map;

public record FinancialReportResponse(

        LocalDate startDate,

        LocalDate endDate,

        Map<FinancialReportMetric, Object> metrics
) {
}
