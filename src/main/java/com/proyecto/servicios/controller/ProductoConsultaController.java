package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ConsultaProductoResponse;
import com.proyecto.servicios.service.ConsultaProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductoConsultaController {

    @Autowired
    private ConsultaProductoService consultaProductoService;

    @GetMapping(value = "/productos/consulta", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConsultaProductoResponse> consultarProductos() {
        ConsultaProductoResponse response = new ConsultaProductoResponse();
        response.setCodigo(200);
        response.setMensaje("OK");
        response.setProductos(consultaProductoService.consultarProductos());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}