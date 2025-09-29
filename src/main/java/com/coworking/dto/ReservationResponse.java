package com.coworking.dto;

import com.coworking.model.ReservationStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class ReservationResponse {
    private Long id;
    private String roomName;
    private String username;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private ReservationStatus status;
}
