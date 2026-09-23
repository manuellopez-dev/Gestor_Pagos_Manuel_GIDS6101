package com.proyecto.servicios.exception;

import feign.FeignException;
import feign.RetryableException;
import org.springframework.stereotype.Component;

import java.net.SocketTimeoutException;

@Component
public class IntegracionErrorMapper {

    public IntegracionException mapear(FeignException exception) {
        if (exception instanceof RetryableException) {
            if (exception.getCause() instanceof SocketTimeoutException) {
                return new IntegracionException(TipoErrorIntegracion.TIMEOUT,
                        "Se agoto el tiempo de espera al invocar el servicio externo", exception);
            }
            return new IntegracionException(TipoErrorIntegracion.ERROR_COMUNICACION,
                    "Error de comunicacion con el servicio externo", exception);
        }
        if (exception instanceof FeignException.Unauthorized
                || exception instanceof FeignException.Forbidden) {
            return new IntegracionException(TipoErrorIntegracion.ERROR_AUTENTICACION,
                    "Error de autenticacion con el servicio externo", exception);
        }
        return new IntegracionException(TipoErrorIntegracion.RESPUESTA_NO_EXITOSA,
                "El servicio externo devolvio una respuesta no exitosa (HTTP " + exception.status() + ")", exception);
    }
}