package com.coworking.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request para login")
public record LoginRequest(String email, String password) {
}
