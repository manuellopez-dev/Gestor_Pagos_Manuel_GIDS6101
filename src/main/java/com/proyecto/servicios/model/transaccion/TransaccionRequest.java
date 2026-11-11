package com.proyecto.servicios.model.transaccion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionRequest {

    @NotBlank(message = "La clave de idempotencia es obligatoria para garantizar la integridad financiera")
    @Size(min = 8, max = 100, message = "La clave de idempotencia debe tener entre 8 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "La clave de idempotencia contiene caracteres no permitidos")
    private String idempotenciaKey;

    @NotNull(message = "El ID de la cuenta es obligatorio")
    private Long cuentaId;

    @NotBlank(message = "El ID del servicio es obligatorio")
    @Size(max = 50, message = "El ID del servicio no debe exceder 50 caracteres")
    private String idServicio;

    @NotBlank(message = "El ID del producto es obligatorio")
    @Size(max = 50, message = "El ID del producto no debe exceder 50 caracteres")
    private String idProducto;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto de la transacción debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "El monto debe tener como máximo 2 decimales")
    private BigDecimal monto;

    @NotBlank(message = "La referencia de la transacción es obligatoria")
    @Size(min = 3, max = 100, message = "La referencia debe tener entre 3 y 100 caracteres")
    private String referencia;

    @Size(max = 10, message = "El dígito verificador no puede exceder 10 caracteres")
    private String digitoVerificador;
}
