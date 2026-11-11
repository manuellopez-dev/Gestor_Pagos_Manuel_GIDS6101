package com.proyecto.servicios.repositorys.usuario;

import com.proyecto.servicios.entity.usuario.Usuario;
import com.proyecto.servicios.model.usuario.UsuarioFiltroRequest;
import jakarta.persistence.criteria.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class UsuarioSpecification {

    private UsuarioSpecification() {
    }

    public static Specification<Usuario> conFiltros(UsuarioFiltroRequest filtro) {
        return (root, query, cb) -> {
            if (filtro == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.isNotBlank(filtro.getUsername())) {
                predicates.add(cb.like(
                        cb.lower(root.get("username")),
                        "%" + filtro.getUsername().trim().toLowerCase() + "%"
                ));
            }

            if (StringUtils.isNotBlank(filtro.getNombre())) {
                predicates.add(cb.like(
                        cb.lower(root.get("nombre")),
                        "%" + filtro.getNombre().trim().toLowerCase() + "%"
                ));
            }

            if (StringUtils.isNotBlank(filtro.getApellidos())) {
                predicates.add(cb.like(
                        cb.lower(root.get("apellidos")),
                        "%" + filtro.getApellidos().trim().toLowerCase() + "%"
                ));
            }

            if (StringUtils.isNotBlank(filtro.getEmail())) {
                predicates.add(cb.like(
                        cb.lower(root.get("email")),
                        "%" + filtro.getEmail().trim().toLowerCase() + "%"
                ));
            }

            if (StringUtils.isNotBlank(filtro.getTelefono())) {
                predicates.add(cb.like(
                        root.get("telefono"),
                        "%" + filtro.getTelefono().trim() + "%"
                ));
            }

            if (StringUtils.isNotBlank(filtro.getEstado())) {
                predicates.add(cb.equal(
                        cb.upper(root.get("estado")),
                        filtro.getEstado().trim().toUpperCase()
                ));
            }

            if (StringUtils.isNotBlank(filtro.getRol())) {
                predicates.add(cb.like(
                        cb.lower(root.get("roles")),
                        "%" + filtro.getRol().trim().toLowerCase() + "%"
                ));
            }

            if (filtro.getFechaInicio() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("fechaCreacion"),
                        filtro.getFechaInicio()
                ));
            }

            if (filtro.getFechaFin() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("fechaCreacion"),
                        filtro.getFechaFin()
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
