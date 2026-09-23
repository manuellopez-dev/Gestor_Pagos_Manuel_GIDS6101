package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.producto.Producto;

import java.util.List;

public interface GetProductoXmlService {

    List<Producto> parsear(String xml);
}