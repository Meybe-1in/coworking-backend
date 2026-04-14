package com.coworking.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

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

    @Schema(description = "Precio por hora", example = "5.0")
    private BigDecimal price;

    @Schema(description = "URL de imagen", example = "https://example.com/room.jpg")
    private String imageUrl;

    @Schema(description = "Ubicación de la sala", example = "Barrio La Cruz, Usulután, El Salvador")
    private String location;

    @Schema(description = "Características de la sala", example = "[\"Wifi\", \"Silla ergonómica\"]")
    private List<String> features;
}
