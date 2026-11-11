package com.proyecto.servicios.exception;

public class TransaccionIndeterminadaException extends RuntimeException {

    private final String idempotenciaKey;

    public TransaccionIndeterminadaException(String idempotenciaKey, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.idempotenciaKey = idempotenciaKey;
    }

    public TransaccionIndeterminadaException(String idempotenciaKey, String mensaje) {
        super(mensaje);
        this.idempotenciaKey = idempotenciaKey;
    }

    public String getIdempotenciaKey() {
        return idempotenciaKey;
    }
}
