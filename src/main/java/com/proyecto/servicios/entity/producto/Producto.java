package com.proyecto.servicios.entity.producto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "productos")
@Getter
@Setter
public class Producto {

    @Id
    private String id;

    private String idProducto;

    private String idServicio;

    private String servicio;

    private String idCatTipoServicio;

    private String tipoFront;

    private String nombre;

    private String precio;

    private String hasDigitoVerificador;

    private String showAyuda;

    private String tipoReferencia;

    private String legend;

    private LocalDateTime fechaSincronizacion;
}