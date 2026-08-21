package com.coworking.controller.reports;

import com.coworking.admin.report.controller.AdminReportController;
import com.coworking.admin.report.dto.reservation.ReservationReportItem;
import com.coworking.admin.report.dto.reservation.ReservationReportMetricResult;
import com.coworking.admin.report.dto.reservation.ReservationReportResponse;
import com.coworking.admin.report.enums.reservation.ReservationReportMetric;
import com.coworking.admin.report.service.ReservationReportService;
import com.coworking.reservation.enums.ReservationStatus;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(AdminReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReservationReportControllerTest {


    @Autowired
    private MockMvc mockMvc;


    @MockitoBean
    private ReservationReportService reservationReportService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;


    @MockitoBean
    private JwtUtil jwtUtil;




    // =========================================================
    // GET RESERVATION REPORT
    // =========================================================


    @Test
    void shouldReturnReservationReport() throws Exception {


        ReservationReportResponse response =
                createReservationReport();


        when(
                reservationReportService.getReservationReport(any())
        ).thenReturn(response);


        String requestBody = """
               {
                   "startDate": "2026-07-01",
                   "endDate": "2026-07-31",
                   "metrics": [
                       "TOTAL_RESERVAS",
                       "PAID"
                   ]
               }
               """;


        mockMvc.perform(
                        post("/admin/reports/reservations")
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
                        jsonPath("$.metrics.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.reservations.length()")
                                .value(1)
                );
    }




    // =========================================================
    // VALIDATION
    // =========================================================


    @Test
    void shouldReturnBadRequestWhenStartDateIsMissing()
            throws Exception {


        String requestBody = """
               {
                   "endDate": "2026-07-31",
                   "metrics": [
                       "TOTAL_RESERVAS"
                   ]
               }
               """;


        mockMvc.perform(
                        post("/admin/reports/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }




    @Test
    void shouldReturnBadRequestWhenEndDateIsMissing()
            throws Exception {


        String requestBody = """
               {
                   "startDate": "2026-07-01",
                   "metrics": [
                       "TOTAL_RESERVAS"
                   ]
               }
               """;


        mockMvc.perform(
                        post("/admin/reports/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }




    @Test
    void shouldReturnBadRequestWhenEndDateIsBeforeStartDate()
            throws Exception {


        String requestBody = """
               {
                   "startDate": "2026-07-31",
                   "endDate": "2026-07-01",
                   "metrics": [
                       "TOTAL_RESERVAS"
                   ]
               }
               """;


        mockMvc.perform(
                        post("/admin/reports/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }




    @Test
    void shouldReturnBadRequestWhenMetricsAreEmpty()
            throws Exception {


        String requestBody = """
               {
                   "startDate": "2026-07-01",
                   "endDate": "2026-07-31",
                   "metrics": []
               }
               """;


        mockMvc.perform(
                        post("/admin/reports/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());
    }




    // =========================================================
    // PDF
    // =========================================================


    @Test
    void shouldGeneratePdf() throws Exception {


        byte[] pdf =
                "fake-pdf-content".getBytes();


        when(
                reservationReportService.generateReservationReportPdf(any())
        ).thenReturn(pdf);


        String requestBody = """
               {
                   "startDate": "2026-07-01",
                   "endDate": "2026-07-31",
                   "metrics": [
                       "TOTAL_RESERVAS"
                   ]
               }
               """;


        mockMvc.perform(
                        post("/admin/reports/reservations/pdf")
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
                                "inline; filename=reservation-report.pdf"
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
    void shouldGenerateCsv() throws Exception {


        byte[] csv =
                "COWORKING\r\nReporte de Reservas"
                        .getBytes();


        when(
                reservationReportService.generateReservationReportCsv(any())
        ).thenReturn(csv);


        String requestBody = """
               {
                   "startDate": "2026-07-01",
                   "endDate": "2026-07-31",
                   "metrics": [
                       "TOTAL_RESERVAS"
                   ]
               }
               """;


        mockMvc.perform(
                        post("/admin/reports/reservations/csv")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                "Content-Disposition",
                                "attachment; filename=reservation-report.csv"
                        )
                )
                .andExpect(
                        content().bytes(csv)
                );
    }




    // =========================================================
    // FIXTURE
    // =========================================================


    private ReservationReportResponse createReservationReport() {


        ReservationReportMetricResult totalReservations =
                new ReservationReportMetricResult(
                        ReservationReportMetric.TOTAL_RESERVAS,
                        1
                );


        ReservationReportMetricResult paid =
                new ReservationReportMetricResult(
                        ReservationReportMetric.PAID,
                        1
                );


        ReservationReportItem reservation =
                new ReservationReportItem(
                        1L,
                        "usuario_prueba",
                        "Sala A",
                        Instant.parse(
                                "2026-07-08T13:00:00Z"
                        ),
                        Instant.parse(
                                "2026-07-08T14:00:00Z"
                        ),
                        new BigDecimal("25.00"),
                        ReservationStatus.PAID
                );

        return new ReservationReportResponse(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                List.of(
                        totalReservations,
                        paid
                ),
                List.of(reservation)
        );
    }
}