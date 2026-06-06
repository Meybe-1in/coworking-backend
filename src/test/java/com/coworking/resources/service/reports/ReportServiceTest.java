package com.coworking.resources.service.reports;

import com.coworking.payment.repository.PaymentRepository;
import com.coworking.reports.service.ReportServiceImpl;
import com.coworking.reservation.enums.ReservationStatus;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import com.coworking.room.model.Room;
import com.coworking.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void shouldGenerateReservationsCsv() {

        User user = new User();
        user.setEmail("test@test.com");

        Room room = new Room();
        room.setName("Sala A");

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setUser(user);
        reservation.setRoom(room);
        reservation.setStartAt(LocalDateTime.of(2025, 1, 1, 10, 0).toInstant(ZoneOffset.UTC));
        reservation.setEndAt(LocalDateTime.of(2025, 1, 1, 12, 0).toInstant(ZoneOffset.UTC));
        reservation.setPrice(BigDecimal.valueOf(50));
        reservation.setStatus(ReservationStatus.valueOf("PAID"));

        when(reservationRepository.findAll())
                .thenReturn(List.of(reservation));

        byte[] csv = reportService.exportReservationsCsv();

        String content = new String(csv);

        assertTrue(content.contains("ID"));
        assertTrue(content.contains("test@test.com"));
        assertTrue(content.contains("Sala A"));
        assertTrue(content.contains("50"));
    }
}