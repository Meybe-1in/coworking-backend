package com.coworking.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Respuesta estándar de la API")
public class ApiResponseDto<T> {

    @Schema(description = "Indica si la operación fue exitosa")
    private boolean success;

    @Schema(description = "Mensaje descriptivo")
    private String message;

    @Schema(description = "Datos de la respuesta")
    private T data;

    // Metodos helper
    public static <T> ApiResponseDto<T> success(String message, T data){
        return new ApiResponseDto<>(true, message, data);
    }

    public static <T> ApiResponseDto<T> success(String message){
        return new ApiResponseDto<>(true, message, null);
    }

    public static <T> ApiResponseDto<T> error(String message){
        return new ApiResponseDto<>(false, message, null);
    }
}