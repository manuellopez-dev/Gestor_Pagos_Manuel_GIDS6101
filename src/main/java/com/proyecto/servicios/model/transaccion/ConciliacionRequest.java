package com.proyecto.servicios.model.transaccion;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConciliacionRequest {

    @NotBlank(message = "La clave de idempotencia es obligatoria para la conciliación")
    private String idempotenciaKey;
}
