package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.usuario.Usuario;
import com.proyecto.servicios.model.usuario.UsuarioActualizarRequest;
import com.proyecto.servicios.model.usuario.UsuarioCrearRequest;
import com.proyecto.servicios.model.usuario.UsuarioResponse;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", constant = "ACTIVO")
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "roles", source = "roles", qualifiedByName = "listToString")
    Usuario toEntity(UsuarioCrearRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "roles", source = "roles", qualifiedByName = "listToString")
    void updateEntityFromDto(UsuarioActualizarRequest request, @MappingTarget Usuario entity);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "stringToList")
    UsuarioResponse toDto(Usuario entity);

    List<UsuarioResponse> toDtoList(List<Usuario> entities);

    @Named("listToString")
    default String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "ROLE_USER";
        }
        return String.join(",", list);
    }

    @Named("stringToList")
    default List<String> stringToList(String str) {
        if (StringUtils.isBlank(str)) {
            return Collections.emptyList();
        }
        return Arrays.stream(str.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toList();
    }
}
