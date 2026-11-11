package com.proyecto.servicios.exception;

public class TransaccionDuplicadaException extends RuntimeException {

    public TransaccionDuplicadaException(String idempotenciaKey) {
        super("La clave de idempotencia '" + idempotenciaKey + "' ya fue procesada previamente");
    }
}
