package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductoClient;
import com.proyecto.servicios.entity.producto.Producto;
import com.proyecto.servicios.exception.IntegracionErrorMapper;
import com.proyecto.servicios.exception.IntegracionException;
import com.proyecto.servicios.exception.TipoErrorIntegracion;
import com.proyecto.servicios.mapper.ProductoMapper;
import com.proyecto.servicios.model.gestopago.ProductoDTO;
import com.proyecto.servicios.model.gestopago.ProductoListResponse;
import com.proyecto.servicios.service.GetProductoXmlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;

import static com.proyecto.servicios.support.IntegracionTestSupport.XML_PRODUCTOS;
import static com.proyecto.servicios.support.IntegracionTestSupport.errorReintentable;
import static com.proyecto.servicios.support.IntegracionTestSupport.feignError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock
    private GestoPagoProductoClient productoClient;

    @Mock
    private GetProductoXmlService getProductoXmlService;

    @Mock
    private ProductoMapper productoMapper;

    private ProductoServiceImpl productoService;

    @BeforeEach
    void setUp() {
        productoService = new ProductoServiceImpl(productoClient, getProductoXmlService,
                productoMapper, new IntegracionErrorMapper());
    }

    @Test
    void obtenerProductos_cuandoElServicioRespondeExitosamente_retornaLaListaParsada() {
        Producto p1 = new Producto();
        p1.setIdProducto("P001");
        p1.setNombre("Producto Uno");
        ProductoDTO dto1 = new ProductoDTO();
        dto1.setId("P001");
        dto1.setNombre("Producto Uno");
        when(productoClient.getProductList()).thenReturn(XML_PRODUCTOS);
        when(getProductoXmlService.parsear(XML_PRODUCTOS)).thenReturn(List.of(p1));
        when(productoMapper.toDtos(List.of(p1))).thenReturn(List.of(dto1));

        ProductoListResponse response = productoService.obtenerProductos();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getProductos()).containsExactly(dto1);
        verify(productoClient).getProductList();
    }

    @Test
    void obtenerProductos_cuandoOcurreErrorDeAutenticacion_lanzaErrorAutenticacion() {
        when(productoClient.getProductList()).thenThrow(feignError(401, "Unauthorized"));

        assertThatThrownBy(() -> productoService.obtenerProductos())
                .isInstanceOfSatisfying(IntegracionException.class, exception ->
                        assertThat(exception.getTipoError()).isEqualTo(TipoErrorIntegracion.ERROR_AUTENTICACION));
    }

    @Test
    void obtenerProductos_cuandoOcurreErrorForbidden_lanzaErrorAutenticacion() {
        when(productoClient.getProductList()).thenThrow(feignError(403, "Forbidden"));

        assertThatThrownBy(() -> productoService.obtenerProductos())
                .isInstanceOfSatisfying(IntegracionException.class, exception ->
                        assertThat(exception.getTipoError()).isEqualTo(TipoErrorIntegracion.ERROR_AUTENTICACION));
    }

    @Test
    void obtenerProductos_cuandoOcurreTimeoutEnLaLectura_lanzaTimeout() {
        when(productoClient.getProductList())
                .thenThrow(errorReintentable("Read timed out", new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> productoService.obtenerProductos())
                .isInstanceOfSatisfying(IntegracionException.class, exception ->
                        assertThat(exception.getTipoError()).isEqualTo(TipoErrorIntegracion.TIMEOUT));
    }

    @Test
    void obtenerProductos_cuandoOcurreErrorDeConexion_lanzaErrorComunicacion() {
        when(productoClient.getProductList())
                .thenThrow(errorReintentable("Connection refused", new ConnectException("Connection refused")));

        assertThatThrownBy(() -> productoService.obtenerProductos())
                .isInstanceOfSatisfying(IntegracionException.class, exception ->
                        assertThat(exception.getTipoError()).isEqualTo(TipoErrorIntegracion.ERROR_COMUNICACION));
    }

    @Test
    void obtenerProductos_cuandoElServicioDevuelveErrorHttp_lanzaRespuestaNoExitosa() {
        when(productoClient.getProductList()).thenThrow(feignError(500, "Internal Server Error"));

        assertThatThrownBy(() -> productoService.obtenerProductos())
                .isInstanceOfSatisfying(IntegracionException.class, exception ->
                        assertThat(exception.getTipoError()).isEqualTo(TipoErrorIntegracion.RESPUESTA_NO_EXITOSA));
    }
}