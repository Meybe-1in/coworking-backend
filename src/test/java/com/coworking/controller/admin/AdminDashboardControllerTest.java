package com.coworking.controller.admin;

import com.coworking.admin.controller.AdminDashboardController;
import com.coworking.admin.dto.AdminStatsResponse;
import com.coworking.admin.dto.ChartPointResponse;
import com.coworking.admin.dto.RecentActivityResponse;
import com.coworking.admin.dto.RoomOccupancyResponse;
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
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;
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
                                .total(BigDecimal.valueOf(5))
                                .build(),

                        ChartPointResponse.builder()
                                .period("2026-07-21")
                                .total(BigDecimal.valueOf(3))
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
                                .total(BigDecimal.valueOf(45))
                                .build(),

                        ChartPointResponse.builder()
                                .period("Febrero")
                                .total(BigDecimal.valueOf(62))
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

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnRoomOccupancy() throws Exception {

        List<RoomOccupancyResponse> response =
                List.of(
                        RoomOccupancyResponse.builder()
                                .roomName("Sala A")
                                .reservationCount(20)
                                .reservedHours(BigDecimal.valueOf(100))
                                .occupancyPercentage(BigDecimal.valueOf(75))
                                .build(),

                        RoomOccupancyResponse.builder()
                                .roomName("Sala B")
                                .reservationCount(10)
                                .reservedHours(BigDecimal.valueOf(50))
                                .occupancyPercentage(BigDecimal.valueOf(40))
                                .build()
                );


        when(dashboardService.getRoomOccupancy()).thenReturn(response);

        mockMvc.perform(
                        get("/admin/dashboard/room-occupancy")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].roomName").value("Sala A"))
                .andExpect(jsonPath("$[0].reservationCount").value(20))
                .andExpect(jsonPath("$[0].reservedHours").value(100))
                .andExpect(jsonPath("$[0].occupancyPercentage").value(75));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnRecentActivities() throws Exception {

        List<RecentActivityResponse> response = List.of(

                RecentActivityResponse.builder()
                        .type("Pago")
                        .description("Completó el pago de Sala Ejecutiva")
                        .user("ana")
                        .date(Instant.now())
                        .build(),

                RecentActivityResponse.builder()
                        .type("Reserva")
                        .description("Creó una reserva para Sala Ejecutiva")
                        .user("juan")
                        .date(Instant.now().minusSeconds(3600))
                        .build()
        );

        when(dashboardService.getRecentActivities()).thenReturn(response);

        mockMvc.perform(get("/admin/dashboard/recent-activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))

                .andExpect(jsonPath("$[0].type").value("Pago"))

                .andExpect(jsonPath("$[0].description").value("Completó el pago de Sala Ejecutiva"))

                .andExpect(jsonPath("$[0].user").value("ana"))

                .andExpect(jsonPath("$[1].type").value("Reserva"))

                .andExpect(jsonPath("$[1].description").value("Creó una reserva para Sala Ejecutiva"))

                .andExpect(jsonPath("$[1].user").value("juan"));

        verify(dashboardService).getRecentActivities();
    }

}
