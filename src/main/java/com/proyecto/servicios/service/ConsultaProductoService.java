package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.ProductoDTO;

import java.util.List;

public interface ConsultaProductoService {

    List<ProductoDTO> consultarProductos();
}