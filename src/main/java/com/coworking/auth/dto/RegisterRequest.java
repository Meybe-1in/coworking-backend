package com.coworking.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request para registrar un usuario")
public record RegisterRequest(

        String username,

        @NotBlank(message = "Email es obligatorio")
        @Email(message = "Formato de email inválido")
        String email,

        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._-])[A-Za-z\\d@$!%*?&._-]{8,}$",
                message = "La contraseña debe contener mayúscula, minúscula, número y símbolo"
        )
        String password,

        Boolean termsAccepted
) {}

