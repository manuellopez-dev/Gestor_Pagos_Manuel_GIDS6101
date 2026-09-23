package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.IntegracionException;
import com.proyecto.servicios.exception.TipoErrorIntegracion;
import com.proyecto.servicios.model.GenericResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IntegracionException.class)
    public ResponseEntity<GenericResponse> handleIntegracionException(IntegracionException exception) {
        HttpStatus httpStatus = mapearHttpStatus(exception.getTipoError());
        GenericResponse response = new GenericResponse();
        response.setCodigo(httpStatus.value());
        response.setMensaje(exception.getMessage());
        return new ResponseEntity<>(response, httpStatus);
    }

    private HttpStatus mapearHttpStatus(TipoErrorIntegracion tipoError) {
        return switch (tipoError) {
            case ERROR_AUTENTICACION -> HttpStatus.UNAUTHORIZED;
            case TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case ERROR_COMUNICACION -> HttpStatus.BAD_GATEWAY;
            case RESPUESTA_NO_EXITOSA -> HttpStatus.BAD_GATEWAY;
        };
    }
}