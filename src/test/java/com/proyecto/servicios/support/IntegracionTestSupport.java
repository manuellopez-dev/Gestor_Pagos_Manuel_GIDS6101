package com.proyecto.servicios.support;

import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

@SuppressWarnings("deprecation")
public final class IntegracionTestSupport {

    public static final String URL_SERVICIO = "http://servicio.test/sistema/service/getProductList.do";

    public static final String XML_PRODUCTOS =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<RESPONSE>"
            + "  <MENSAJE><CODIGO>01</CODIGO><TEXTO>Operacion realizada con exito</TEXTO></MENSAJE>"
            + "  <PRODUCTOS>"
            + "    <producto servicio='AGUAKAN (Cancun)' producto='Agua Cancun (Mun. de Benito Juarez)'"
            + " idServicio='56' idProducto='185' idCatTipoServicio='15' tipoFront='2'"
            + " hasDigitoVerificador='false' precio='100.50' showAyuda='false' tipoReferencia='c'><legend><![CDATA[Por cualquier duda contacta al 073]]></legend></producto>"
            + "    <producto servicio='Aguas de Saltillo' producto='Aguas de Saltillo'"
            + " idServicio='187' idProducto='656' idCatTipoServicio='15' tipoFront='2'"
            + " hasDigitoVerificador='false' precio='200.75' showAyuda='false' tipoReferencia='c'><legend><![CDATA[www.aguasdesaltillo.com]]></legend></producto>"
            + "  </PRODUCTOS>"
            + "</RESPONSE>";

    private IntegracionTestSupport() {
    }

    public static FeignException feignError(int status, String reason) {
        return FeignException.errorStatus("getProductList",
                Response.builder()
                        .status(status)
                        .reason(reason)
                        .request(request())
                        .headers(Collections.emptyMap())
                        .body(new byte[0])
                        .build());
    }

    public static RetryableException errorReintentable(String mensaje, Throwable causa) {
        RetryableException exception = new RetryableException(503, mensaje, Request.HttpMethod.GET, -1L, request());
        exception.initCause(causa);
        return exception;
    }

    public static Request request() {
        return Request.create(Request.HttpMethod.GET, URL_SERVICIO, Collections.emptyMap(), null, StandardCharsets.UTF_8);
    }
}