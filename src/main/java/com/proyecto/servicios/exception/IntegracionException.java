package com.proyecto.servicios.exception;

import lombok.Getter;

@Getter
public class IntegracionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final TipoErrorIntegracion tipoError;

    public IntegracionException(TipoErrorIntegracion tipoError, String message) {
        super(message);
        this.tipoError = tipoError;
    }

    public IntegracionException(TipoErrorIntegracion tipoError, String message, Throwable cause) {
        super(message, cause);
        this.tipoError = tipoError;
    }
}