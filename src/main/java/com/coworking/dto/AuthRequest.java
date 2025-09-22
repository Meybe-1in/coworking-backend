package com.coworking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AuthRequest {

    @Schema(description = "Nombre de usuario", example = "user1")
    private String username;

    @Schema(description = "Contraseña del usuario", example = "12345678")
    private String password;

}
