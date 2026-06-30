package com.coworking.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRoleRequest(
        @NotBlank(message = "El rol es obligatorio")
        String role
) {
}
