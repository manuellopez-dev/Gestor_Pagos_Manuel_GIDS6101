package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductoDTO {

    private String id;

    private String idProducto;

    private String idServicio;

    private String servicio;

    private String idCatTipoServicio;

    private String tipoFront;

    private String nombre;

    private String precio;

    private String tipoReferencia;

    private String legend;
}