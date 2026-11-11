package com.proyecto.servicios.model.transaccion;

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
public class PayloadTransaccion {

    private String idCuenta;

    private String idServicio;

    private String telefonoReferencia;

    private String horaLocal;

    private BigDecimal montoPago;

    private String upe;

    private String unidad;

    public String toCanonicalString() {
        return String.format("idCuenta=%s|idServicio=%s|telefonoReferencia=%s|horaLocal=%s|montoPago=%s|upe=%s|unidad=%s",
                idCuenta, idServicio, telefonoReferencia, horaLocal,
                montoPago != null ? montoPago.toPlainString() : "0.00",
                upe, unidad);
    }
}
