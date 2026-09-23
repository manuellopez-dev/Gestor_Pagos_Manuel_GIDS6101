package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.producto.Producto;
import com.proyecto.servicios.exception.IntegracionException;
import com.proyecto.servicios.exception.TipoErrorIntegracion;
import com.proyecto.servicios.mapper.ProductoMapperImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.proyecto.servicios.support.IntegracionTestSupport.XML_PRODUCTOS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetProductoXmlServiceImplTest {

    private GetProductoXmlServiceImpl getProductoXmlService;

    @BeforeEach
    void setUp() {
        getProductoXmlService = new GetProductoXmlServiceImpl(new ProductoMapperImpl());
    }

    @Test
    void parsear_cuandoElXmlEsValido_devuelveLosProductosDelXml() {
        List<Producto> productos = getProductoXmlService.parsear(XML_PRODUCTOS);

        assertThat(productos).hasSize(2);
        assertThat(productos.get(0).getIdProducto()).isEqualTo("185");
        assertThat(productos.get(0).getNombre()).isEqualTo("Agua Cancun (Mun. de Benito Juarez)");
        assertThat(productos.get(0).getPrecio()).isEqualTo("100.50");
        assertThat(productos.get(0).getServicio()).isEqualTo("AGUAKAN (Cancun)");
        assertThat(productos.get(1).getIdProducto()).isEqualTo("656");
        assertThat(productos.get(1).getPrecio()).isEqualTo("200.75");
    }

    @Test
    void parsear_cuandoElXmlEsVacio_lanzaRespuestaNoExitosa() {
        assertThatThrownBy(() -> getProductoXmlService.parsear(" "))
                .isInstanceOfSatisfying(IntegracionException.class, exception ->
                        assertThat(exception.getTipoError()).isEqualTo(TipoErrorIntegracion.RESPUESTA_NO_EXITOSA));
    }

    @Test
    void parsear_cuandoElXmlEsInvalido_lanzaRespuestaNoExitosa() {
        assertThatThrownBy(() -> getProductoXmlService.parsear("esto no es xml"))
                .isInstanceOfSatisfying(IntegracionException.class, exception ->
                        assertThat(exception.getTipoError()).isEqualTo(TipoErrorIntegracion.RESPUESTA_NO_EXITOSA));
    }
}