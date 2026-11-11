package com.proyecto.servicios.exception;

public class CuentaInactivaException extends RuntimeException {

    public CuentaInactivaException(String mensaje) {
        super(mensaje);
    }
}
