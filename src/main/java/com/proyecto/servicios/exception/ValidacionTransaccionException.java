package com.proyecto.servicios.exception;

public class ValidacionTransaccionException extends RuntimeException {

    public ValidacionTransaccionException(String mensaje) {
        super(mensaje);
    }
}
