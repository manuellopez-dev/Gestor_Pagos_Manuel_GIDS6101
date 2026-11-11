package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.transaccion.Transaccion;
import com.proyecto.servicios.model.transaccion.TransaccionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransaccionMapper {

    @Mapping(target = "cuentaId", source = "cuenta.id")
    TransaccionResponse toDto(Transaccion entity);

    List<TransaccionResponse> toDtoList(List<Transaccion> entities);
}
