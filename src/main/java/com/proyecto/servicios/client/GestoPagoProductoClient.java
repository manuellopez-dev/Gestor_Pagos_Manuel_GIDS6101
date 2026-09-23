package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "gestoPagoProducto", url = "${gestopago.productos.url}", configuration = ProductoFeignConfig.class)
public interface GestoPagoProductoClient {

    @GetMapping(value = "/sistema/service/getProductList.do", produces = MediaType.APPLICATION_XML_VALUE)
    String getProductList();
}