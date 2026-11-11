package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.transaccion.Transaccion;
import com.proyecto.servicios.model.xml.RespuestaTransaccionXml;

import java.math.BigDecimal;

public interface TransaccionOperacionService {

    Transaccion reservarSaldoYCrearTransaccion(String idempotenciaKey, Long cuentaId, String idServicio,
                                              String idProducto, String nombreProducto, BigDecimal monto,
                                              String referencia, String digitoVerificador);

    Transaccion reservarSaldoVenta(String idempotenciaKey, String numCuenta, String pin,
                                   String idServicio, String idProducto, String nombreProducto,
                                   BigDecimal monto, String referencia, String distribuidor,
                                   String idPermiso, String bandera, String upe,
                                   String unidad, String firmaDigital, String payloadCifrado);

    Transaccion liquidarTransaccionExitosa(Long transaccionId, RespuestaTransaccionXml respuestaXml);

    Transaccion revertirTransaccion(Long transaccionId, String codigoError, String mensajeError, boolean esReversa);

    Transaccion marcarPorConciliar(Long transaccionId, String detalle);
}
