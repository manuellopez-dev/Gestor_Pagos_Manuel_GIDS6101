package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.exception.IntegracionException;
import com.proyecto.servicios.model.xml.RespuestaTransaccionXml;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetTransaccionXmlServiceImplTest {

    private GetTransaccionXmlServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GetTransaccionXmlServiceImpl();
    }

    @Test
    void parsear_cuandoXmlEsValidoConAtributos_retornaObjetoParsado() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <RESPONSE>
                  <MENSAJE>
                    <CODIGO>01</CODIGO>
                    <TEXTO>Operacion realizada con exito</TEXTO>
                  </MENSAJE>
                  <TRANSACCION autorizacion="123456" folio="GP-889900" idTransaccion="9988"/>
                </RESPONSE>
                """;

        RespuestaTransaccionXml respuesta = service.parsear(xml);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getMensaje()).isNotNull();
        assertThat(respuesta.getMensaje().getCodigo()).isEqualTo("01");
        assertThat(respuesta.getMensaje().getTexto()).isEqualTo("Operacion realizada con exito");
        assertThat(respuesta.getTransaccion()).isNotNull();
        assertThat(respuesta.getTransaccion().getAutorizacion()).isEqualTo("123456");
        assertThat(respuesta.getTransaccion().getFolio()).isEqualTo("GP-889900");
    }

    @Test
    void parsear_cuandoXmlEsValidoConElementosHijos_retornaObjetoParsado() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <RESPONSE>
                  <MENSAJE>
                    <CODIGO>00</CODIGO>
                    <TEXTO>Saldo insuficiente distribuidor</TEXTO>
                  </MENSAJE>
                  <TRANSACCION>
                    <AUTORIZACION>654321</AUTORIZACION>
                    <FOLIO>GP-112233</FOLIO>
                  </TRANSACCION>
                </RESPONSE>
                """;

        RespuestaTransaccionXml respuesta = service.parsear(xml);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getMensaje().getCodigo()).isEqualTo("00");
        assertThat(respuesta.getTransaccion().getAutorizacion()).isEqualTo("654321");
        assertThat(respuesta.getTransaccion().getFolio()).isEqualTo("GP-112233");
    }

    @Test
    void parsear_cuandoXmlEsVacio_lanzaIntegracionException() {
        assertThatThrownBy(() -> service.parsear(""))
                .isInstanceOf(IntegracionException.class)
                .hasMessageContaining("no devolvió respuesta XML");
    }

    @Test
    void parsear_cuandoXmlEsInvalido_lanzaIntegracionException() {
        assertThatThrownBy(() -> service.parsear("esto no es un xml"))
                .isInstanceOf(IntegracionException.class)
                .hasMessageContaining("No fue posible procesar la respuesta XML");
    }
}
