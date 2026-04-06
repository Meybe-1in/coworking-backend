package com.coworking.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request para registrar un usuario")
public record RegisterRequest(
        String username,

        @NotBlank(message = "Email es obligatorio")
        @Email(message = "Formato de email inválido")
        String email,

        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
                message = "La contraseña debe tener mayúsculas, minúsculas, número y símbolo"
        )
        String password,

        Boolean termsAccepted
) {}

