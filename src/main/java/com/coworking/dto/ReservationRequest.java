package com.coworking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class ReservationRequest {

    @NotNull(message = "El ID de la sala es obligatorio")
    private Long roomId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime startAt;

    @NotNull(message = "La fecha de finalización es obligatoria")
    private LocalDateTime endAt;

    private String note;

}
