package com.coworking.admin.report.dto.financial;

import com.coworking.admin.report.enums.financial.FinancialReportMetric;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record FinancialReportRequest(

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate,

        @NotEmpty
        List<FinancialReportMetric> metrics

) {
}
