package com.proyecto.servicios.exception;

public class CuentaNoEncontradaException extends RuntimeException {

    public CuentaNoEncontradaException(Long id) {
        super("No se encontró la cuenta con ID " + id);
    }

    public CuentaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
