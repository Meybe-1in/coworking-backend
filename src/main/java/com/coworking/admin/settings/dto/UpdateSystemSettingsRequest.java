package com.coworking.admin.settings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record UpdateSystemSettingsRequest(
        @NotNull(message = "La hora de apertura es obligatoria")
        LocalTime openingTime,

        @NotNull(message = "La hora de cierre es obligatoria")
        LocalTime closingTime,

        @NotNull(message = "La duración máxima es obligatoria")
        @Min(value = 1, message = "La duración máxima debe ser mayor que 0")
        Integer maxReservationHours,

        @NotNull(message = "El tiempo de expiración es obligatorio")
        @Min(value = 1, message = "El tiempo de expiración debe ser mayor que 0")
        Integer pendingExpirationMinutes,

        @NotBlank(message = "El nombre institucional es obligatorio")
        String institutionName,

        @Email(message = "Formato de correo inválido")
        String institutionEmail,

        String institutionPhone,

        String institutionAddress
) {
}
