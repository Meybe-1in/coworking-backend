package com.coworking.admin.report.dto.financial;

import com.coworking.admin.report.enums.financial.FinancialReportMetric;

public record FinancialReportMetricResult(
        FinancialReportMetric name,
        Object value
) {
}
