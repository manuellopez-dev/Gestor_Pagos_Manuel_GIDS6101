package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.producto.Producto;
import com.proyecto.servicios.mapper.ProductoMapper;
import com.proyecto.servicios.model.gestopago.ProductoDTO;
import com.proyecto.servicios.repositorys.producto.ProductoRepository;
import com.proyecto.servicios.service.ConsultaProductoService;
import com.proyecto.servicios.service.SincronizacionProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ConsultaProductoServiceImpl implements ConsultaProductoService {

    private final ProductoRepository productoRepository;
    private final SincronizacionProductoService sincronizacionProductoService;
    private final ProductoMapper productoMapper;
    private final Integer maxAntiguedadHoras;

    public ConsultaProductoServiceImpl(ProductoRepository productoRepository,
                                       SincronizacionProductoService sincronizacionProductoService,
                                       ProductoMapper productoMapper,
                                       @Value("${gestopago.productos.consulta.max-antiguedad-horas:24}") Integer maxAntiguedadHoras) {
        this.productoRepository = productoRepository;
        this.sincronizacionProductoService = sincronizacionProductoService;
        this.productoMapper = productoMapper;
        this.maxAntiguedadHoras = maxAntiguedadHoras;
    }

    @Override
    public List<ProductoDTO> consultarProductos() {
        log.info("Iniciando consulta de productos disponibles");
        List<Producto> productos = productoRepository.findAll();

        if (requiereActualizacion(productos)) {
            log.info("Los datos no estan vigentes, se sincroniza desde el servicio externo");
            sincronizacionProductoService.sincronizarProductos();
            productos = productoRepository.findAll();
        }

        List<ProductoDTO> response = productoMapper.toDtos(productos);
        log.info("Consulta finalizada, {} productos disponibles", response.size());
        return response;
    }

    private boolean requiereActualizacion(List<Producto> productos) {
        if (productos.isEmpty()) {
            return true;
        }
        LocalDateTime limite = LocalDateTime.now().minusHours(maxAntiguedadHoras);
        return productos.stream()
                .map(Producto::getFechaSincronizacion)
                .anyMatch(fecha -> fecha == null || fecha.isBefore(limite));
    }
}