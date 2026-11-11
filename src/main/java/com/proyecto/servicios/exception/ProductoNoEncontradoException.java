package com.proyecto.servicios.exception;

public class ProductoNoEncontradoException extends RuntimeException {

    public ProductoNoEncontradoException(String idProducto) {
        super("El producto con ID '" + idProducto + "' no existe en el catálogo");
    }
}
