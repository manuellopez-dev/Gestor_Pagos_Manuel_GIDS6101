package com.proyecto.servicios.model;

import com.proyecto.servicios.model.gestopago.ProductoDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ConsultaProductoResponse extends GenericResponse {

    private List<ProductoDTO> productos;
}