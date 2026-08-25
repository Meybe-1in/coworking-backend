package com.coworking.controller.reports;

import com.coworking.admin.report.controller.AdminReportController;
import com.coworking.admin.report.dto.room.RoomUsageReportResponse;
import com.coworking.admin.report.service.FinancialReportService;
import com.coworking.admin.report.service.ReservationReportService;
import com.coworking.admin.report.service.RoomReportService;
import com.coworking.security.JwtAuthenticationFilter;
import com.coworking.security.JwtUtil;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoomUsageReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FinancialReportService financialReportService;

    @MockitoBean
    private ReservationReportService reservationReportService;

    @MockitoBean
    private RoomReportService roomReportService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtUtil jwtUtil;


    // =========================================================
    // ROOM USAGE REPORT
    // =========================================================

    @Test
    void shouldReturnRoomUsageReport()
            throws Exception {

        RoomUsageReportResponse response =
                createRoomUsageReport();

        when(
                roomReportService.getRoomUsageReport(any())
        ).thenReturn(response);

        String requestBody = """
                {
                    "startDate": "2026-07-01",
                    "endDate": "2026-07-31",
                    "metrics": [
                        "OCCUPANCY_PERCENTAGE",
                        "TOTAL_RESERVATIONS",
                        "RESERVED_HOURS"
                    ]
                }
                """;

        mockMvc.perform(
                        post("/admin/reports/room-usage")
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
                        jsonPath("$.metrics")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.rooms")
                                .exists()
                );
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    @Test
    void shouldReturnBadRequestWhenRoomUsageStartDateIsMissing()
            throws Exception {

        String requestBody = """
                {
                    "endDate": "2026-07-31",
                    "metrics": [
                        "OCCUPANCY_PERCENTAGE"
                    ]
                }
                """;

        mockMvc.perform(
                        post("/admin/reports/room-usage")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenRoomUsageEndDateIsMissing()
            throws Exception {

        String requestBody = """
                {
                    "startDate": "2026-07-01",
                    "metrics": [
                        "OCCUPANCY_PERCENTAGE"
                    ]
                }
                """;

        mockMvc.perform(
                        post("/admin/reports/room-usage")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenRoomUsageEndDateIsBeforeStartDate()
            throws Exception {

        String requestBody = """
                {
                    "startDate": "2026-07-31",
                    "endDate": "2026-07-01",
                    "metrics": [
                        "OCCUPANCY_PERCENTAGE"
                    ]
                }
                """;

        mockMvc.perform(
                        post("/admin/reports/room-usage")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenRoomUsageMetricsAreEmpty()
            throws Exception {

        String requestBody = """
                {
                    "startDate": "2026-07-01",
                    "endDate": "2026-07-31",
                    "metrics": []
                }
                """;

        mockMvc.perform(
                        post("/admin/reports/room-usage")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }


    // =========================================================
    // PDF
    // =========================================================

    @Test
    void shouldGenerateRoomUsagePdf()
            throws Exception {

        byte[] pdf =
                "fake-room-usage-pdf-content".getBytes();

        when(
                roomReportService.generateRoomUsageReportPdf(any())
        ).thenReturn(pdf);

        String requestBody = """
                {
                    "startDate": "2026-07-01",
                    "endDate": "2026-07-31",
                    "metrics": [
                        "OCCUPANCY_PERCENTAGE"
                    ]
                }
                """;

        mockMvc.perform(
                        post("/admin/reports/room-usage/pdf")
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
                                "inline; filename=room-usage-report.pdf"
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
    void shouldGenerateRoomUsageCsv()
            throws Exception {

        byte[] csv =
                "COWORKING\r\nReporte de Uso de Salas"
                        .getBytes();

        when(
                roomReportService.generateRoomUsageReportCsv(any())
        ).thenReturn(csv);

        String requestBody = """
                {
                    "startDate": "2026-07-01",
                    "endDate": "2026-07-31",
                    "metrics": [
                        "OCCUPANCY_PERCENTAGE"
                    ]
                }
                """;

        mockMvc.perform(
                        post("/admin/reports/room-usage/csv")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                "Content-Disposition",
                                "attachment; filename=room-usage-report.csv"
                        )
                )
                .andExpect(
                        content().bytes(csv)
                );
    }


    // =========================================================
    // FIXTURE
    // =========================================================

    private RoomUsageReportResponse createRoomUsageReport() {

        return RoomUsageReportResponse.builder()
                .startDate(
                        LocalDate.of(2026, 7, 1)
                )
                .endDate(
                        LocalDate.of(2026, 7, 31)
                )
                .metrics(List.of())
                .rooms(List.of())
                .build();
    }
}