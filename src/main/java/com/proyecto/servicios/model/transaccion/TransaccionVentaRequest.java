package com.proyecto.servicios.model.transaccion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class TransaccionVentaRequest {

    @NotBlank(message = "El número de cuenta (NumCuenta) es obligatorio")
    @Size(min = 3, max = 50, message = "El número de cuenta debe tener entre 3 y 50 caracteres")
    private String numCuenta;

    @NotBlank(message = "El identificador del servicio (IdServicio) es obligatorio")
    private String idServicio;

    @NotBlank(message = "La clave o nivel de permiso (idpermiso) es obligatoria")
    private String idpermiso;

    @NotNull(message = "El precio o importe (Precio) es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    @Digits(integer = 10, fraction = 2, message = "El precio debe tener como máximo 2 decimales")
    private BigDecimal precio;

    @NotBlank(message = "El identificador del distribuidor (Distribuidor) es obligatorio")
    private String distribuidor;

    @NotBlank(message = "El teléfono o referencia (Telefono / Referencia) es obligatorio")
    @Size(min = 3, max = 100, message = "El teléfono o referencia debe tener entre 3 y 100 caracteres")
    private String telefono;

    @NotBlank(message = "La bandera de control (Bandera) es obligatoria")
    private String bandera;

    @NotBlank(message = "La credencial de seguridad (Pin) es obligatoria")
    @Size(min = 4, max = 20, message = "El PIN debe tener entre 4 y 20 caracteres")
    private String pin;

    private String idProducto;

    private String upe;

    private String unidad;

    private String idempotenciaKey;
}
