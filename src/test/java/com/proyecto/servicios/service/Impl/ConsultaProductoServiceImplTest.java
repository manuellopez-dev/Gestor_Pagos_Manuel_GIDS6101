package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.producto.Producto;
import com.proyecto.servicios.mapper.ProductoMapper;
import com.proyecto.servicios.model.gestopago.ProductoDTO;
import com.proyecto.servicios.repositorys.producto.ProductoRepository;
import com.proyecto.servicios.service.SincronizacionProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ConsultaProductoServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private SincronizacionProductoService sincronizacionProductoService;

    @Mock
    private ProductoMapper productoMapper;

    private ConsultaProductoServiceImpl consultaProductoService;

    @BeforeEach
    void setUp() {
        consultaProductoService = new ConsultaProductoServiceImpl(productoRepository,
                sincronizacionProductoService, productoMapper, 24);
    }

    @Test
    void consultarProductos_cuandoNoHayDatos_sincronizaYDisponeLaInformacion() {
        Producto p1 = new Producto();
        p1.setIdProducto("P001");
        p1.setFechaSincronizacion(LocalDateTime.now());
        ProductoDTO dto = new ProductoDTO();
        dto.setId("P001");
        when(productoRepository.findAll()).thenReturn(Collections.<Producto>emptyList(), List.of(p1));
        when(productoMapper.toDtos(List.of(p1))).thenReturn(List.of(dto));

        List<ProductoDTO> response = consultaProductoService.consultarProductos();

        assertThat(response).containsExactly(dto);
        verify(sincronizacionProductoService).sincronizarProductos();
    }

    @Test
    void consultarProductos_cuandoLosDatosEstanVigentes_noSincroniza() {
        Producto p1 = new Producto();
        p1.setIdProducto("P001");
        p1.setFechaSincronizacion(LocalDateTime.now());
        ProductoDTO dto = new ProductoDTO();
        dto.setId("P001");
        when(productoRepository.findAll()).thenReturn(List.of(p1));
        when(productoMapper.toDtos(List.of(p1))).thenReturn(List.of(dto));

        List<ProductoDTO> response = consultaProductoService.consultarProductos();

        assertThat(response).containsExactly(dto);
        verifyNoInteractions(sincronizacionProductoService);
    }

    @Test
    void consultarProductos_cuandoLosDatosEstanVencidos_sincroniza() {
        Producto vencido = new Producto();
        vencido.setIdProducto("P001");
        vencido.setFechaSincronizacion(LocalDateTime.now().minusHours(48));
        Producto vigente = new Producto();
        vigente.setIdProducto("P001");
        vigente.setFechaSincronizacion(LocalDateTime.now());
        ProductoDTO dto = new ProductoDTO();
        dto.setId("P001");
        when(productoRepository.findAll()).thenReturn(List.of(vencido), List.of(vigente));
        when(productoMapper.toDtos(List.of(vigente))).thenReturn(List.of(dto));

        List<ProductoDTO> response = consultaProductoService.consultarProductos();

        assertThat(response).containsExactly(dto);
        verify(sincronizacionProductoService).sincronizarProductos();
    }

    @Test
    void consultarProductos_cuandoLosDatosCarecenDeFecha_sincroniza() {
        Producto sinFecha = new Producto();
        sinFecha.setIdProducto("P001");
        when(productoRepository.findAll()).thenReturn(List.of(sinFecha), List.of(sinFecha));
        when(productoMapper.toDtos(List.of(sinFecha))).thenReturn(Collections.<ProductoDTO>emptyList());

        consultaProductoService.consultarProductos();

        verify(sincronizacionProductoService).sincronizarProductos();
    }
}