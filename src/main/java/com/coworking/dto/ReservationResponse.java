package com.coworking.dto;

import com.coworking.model.ReservationStatus;
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
    private ReservationStatus status;
}
