package com.coworking.reports.service;

import com.coworking.payment.model.Payment;
import com.coworking.payment.repository.PaymentRepository;
import com.coworking.reservation.model.Reservation;
import com.coworking.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService{

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public byte[] exportReservationsCsv() {
        List<Reservation> reservations =
                reservationRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Usuario,Sala,Inicio,Fin,Precio,Estado\n");

        for(Reservation reservation : reservations){
            csv.append(reservation.getId()).append(",");
            csv.append(reservation.getUser().getEmail()).append(",");
            csv.append(reservation.getRoom().getName()).append(",");
            csv.append(reservation.getStartAt()).append(",");
            csv.append(reservation.getEndAt()).append(",");
            csv.append(reservation.getPrice()).append(",");
            csv.append(reservation.getStatus()).append("\n");
        }

        return csv.toString()
                .getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportPaymentsCsv() {
        List<Payment> payments =
                paymentRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Reserva,Sala,Monto,Metodo,Estado,FechaPago\n");
        for(Payment payment : payments){
            csv.append(payment.getId()).append(",");
            csv.append(payment.getReservation().getId()).append(",");
            csv.append(payment.getReservation().getRoom().getName()).append(",");
            csv.append(payment.getAmount()).append(",");
            csv.append(payment.getPaymentMethod()).append(",");
            csv.append(payment.getStatus()).append(",");
            csv.append(payment.getPaidAt()).append("\n");
        }
        return csv.toString()
                .getBytes(StandardCharsets.UTF_8);
    }
}
