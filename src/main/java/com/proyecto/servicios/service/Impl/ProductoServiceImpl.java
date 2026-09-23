package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductoClient;
import com.proyecto.servicios.exception.IntegracionErrorMapper;
import com.proyecto.servicios.exception.IntegracionException;
import com.proyecto.servicios.exception.TipoErrorIntegracion;
import com.proyecto.servicios.mapper.ProductoMapper;
import com.proyecto.servicios.model.gestopago.ProductoDTO;
import com.proyecto.servicios.model.gestopago.ProductoListResponse;
import com.proyecto.servicios.service.GetProductoXmlService;
import com.proyecto.servicios.service.ProductoService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ProductoServiceImpl implements ProductoService {

    private final GestoPagoProductoClient productoClient;
    private final GetProductoXmlService getProductoXmlService;
    private final ProductoMapper productoMapper;
    private final IntegracionErrorMapper integracionErrorMapper;

    public ProductoServiceImpl(GestoPagoProductoClient productoClient,
                               GetProductoXmlService getProductoXmlService,
                               ProductoMapper productoMapper,
                               IntegracionErrorMapper integracionErrorMapper) {
        this.productoClient = productoClient;
        this.getProductoXmlService = getProductoXmlService;
        this.productoMapper = productoMapper;
        this.integracionErrorMapper = integracionErrorMapper;
    }

    @Override
    public ProductoListResponse obtenerProductos() {
        log.info("Iniciando invocacion al servicio externo para obtener lista de productos");
        try {
            String xml = productoClient.getProductList();
            List<ProductoDTO> productos = productoMapper.toDtos(getProductoXmlService.parsear(xml));
            ProductoListResponse response = new ProductoListResponse();
            response.setStatus(200);
            response.setMessage("OK");
            response.setProductos(productos);
            log.info("Finalizando invocacion al servicio externo, {} productos obtenidos", productos.size());
            return response;
        } catch (IntegracionException exception) {
            log.error("Error de integracion al obtener lista de productos: {}", exception.getMessage());
            throw exception;
        } catch (FeignException exception) {
            IntegracionException errorIntegracion = integracionErrorMapper.mapear(exception);
            log.error("Error de integracion al obtener lista de productos: {}", errorIntegracion.getMessage());
            throw errorIntegracion;
        } catch (Exception exception) {
            IntegracionException errorIntegracion = new IntegracionException(TipoErrorIntegracion.ERROR_COMUNICACION,
                    "Error inesperado al invocar el servicio externo", exception);
            log.error("Error de integracion al obtener lista de productos: {}", errorIntegracion.getMessage());
            throw errorIntegracion;
        }
    }
}