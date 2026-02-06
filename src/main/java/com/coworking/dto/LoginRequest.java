package com.coworking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

@Schema(description = "Request para login")
public record LoginRequest(
        @Email(message = "Formato de email inválido")
        String email,
        String password,
        boolean rememberMe)
{ }
