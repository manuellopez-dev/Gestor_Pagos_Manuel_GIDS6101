package com.proyecto.servicios.service;

import com.proyecto.servicios.model.transaccion.ConciliacionRequest;
import com.proyecto.servicios.model.transaccion.TransaccionRequest;
import com.proyecto.servicios.model.transaccion.TransaccionResponse;
import com.proyecto.servicios.model.transaccion.TransaccionVentaRequest;

public interface TransaccionService {

    TransaccionResponse procesarTransaccion(TransaccionRequest request);

    TransaccionResponse procesarVenta(TransaccionVentaRequest request);

    TransaccionResponse conciliarTransaccion(ConciliacionRequest request);

    TransaccionResponse consultarTransaccion(String idempotenciaKey);
}
