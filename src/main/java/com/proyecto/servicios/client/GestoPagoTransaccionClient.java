package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "gestoPagoTransaccion", url = "${gestopago.productos.url}", configuration = ProductoFeignConfig.class)
public interface GestoPagoTransaccionClient {

    @PostMapping(value = "/sistema/service/sendTx.do", produces = MediaType.APPLICATION_XML_VALUE)
    String sendTx(
            @RequestParam("idServicio") String idServicio,
            @RequestParam("idProducto") String idProducto,
            @RequestParam("monto") String monto,
            @RequestParam("referencia") String referencia,
            @RequestParam(value = "idTransaccion", required = false) String idTransaccion,
            @RequestParam(value = "digitoVerificador", required = false) String digitoVerificador
    );

    @GetMapping(value = "/sistema/service/queryTx.do", produces = MediaType.APPLICATION_XML_VALUE)
    String queryTx(
            @RequestParam("idTransaccion") String idTransaccion
    );
}
