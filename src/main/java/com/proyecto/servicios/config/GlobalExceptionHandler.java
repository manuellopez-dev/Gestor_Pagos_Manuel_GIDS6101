package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.CorreoDuplicadoException;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.IntegracionException;
import com.proyecto.servicios.exception.MontoInvalidoException;
import com.proyecto.servicios.exception.ProductoNoEncontradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.exception.TipoErrorIntegracion;
import com.proyecto.servicios.exception.TransaccionDuplicadaException;
import com.proyecto.servicios.exception.TransaccionEnProcesoException;
import com.proyecto.servicios.exception.TransaccionIndeterminadaException;
import com.proyecto.servicios.exception.TransaccionRechazadaException;
import com.proyecto.servicios.exception.UsernameDuplicadoException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.exception.ValidacionTransaccionException;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> errores = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.add(error.getField() + ": " + error.getDefaultMessage());
        }
        log.warn("Error de validacion en la solicitud: {}", errores);
        ApiResponse<Void> response = ApiResponse.fallo(
                HttpStatus.BAD_REQUEST.value(),
                "Error en las validaciones de entrada",
                errores
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        log.warn("Usuario no encontrado: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.fallo(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CorreoDuplicadoException.class)
    public ResponseEntity<ApiResponse<Void>> handleCorreoDuplicado(CorreoDuplicadoException ex) {
        log.warn("Conflicto de correo duplicado: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.fallo(
                HttpStatus.CONFLICT.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UsernameDuplicadoException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsernameDuplicado(UsernameDuplicadoException ex) {
        log.warn("Conflicto de nombre de usuario duplicado: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.fallo(
                HttpStatus.CONFLICT.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiResponse<Void>> handleReglaNegocio(ReglaNegocioException ex) {
        log.warn("Violacion de regla de negocio: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.fallo(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<ApiResponse<Void>> handleCuentaNoEncontrada(CuentaNoEncontradaException ex) {
        log.warn("Cuenta no encontrada: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.fallo(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ProductoNoEncontradoException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductoNoEncontrado(ProductoNoEncontradoException ex) {
        log.warn("Producto no encontrado en catálogo: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.fallo(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ValidacionTransaccionException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidacionTransaccion(ValidacionTransaccionException ex) {
        log.warn("Validación de transacción fallida: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.fallo(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MontoInvalidoException.class)
    public ResponseEntity<ApiResponse<Void>> handleMontoInvalido(MontoInvalidoException ex) {
        log.warn("Monto inválido para el producto: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.fallo(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SaldoInsuficienteException.class)
    public ResponseEntity<ApiResponse<Void>> handleSaldoInsuficiente(SaldoInsuficienteException ex) {
        log.warn("Saldo insuficiente: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.fallo(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(CuentaInactivaException.class)
    public ResponseEntity<ApiResponse<Void>> handleCuentaInactiva(CuentaInactivaException ex) {
        log.warn("Cuenta inactiva: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.fallo(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(TransaccionDuplicadaException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransaccionDuplicada(TransaccionDuplicadaException ex) {
        log.warn("Transacción duplicada: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.fallo(
                HttpStatus.CONFLICT.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TransaccionEnProcesoException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransaccionEnProceso(TransaccionEnProcesoException ex) {
        log.warn("Transacción en proceso: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.fallo(
                HttpStatus.CONFLICT.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TransaccionRechazadaException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransaccionRechazada(TransaccionRechazadaException ex) {
        log.warn("Transacción rechazada por proveedor: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.fallo(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(TransaccionIndeterminadaException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransaccionIndeterminada(TransaccionIndeterminadaException ex) {
        log.error("Transacción indeterminada por timeout con proveedor: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.fallo(
                HttpStatus.GATEWAY_TIMEOUT.value(),
                ex.getMessage()
        );
        return new ResponseEntity<>(response, HttpStatus.GATEWAY_TIMEOUT);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Cuerpo de la solicitud invalido o malformado: {}", ex.getMessage());
        ApiResponse<Void> response = ApiResponse.fallo(
                HttpStatus.BAD_REQUEST.value(),
                "El cuerpo de la solicitud no es valido o contiene un formato incorrecto"
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IntegracionException.class)
    public ResponseEntity<GenericResponse> handleIntegracionException(IntegracionException exception) {
        HttpStatus httpStatus = mapearHttpStatus(exception.getTipoError());
        GenericResponse response = new GenericResponse();
        response.setCodigo(httpStatus.value());
        response.setMensaje(exception.getMessage());
        return new ResponseEntity<>(response, httpStatus);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Error no controlado en la aplicacion", ex);
        // Sin exponer trazas tecnicas ni SQL
        ApiResponse<Void> response = ApiResponse.fallo(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Ha ocurrido un error interno en el servidor al procesar la solicitud"
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
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
