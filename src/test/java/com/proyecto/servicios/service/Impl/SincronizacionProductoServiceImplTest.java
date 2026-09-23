package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductoClient;
import com.proyecto.servicios.entity.producto.Producto;
import com.proyecto.servicios.exception.IntegracionErrorMapper;
import com.proyecto.servicios.exception.IntegracionException;
import com.proyecto.servicios.exception.TipoErrorIntegracion;
import com.proyecto.servicios.repositorys.producto.ProductoRepository;
import com.proyecto.servicios.service.GetProductoXmlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;

import static com.proyecto.servicios.support.IntegracionTestSupport.XML_PRODUCTOS;
import static com.proyecto.servicios.support.IntegracionTestSupport.errorReintentable;
import static com.proyecto.servicios.support.IntegracionTestSupport.feignError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SincronizacionProductoServiceImplTest {

    @Mock
    private GestoPagoProductoClient productoClient;

    @Mock
    private GetProductoXmlService getProductoXmlService;

    @Mock
    private ProductoRepository productoRepository;

    private SincronizacionProductoServiceImpl sincronizacionProductoService;

    @BeforeEach
    void setUp() {
        sincronizacionProductoService = new SincronizacionProductoServiceImpl(productoClient,
                getProductoXmlService, productoRepository, new IntegracionErrorMapper());
    }

    @Test
    void sincronizarProductos_cuandoElServicioResponde_guardaProductosNuevos() {
        Producto p1 = new Producto();
        p1.setIdProducto("P001");
        when(productoClient.getProductList()).thenReturn(XML_PRODUCTOS);
        when(getProductoXmlService.parsear(XML_PRODUCTOS)).thenReturn(List.of(p1));
        when(productoRepository.findByIdProducto("P001")).thenReturn(Optional.empty());

        int count = sincronizacionProductoService.sincronizarProductos();

        assertThat(count).isEqualTo(1);
        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(captor.capture());
        assertThat(captor.getValue().getFechaSincronizacion()).isNotNull();
    }

    @Test
    void sincronizarProductos_cuandoElProductoYaExiste_mantieneElIdentificador() {
        Producto p1 = new Producto();
        p1.setIdProducto("P001");
        Producto existente = new Producto();
        existente.setId("abc123");
        existente.setIdProducto("P001");
        when(productoClient.getProductList()).thenReturn(XML_PRODUCTOS);
        when(getProductoXmlService.parsear(XML_PRODUCTOS)).thenReturn(List.of(p1));
        when(productoRepository.findByIdProducto("P001")).thenReturn(Optional.of(existente));

        sincronizacionProductoService.sincronizarProductos();

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("abc123");
    }

    @Test
    void sincronizarProductos_cuandoOcurreErrorDeAutenticacion_lanzaErrorAutenticacion() {
        when(productoClient.getProductList()).thenThrow(feignError(401, "Unauthorized"));

        assertThatThrownBy(() -> sincronizacionProductoService.sincronizarProductos())
                .isInstanceOfSatisfying(IntegracionException.class, exception ->
                        assertThat(exception.getTipoError()).isEqualTo(TipoErrorIntegracion.ERROR_AUTENTICACION));
    }

    @Test
    void sincronizarProductos_cuandoOcurreTimeout_lanzaTimeout() {
        when(productoClient.getProductList())
                .thenThrow(errorReintentable("Read timed out", new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> sincronizacionProductoService.sincronizarProductos())
                .isInstanceOfSatisfying(IntegracionException.class, exception ->
                        assertThat(exception.getTipoError()).isEqualTo(TipoErrorIntegracion.TIMEOUT));
    }

    @Test
    void sincronizarProductos_cuandoOcurreErrorDeConexion_lanzaErrorComunicacion() {
        when(productoClient.getProductList())
                .thenThrow(errorReintentable("Connection refused", new ConnectException("Connection refused")));

        assertThatThrownBy(() -> sincronizacionProductoService.sincronizarProductos())
                .isInstanceOfSatisfying(IntegracionException.class, exception ->
                        assertThat(exception.getTipoError()).isEqualTo(TipoErrorIntegracion.ERROR_COMUNICACION));
    }
}