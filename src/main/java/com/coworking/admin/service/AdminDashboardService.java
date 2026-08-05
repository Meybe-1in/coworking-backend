package com.coworking.admin.service;

import com.coworking.admin.dto.AdminStatsResponse;
import com.coworking.admin.dto.ChartPointResponse;
import com.coworking.admin.dto.RecentActivityResponse;
import com.coworking.admin.dto.RoomOccupancyResponse;
import com.coworking.admin.enums.ChartPeriod;

import java.util.List;

public interface AdminDashboardService {

    AdminStatsResponse getStats();

    List<ChartPointResponse> getReservationsChart(
            ChartPeriod period
    );

    List<ChartPointResponse> getRevenueChart(
            ChartPeriod period
    );

    List<RoomOccupancyResponse> getRoomOccupancy();

    List<RecentActivityResponse> getRecentActivities();
}
