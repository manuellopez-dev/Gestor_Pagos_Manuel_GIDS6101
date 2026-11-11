package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.common.ApiResponse;
import com.proyecto.servicios.model.transaccion.ConciliacionRequest;
import com.proyecto.servicios.model.transaccion.TransaccionRequest;
import com.proyecto.servicios.model.transaccion.TransaccionResponse;
import com.proyecto.servicios.model.transaccion.TransaccionVentaRequest;
import com.proyecto.servicios.service.TransaccionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/transacciones", produces = MediaType.APPLICATION_JSON_VALUE)
public class TransaccionController {

    private final TransaccionService transaccionService;

    public TransaccionController(TransaccionService transaccionService) {
        this.transaccionService = transaccionService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<TransaccionResponse>> enviarTransaccion(
            @Valid @RequestBody TransaccionRequest request) {
        TransaccionResponse response = transaccionService.procesarTransaccion(request);
        return new ResponseEntity<>(
                ApiResponse.exitoso(HttpStatus.OK.value(), "Transacción procesada exitosamente", response),
                HttpStatus.OK
        );
    }

    @PostMapping(value = "/venta", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<TransaccionResponse>> procesarVenta(
            @Valid @RequestBody com.proyecto.servicios.model.transaccion.TransaccionVentaRequest request) {
        TransaccionResponse response = transaccionService.procesarVenta(request);
        return new ResponseEntity<>(
                ApiResponse.exitoso(HttpStatus.OK.value(), "Venta procesada exitosamente (Código 01 / Saldo descontado)", response),
                HttpStatus.OK
        );
    }

    @PostMapping(value = "/conciliar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<TransaccionResponse>> conciliarTransaccion(
            @Valid @RequestBody ConciliacionRequest request) {
        TransaccionResponse response = transaccionService.conciliarTransaccion(request);
        return ResponseEntity.ok(ApiResponse.exitoso("Conciliación ejecutada exitosamente", response));
    }

    @PostMapping(value = "/consultar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<TransaccionResponse>> consultarTransaccion(
            @Valid @RequestBody ConciliacionRequest request) {
        TransaccionResponse response = transaccionService.consultarTransaccion(request.getIdempotenciaKey());
        return ResponseEntity.ok(ApiResponse.exitoso("Transacción consultada exitosamente", response));
    }
}
