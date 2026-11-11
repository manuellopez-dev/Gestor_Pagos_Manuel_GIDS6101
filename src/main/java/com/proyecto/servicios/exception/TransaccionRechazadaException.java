package com.proyecto.servicios.exception;

public class TransaccionRechazadaException extends RuntimeException {

    private final String codigoProveedor;

    public TransaccionRechazadaException(String codigoProveedor, String mensajeProveedor) {
        super("Transacción rechazada por GestoPago [Código: " + codigoProveedor + "]: " + mensajeProveedor);
        this.codigoProveedor = codigoProveedor;
    }

    public String getCodigoProveedor() {
        return codigoProveedor;
    }
}
