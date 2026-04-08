package com.coworking.reservation.dto;

import com.coworking.reservation.model.ReservationStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter
public class ReservationResponse {
    private Long id;
    private String roomName;
    private String username;
    private Instant startAt;
    private Instant endAt;
    private Double price;
    private ReservationStatus status;

}
