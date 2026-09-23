package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.producto.Producto;
import com.proyecto.servicios.model.gestopago.ProductoDTO;
import com.proyecto.servicios.model.xml.ProductoXml;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nombre", source = "producto")
    @Mapping(target = "fechaSincronizacion", ignore = true)
    Producto toEntity(ProductoXml xml);

    @IterableMapping(elementTargetType = Producto.class)
    List<Producto> toEntities(List<ProductoXml> xmlList);

    @Mapping(target = "id", source = "idProducto")
    ProductoDTO toDto(Producto producto);

    List<ProductoDTO> toDtos(List<Producto> productos);
}