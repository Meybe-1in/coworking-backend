package com.coworking.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter
public class ReservationRequest {

    @NotNull(message = "El ID de la sala es obligatorio")
    private Long roomId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private Instant startAt;

    @NotNull(message = "La fecha de finalización es obligatoria")
    private Instant endAt;

    @NotBlank
    private String note;

}
