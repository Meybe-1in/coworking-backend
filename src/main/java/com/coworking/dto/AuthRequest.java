package com.coworking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AuthRequest {

    @Schema(description = "Correo electronico de usuario", example = "user1@gmail.com")
    private String email;

    @Schema(description = "Contraseña del usuario", example = "12345678")
    private String password;

}
