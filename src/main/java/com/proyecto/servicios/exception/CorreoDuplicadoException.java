package com.proyecto.servicios.exception;

public class CorreoDuplicadoException extends RuntimeException {

    public CorreoDuplicadoException(String email) {
        super("El correo electrónico '" + email + "' ya se encuentra registrado por otro usuario");
    }
}
