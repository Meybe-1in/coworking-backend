package com.coworking.reservation.dto;

import com.coworking.reservation.enums.ReservationStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter
public class ReservationResponse {
    private Long id;
    private String roomName;
    private String username;
    private Instant startAt;
    private Instant endAt;
    private BigDecimal price;
    private Instant createdAt;
    private ReservationStatus status;

}
