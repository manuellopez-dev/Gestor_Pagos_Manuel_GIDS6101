package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.producto.Producto;
import com.proyecto.servicios.exception.IntegracionException;
import com.proyecto.servicios.exception.TipoErrorIntegracion;
import com.proyecto.servicios.mapper.ProductoMapper;
import com.proyecto.servicios.model.xml.ProductosXml;
import com.proyecto.servicios.model.xml.RespuestaProductosXml;
import com.proyecto.servicios.service.GetProductoXmlService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class GetProductoXmlServiceImpl implements GetProductoXmlService {

    private final ProductoMapper productoMapper;

    public GetProductoXmlServiceImpl(ProductoMapper productoMapper) {
        this.productoMapper = productoMapper;
    }

    @Override
    public List<Producto> parsear(String xml) {
        if (StringUtils.isBlank(xml)) {
            throw new IntegracionException(TipoErrorIntegracion.RESPUESTA_NO_EXITOSA,
                    "El servicio externo no devolvio XML de productos");
        }
        try {
            JAXBContext context = JAXBContext.newInstance(RespuestaProductosXml.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            RespuestaProductosXml respuesta = (RespuestaProductosXml) unmarshaller.unmarshal(new StringReader(xml));
            ProductosXml productosXml = respuesta.getProductos();
            List<Producto> productos = productosXml == null ? null : productoMapper.toEntities(productosXml.getProductos());
            if (productos == null) {
                return Collections.emptyList();
            }
            log.info("XML de productos procesado correctamente, {} registros", productos.size());
            return productos;
        } catch (JAXBException exception) {
            throw new IntegracionException(TipoErrorIntegracion.RESPUESTA_NO_EXITOSA,
                    "No fue posible procesar el XML de productos", exception);
        }
    }
}