package com.coworking.service.reports;

import com.coworking.admin.report.dto.financial.FinancialReportRequest;
import com.coworking.admin.report.dto.financial.FinancialReportResponse;
import com.coworking.admin.report.enums.financial.FinancialReportMetric;
import com.coworking.admin.report.generator.financial.FinancialReportCsvGenerator;
import com.coworking.admin.report.generator.financial.FinancialReportPdfGenerator;
import com.coworking.admin.report.service.FinancialReportServiceImpl;
import com.coworking.payment.enums.PaymentStatus;
import com.coworking.payment.model.Payment;
import com.coworking.payment.repository.PaymentRepository;
import com.coworking.reservation.model.Reservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialReportServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FinancialReportPdfGenerator financialReportPdfGenerator;

    @Mock
    private FinancialReportCsvGenerator financialReportCsvGenerator;

    @Mock
    private Payment payment;

    @Mock
    private Reservation reservation;

    @Mock
    private com.coworking.user.model.User user;

    @Mock
    private com.coworking.room.model.Room room;

    private FinancialReportServiceImpl service;

    private final LocalDate START_DATE =
            LocalDate.of(2026, 8, 1);

    private final LocalDate END_DATE =
            LocalDate.of(2026, 8, 20);

    private final Instant START_INSTANT =
            Instant.parse("2026-08-01T06:00:00Z");

    private final Instant END_INSTANT =
            Instant.parse("2026-08-21T06:00:00Z");

    @BeforeEach
    void setUp() {

        service = new FinancialReportServiceImpl(
                paymentRepository,
                financialReportPdfGenerator,
                financialReportCsvGenerator
        );
    }

    // =========================================================
    // GET FINANCIAL REPORT
    // =========================================================

    @Test
    void shouldGetFinancialReportWithAllMetrics() {

        FinancialReportRequest request =
                new FinancialReportRequest(
                        START_DATE,
                        END_DATE,
                        List.of(
                                FinancialReportMetric.TOTAL_REVENUE,
                                FinancialReportMetric.TOTAL_RESERVATIONS,
                                FinancialReportMetric.AVERAGE_RESERVATION,
                                FinancialReportMetric.SUCCESSFUL_PAYMENTS
                        )
                );

        BigDecimal revenue =
                new BigDecimal("300.00");

        when(paymentRepository.findSuccessfulPaymentsByPeriod(
                eq(PaymentStatus.SUCCEEDED),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(List.of(payment));

        when(paymentRepository.getRevenueByPeriod(
                eq(PaymentStatus.SUCCEEDED),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(revenue);

        when(paymentRepository
                .countReservationsByPaymentStatusAndPeriod(
                        eq(PaymentStatus.SUCCEEDED),
                        any(Instant.class),
                        any(Instant.class)
                ))
                .thenReturn(3L);

        when(paymentRepository.countPaymentsByStatusAndPeriod(
                eq(PaymentStatus.SUCCEEDED),
                any(Instant.class),
                any(Instant.class)
        )).thenReturn(3L);

        mockPayment();

        FinancialReportResponse response =
                service.getFinancialReport(request);

        assertNotNull(response);

        assertEquals(
                START_DATE,
                response.startDate()
        );

        assertEquals(
                END_DATE,
                response.endDate()
        );

        assertEquals(
                new BigDecimal("300.00"),
                response.metrics().get(
                        FinancialReportMetric.TOTAL_REVENUE
                )
        );

        assertEquals(
                3L,
                response.metrics().get(
                        FinancialReportMetric.TOTAL_RESERVATIONS
                )
        );

        assertEquals(
                new BigDecimal("100.00"),
                response.metrics().get(
                        FinancialReportMetric.AVERAGE_RESERVATION
                )
        );

        assertEquals(
                3L,
                response.metrics().get(
                        FinancialReportMetric.SUCCESSFUL_PAYMENTS
                )
        );

        assertEquals(
                1,
                response.reservations().size()
        );

        verify(paymentRepository)
                .findSuccessfulPaymentsByPeriod(
                        eq(PaymentStatus.SUCCEEDED),
                        any(Instant.class),
                        any(Instant.class)
                );
    }

    // =========================================================
    // ONLY TOTAL REVENUE
    // =========================================================

    @Test
    void shouldCalculateTotalRevenue() {

        FinancialReportRequest request =
                new FinancialReportRequest(
                        START_DATE,
                        END_DATE,
                        List.of(
                                FinancialReportMetric.TOTAL_REVENUE
                        )
                );

        BigDecimal revenue =
                new BigDecimal("450.50");

        when(paymentRepository.findSuccessfulPaymentsByPeriod(
                any(),
                any(),
                any()
        )).thenReturn(List.of());

        when(paymentRepository.getRevenueByPeriod(
                eq(PaymentStatus.SUCCEEDED),
                any(),
                any()
        )).thenReturn(revenue);

        FinancialReportResponse response =
                service.getFinancialReport(request);

        assertEquals(
                revenue,
                response.metrics().get(
                        FinancialReportMetric.TOTAL_REVENUE
                )
        );

        verify(paymentRepository)
                .getRevenueByPeriod(
                        eq(PaymentStatus.SUCCEEDED),
                        any(),
                        any()
                );
    }

    // =========================================================
    // AVERAGE = ZERO
    // =========================================================

    @Test
    void shouldReturnZeroAverageWhenThereAreNoReservations() {

        FinancialReportRequest request =
                new FinancialReportRequest(
                        START_DATE,
                        END_DATE,
                        List.of(
                                FinancialReportMetric.AVERAGE_RESERVATION
                        )
                );

        when(paymentRepository.findSuccessfulPaymentsByPeriod(
                any(),
                any(),
                any()
        )).thenReturn(List.of());

        when(paymentRepository.getRevenueByPeriod(
                eq(PaymentStatus.SUCCEEDED),
                any(),
                any()
        )).thenReturn(BigDecimal.ZERO);

        when(paymentRepository
                .countReservationsByPaymentStatusAndPeriod(
                        eq(PaymentStatus.SUCCEEDED),
                        any(),
                        any()
                ))
                .thenReturn(0L);

        FinancialReportResponse response =
                service.getFinancialReport(request);

        assertEquals(
                BigDecimal.ZERO,
                response.metrics().get(
                        FinancialReportMetric.AVERAGE_RESERVATION
                )
        );
    }

    // =========================================================
    // INVALID DATES
    // =========================================================

    @Test
    void shouldRejectWhenStartDateIsAfterEndDate() {

        FinancialReportRequest request =
                new FinancialReportRequest(
                        LocalDate.of(2026, 8, 20),
                        LocalDate.of(2026, 8, 1),
                        List.of(
                                FinancialReportMetric.TOTAL_REVENUE
                        )
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.getFinancialReport(request)
                );

        assertEquals(
                "La fecha de inicio no puede ser posterior a la fecha de fin",
                exception.getMessage()
        );

        verifyNoInteractions(paymentRepository);
    }

    // =========================================================
    // GENERATE PDF
    // =========================================================

    @Test
    void shouldGenerateFinancialReportPdf() {

        FinancialReportRequest request =
                new FinancialReportRequest(
                        START_DATE,
                        END_DATE,
                        List.of(
                                FinancialReportMetric.TOTAL_REVENUE
                        )
                );

        when(paymentRepository.findSuccessfulPaymentsByPeriod(
                any(),
                any(),
                any()
        )).thenReturn(List.of());

        when(paymentRepository.getRevenueByPeriod(
                any(),
                any(),
                any()
        )).thenReturn(
                new BigDecimal("100.00")
        );

        FinancialReportResponse report =
                service.getFinancialReport(request);

        byte[] expectedPdf =
                "PDF".getBytes();

        when(financialReportPdfGenerator.generatePdf(report))
                .thenReturn(expectedPdf);

        byte[] result =
                service.generateFinancialReportPdf(request);

        assertArrayEquals(
                expectedPdf,
                result
        );

        verify(financialReportPdfGenerator)
                .generatePdf(any(FinancialReportResponse.class));
    }

    // =========================================================
    // GENERATE CSV
    // =========================================================

    @Test
    void shouldGenerateFinancialReportCsv() {

        FinancialReportRequest request =
                new FinancialReportRequest(
                        START_DATE,
                        END_DATE,
                        List.of(
                                FinancialReportMetric.TOTAL_REVENUE
                        )
                );

        when(paymentRepository.findSuccessfulPaymentsByPeriod(
                any(),
                any(),
                any()
        )).thenReturn(List.of());

        when(paymentRepository.getRevenueByPeriod(
                any(),
                any(),
                any()
        )).thenReturn(
                new BigDecimal("100.00")
        );

        FinancialReportResponse report =
                service.getFinancialReport(request);

        byte[] expectedCsv =
                "COWORKING".getBytes();

        when(financialReportCsvGenerator.generateCsv(report))
                .thenReturn(expectedCsv);

        byte[] result =
                service.generateFinancialReportCsv(request);

        assertArrayEquals(
                expectedCsv,
                result
        );

        verify(financialReportCsvGenerator)
                .generateCsv(any(FinancialReportResponse.class));
    }

    // =========================================================
    // MOCK PAYMENT
    // =========================================================

    private void mockPayment() {

        when(payment.getReservation())
                .thenReturn(reservation);

        when(payment.getAmount())
                .thenReturn(new BigDecimal("300.00"));

        when(payment.getStatus())
                .thenReturn(PaymentStatus.SUCCEEDED);

        when(payment.getPaidAt())
                .thenReturn(
                        Instant.parse(
                                "2026-08-10T18:00:00Z"
                        )
                );

        when(reservation.getId())
                .thenReturn(1L);

        when(reservation.getUser())
                .thenReturn(user);

        when(user.getUsername())
                .thenReturn("dayana");

        when(reservation.getRoom())
                .thenReturn(room);

        when(room.getName())
                .thenReturn("Sala Ejecutiva");

        when(reservation.getStartAt())
                .thenReturn(
                        Instant.parse(
                                "2026-08-10T16:00:00Z"
                        )
                );

        when(reservation.getEndAt())
                .thenReturn(
                        Instant.parse(
                                "2026-08-10T18:00:00Z"
                        )
                );
    }
}