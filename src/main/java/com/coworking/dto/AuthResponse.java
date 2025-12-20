package com.coworking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta con el JWT")
public record AuthResponse(String token, String username, String role) {
}
