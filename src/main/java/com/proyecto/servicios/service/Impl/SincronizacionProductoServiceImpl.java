package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductoClient;
import com.proyecto.servicios.entity.producto.Producto;
import com.proyecto.servicios.exception.IntegracionErrorMapper;
import com.proyecto.servicios.exception.IntegracionException;
import com.proyecto.servicios.exception.TipoErrorIntegracion;
import com.proyecto.servicios.repositorys.producto.ProductoRepository;
import com.proyecto.servicios.service.GetProductoXmlService;
import com.proyecto.servicios.service.SincronizacionProductoService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class SincronizacionProductoServiceImpl implements SincronizacionProductoService {

    private final GestoPagoProductoClient productoClient;
    private final GetProductoXmlService getProductoXmlService;
    private final ProductoRepository productoRepository;
    private final IntegracionErrorMapper integracionErrorMapper;

    public SincronizacionProductoServiceImpl(GestoPagoProductoClient productoClient,
                                             GetProductoXmlService getProductoXmlService,
                                             ProductoRepository productoRepository,
                                             IntegracionErrorMapper integracionErrorMapper) {
        this.productoClient = productoClient;
        this.getProductoXmlService = getProductoXmlService;
        this.productoRepository = productoRepository;
        this.integracionErrorMapper = integracionErrorMapper;
    }

    @Scheduled(cron = "${gestopago.productos.cron:0 0 6 * * *}")
    public void sincronizacionProgramada() {
        try {
            sincronizarProductos();
        } catch (IntegracionException exception) {
            log.error("Fallo la sincronizacion programada de productos: {}", exception.getMessage());
        }
    }

    @Override
    public int sincronizarProductos() {
        log.info("Iniciando sincronizacion de productos desde el servicio externo");
        try {
            String xml = productoClient.getProductList();
            List<Producto> productos = getProductoXmlService.parsear(xml);
            productos.forEach(this::guardarProducto);
            log.info("Sincronizacion finalizada, {} productos procesados", productos.size());
            return productos.size();
        } catch (IntegracionException exception) {
            log.error("Error de integracion en la sincronizacion de productos: {}", exception.getMessage());
            throw exception;
        } catch (FeignException exception) {
            IntegracionException errorIntegracion = integracionErrorMapper.mapear(exception);
            log.error("Error de integracion en la sincronizacion de productos: {}", errorIntegracion.getMessage());
            throw errorIntegracion;
        } catch (Exception exception) {
            IntegracionException errorIntegracion = new IntegracionException(TipoErrorIntegracion.ERROR_COMUNICACION,
                    "Error inesperado al sincronizar productos", exception);
            log.error("Error de integracion en la sincronizacion de productos: {}", errorIntegracion.getMessage());
            throw errorIntegracion;
        }
    }

    private void guardarProducto(Producto producto) {
        Producto existente = productoRepository.findByIdProducto(producto.getIdProducto()).orElse(null);
        if (existente != null) {
            producto.setId(existente.getId());
        }
        producto.setFechaSincronizacion(LocalDateTime.now());
        productoRepository.save(producto);
    }
}