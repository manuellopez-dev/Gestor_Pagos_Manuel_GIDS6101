package com.proyecto.servicios.exception;

public class UsernameDuplicadoException extends RuntimeException {

    public UsernameDuplicadoException(String username) {
        super("El nombre de usuario '" + username + "' ya se encuentra en uso");
    }
}
