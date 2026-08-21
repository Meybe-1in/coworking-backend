package com.coworking.controller.reports;

import com.coworking.admin.report.controller.AdminReportController;
import com.coworking.admin.report.dto.financial.FinancialReportResponse;
import com.coworking.admin.report.enums.financial.FinancialReportMetric;
import com.coworking.admin.report.service.AdminReportService;
import com.coworking.admin.report.service.FinancialReportService;
import com.coworking.security.JwtAuthenticationFilter;
import com.coworking.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class FinancialReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FinancialReportService financialReportService;

    @MockitoBean
    private AdminReportService adminReportService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtUtil jwtUtil;


    // =========================================================
    // FINANCIAL REPORT
    // =========================================================

    @Test
    void shouldReturnFinancialReport()
            throws Exception {

        FinancialReportResponse response =
                createFinancialReport();

        when(
                financialReportService.getFinancialReport(any())
        ).thenReturn(response);

        String requestBody = """
                {
                    "startDate": "2026-07-01",
                    "endDate": "2026-07-31",
                    "metrics": [
                        "TOTAL_REVENUE",
                        "TOTAL_RESERVATIONS",
                        "AVERAGE_RESERVATION",
                        "SUCCESSFUL_PAYMENTS"
                    ]
                }
                """;

        mockMvc.perform(
                        post("/admin/reports/financial")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.startDate")
                                .value("2026-07-01")
                )
                .andExpect(
                        jsonPath("$.endDate")
                                .value("2026-07-31")
                )
                .andExpect(
                        jsonPath("$.metrics").exists()
                )
                .andExpect(
                        jsonPath("$.reservations").exists()
                );
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    @Test
    void shouldReturnBadRequestWhenFinancialStartDateIsMissing()
            throws Exception {

        String requestBody = """
                {
                    "endDate": "2026-07-31",
                    "metrics": [
                        "TOTAL_REVENUE"
                    ]
                }
                """;

        mockMvc.perform(
                        post("/admin/reports/financial")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenFinancialEndDateIsMissing()
            throws Exception {

        String requestBody = """
                {
                    "startDate": "2026-07-01",
                    "metrics": [
                        "TOTAL_REVENUE"
                    ]
                }
                """;

        mockMvc.perform(
                        post("/admin/reports/financial")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenFinancialEndDateIsBeforeStartDate()
            throws Exception {

        String requestBody = """
                {
                    "startDate": "2026-07-31",
                    "endDate": "2026-07-01",
                    "metrics": [
                        "TOTAL_REVENUE"
                    ]
                }
                """;

        mockMvc.perform(
                        post("/admin/reports/financial")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenFinancialMetricsAreEmpty()
            throws Exception {

        String requestBody = """
                {
                    "startDate": "2026-07-01",
                    "endDate": "2026-07-31",
                    "metrics": []
                }
                """;

        mockMvc.perform(
                        post("/admin/reports/financial")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }


    // =========================================================
    // PDF
    // =========================================================

    @Test
    void shouldGenerateFinancialPdf()
            throws Exception {

        byte[] pdf =
                "fake-financial-pdf-content".getBytes();

        when(
                financialReportService.generateFinancialReportPdf(any())
        ).thenReturn(pdf);

        String requestBody = """
                {
                    "startDate": "2026-07-01",
                    "endDate": "2026-07-31",
                    "metrics": [
                        "TOTAL_REVENUE"
                    ]
                }
                """;

        mockMvc.perform(
                        post("/admin/reports/financial/pdf")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentType(
                                MediaType.APPLICATION_PDF
                        )
                )
                .andExpect(
                        header().string(
                                "Content-Disposition",
                                "inline; filename=financial-report.pdf"
                        )
                )
                .andExpect(
                        content().bytes(pdf)
                );
    }


    // =========================================================
    // CSV
    // =========================================================

    @Test
    void shouldGenerateFinancialCsv()
            throws Exception {

        byte[] csv =
                "COWORKING\r\nReporte Financiero"
                        .getBytes();

        when(
                financialReportService.generateFinancialReportCsv(any())
        ).thenReturn(csv);

        String requestBody = """
                {
                    "startDate": "2026-07-01",
                    "endDate": "2026-07-31",
                    "metrics": [
                        "TOTAL_REVENUE"
                    ]
                }
                """;

        mockMvc.perform(
                        post("/admin/reports/financial/csv")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                "Content-Disposition",
                                "attachment; filename=financial-report.csv"
                        )
                )
                .andExpect(
                        content().bytes(csv)
                );
    }


    // =========================================================
    // FIXTURE
    // =========================================================

    private FinancialReportResponse createFinancialReport() {

        Map<FinancialReportMetric, Object> metrics =
                new LinkedHashMap<>();

        metrics.put(
                FinancialReportMetric.TOTAL_REVENUE,
                new BigDecimal("150.00")
        );

        metrics.put(
                FinancialReportMetric.TOTAL_RESERVATIONS,
                5
        );

        metrics.put(
                FinancialReportMetric.AVERAGE_RESERVATION,
                new BigDecimal("30.00")
        );

        metrics.put(
                FinancialReportMetric.SUCCESSFUL_PAYMENTS,
                5
        );

        return new FinancialReportResponse(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                metrics,
                List.of()
        );
    }
}