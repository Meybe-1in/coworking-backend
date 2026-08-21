package com.coworking.admin.report.dto.financial;

import com.coworking.admin.report.enums.financial.FinancialReportMetric;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Builder
public record FinancialReportResponse(

        LocalDate startDate,
        LocalDate endDate,
        Map<FinancialReportMetric, Object> metrics,
        List<FinancialReportItem> reservations
) {
}
