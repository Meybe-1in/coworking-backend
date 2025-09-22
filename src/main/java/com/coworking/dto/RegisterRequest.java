package com.coworking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request para registrar un usuario")
public record RegisterRequest(String username, String password, String role) {
}
