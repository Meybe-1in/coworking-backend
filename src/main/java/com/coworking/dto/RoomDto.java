package com.coworking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class RoomDto {

    @Schema(description = "ID de la sala", example = "1")
    private Long id;

    @Schema(description = "nombre de la sala", example = "Sala principal")
    private String name;

    @Schema(description = " descripcion de la sala", example = "espacio con pizarra y aire acondicionado")
    private String description;

    @Schema(description = "capacidad maxima", example = "6")
    private Integer capacity;

    @Schema(description = "Disponibilidad actual", example = "true")
    private boolean available;
}
