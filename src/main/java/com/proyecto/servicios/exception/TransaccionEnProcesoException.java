package com.proyecto.servicios.exception;

public class TransaccionEnProcesoException extends RuntimeException {

    public TransaccionEnProcesoException(String idempotenciaKey) {
        super("La transacción con clave '" + idempotenciaKey + "' se encuentra actualmente en proceso o pendiente de conciliación");
    }
}
