package com.coworking.controller.admin;

import com.coworking.admin.controller.AdminDashboardController;
import com.coworking.admin.dto.AdminStatsResponse;
import com.coworking.admin.dto.ChartPointResponse;
import com.coworking.admin.enums.ChartPeriod;
import com.coworking.admin.service.AdminDashboardService;

import com.coworking.security.JwtAuthenticationFilter;
import com.coworking.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.security.test.context.support.WithMockUser;


@WebMvcTest(AdminDashboardController.class)
@AutoConfigureMockMvc
class AdminDashboardControllerTest {

    @MockitoBean
    private AdminDashboardService dashboardService;

    @Autowired
    private MockMvc mockMvc;

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
    void shouldReturnAdminStats() throws Exception {

        AdminStatsResponse response =
                AdminStatsResponse.builder()
                        .totalReservations(10)
                        .activeReservations(5)
                        .pendingReservations(2)
                        .cancelledReservations(1)
                        .expiredReservations(2)

                        .totalUsers(20)
                        .activeUsers(18)
                        .disabledUsers(2)

                        .totalRooms(8)
                        .availableRooms(7)
                        .unavailableRooms(1)

                        .todayReservations(3)
                        .monthReservations(15)

                        .totalRevenue(BigDecimal.valueOf(500))
                        .monthlyRevenue(BigDecimal.valueOf(200))

                        .build();

        when(dashboardService.getStats()).thenReturn(response);


        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReservations")
                        .value(10))

                .andExpect(jsonPath("$.totalUsers")
                        .value(20))

                .andExpect(jsonPath("$.totalRevenue")
                        .value(500));
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnReservationsChart() throws Exception {


        List<ChartPointResponse> response =
                List.of(ChartPointResponse.builder()
                                .period("2026-07-20")
                                .total(5L)
                                .build(),

                        ChartPointResponse.builder()
                                .period("2026-07-21")
                                .total(3L)
                                .build()
                );

        when(dashboardService.getReservationsChart(ChartPeriod.WEEK)).thenReturn(response);
        mockMvc.perform(get("/admin/dashboard/reservations")
                        .param(
                                "period",
                                "WEEK"
                        )
                ).andExpect(status().isOk())

                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].period").value("2026-07-20"))
                .andExpect(jsonPath("$[0].total").value(5));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnMonthlyReservationsChart() throws Exception {

        List<ChartPointResponse> response =
                List.of(
                        ChartPointResponse.builder()
                                .period("Enero")
                                .total(45L)
                                .build(),

                        ChartPointResponse.builder()
                                .period("Febrero")
                                .total(62L)
                                .build()
                );

        when(
                dashboardService.getReservationsChart(
                        ChartPeriod.YEAR
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get("/admin/dashboard/reservations/monthly")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].period").value("Enero"))
                .andExpect(jsonPath("$[0].total").value(45))
                .andExpect(jsonPath("$[1].period").value("Febrero"))
                .andExpect(jsonPath("$[1].total").value(62));
    }

}
