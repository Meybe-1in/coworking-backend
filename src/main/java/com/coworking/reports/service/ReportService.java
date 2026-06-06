package com.coworking.reports.service;

public interface ReportService {

    byte[] exportReservationsCsv();
    byte[] exportPaymentsCsv();
}
