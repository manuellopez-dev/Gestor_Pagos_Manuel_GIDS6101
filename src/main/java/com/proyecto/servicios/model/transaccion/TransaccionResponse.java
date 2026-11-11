package com.proyecto.servicios.model.transaccion;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.proyecto.servicios.entity.transaccion.EstadoTransaccion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionResponse {

    private Long id;
    private String idempotenciaKey;
    private Long cuentaId;
    private String idServicio;
    private String idProducto;
    private String nombreProducto;
    private BigDecimal monto;
    private String referencia;
    private EstadoTransaccion estado;
    private String folioProveedor;
    private String autorizacionProveedor;
    private String codigoRespuestaProveedor;
    private String mensajeProveedor;
    private String distribuidor;
    private String idPermiso;
    private String bandera;
    private String upe;
    private String unidad;
    private String firmaDigital;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaCreacion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaActualizacion;
}
