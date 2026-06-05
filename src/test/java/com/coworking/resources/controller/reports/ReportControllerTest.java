package com.coworking.resources.controller.reports;

import com.coworking.reports.controller.ReportController;
import com.coworking.reports.service.ReportService;
import com.coworking.security.JwtAuthenticationFilter;
import com.coworking.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import static org.hamcrest.Matchers.containsString;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {

        doAnswer(invocation -> {
            invocation.<jakarta.servlet.FilterChain>getArgument(2)
                    .doFilter(
                            invocation.getArgument(0),
                            invocation.getArgument(1)
                    );
            return null;
        }).when(jwtAuthenticationFilter)
                .doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldExportReservationsCsv() throws Exception {

        when(reportService.exportReservationsCsv())
                .thenReturn(
                        "ID,Usuario,Sala\n1,dayana,Sala A"
                                .getBytes()
                );

        mockMvc.perform(
                        get("/admin/reports/reservations")
                )
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        containsString("reservations.csv")
                ));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldExportPaymentsCsv() throws Exception {

        when(reportService.exportPaymentsCsv())
                .thenReturn(
                        "ID,Reserva,Sala\n1,1,Sala A"
                                .getBytes()
                );

        mockMvc.perform(
                        get("/admin/reports/payments")
                )
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        containsString("payments.csv")
                ));
    }
}