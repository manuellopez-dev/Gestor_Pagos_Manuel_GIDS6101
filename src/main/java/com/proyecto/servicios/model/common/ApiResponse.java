package com.proyecto.servicios.model.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean exito;
    private int codigo;
    private String mensaje;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    private T datos;
    private List<String> errores;

    public static <T> ApiResponse<T> exitoso(int codigo, String mensaje, T datos) {
        return ApiResponse.<T>builder()
                .exito(true)
                .codigo(codigo)
                .mensaje(mensaje)
                .timestamp(LocalDateTime.now())
                .datos(datos)
                .build();
    }

    public static <T> ApiResponse<T> exitoso(String mensaje, T datos) {
        return exitoso(200, mensaje, datos);
    }

    public static <T> ApiResponse<T> fallo(int codigo, String mensaje, List<String> errores) {
        return ApiResponse.<T>builder()
                .exito(false)
                .codigo(codigo)
                .mensaje(mensaje)
                .timestamp(LocalDateTime.now())
                .errores(errores)
                .build();
    }

    public static <T> ApiResponse<T> fallo(int codigo, String mensaje) {
        return fallo(codigo, mensaje, null);
    }
}
