package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.usuario.Usuario;
import com.proyecto.servicios.exception.CorreoDuplicadoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.UsernameDuplicadoException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.mapper.UsuarioMapper;
import com.proyecto.servicios.model.usuario.UsuarioActualizarRequest;
import com.proyecto.servicios.model.usuario.UsuarioCrearRequest;
import com.proyecto.servicios.model.usuario.UsuarioFiltroRequest;
import com.proyecto.servicios.model.usuario.UsuarioIdRequest;
import com.proyecto.servicios.model.usuario.UsuarioResponse;
import com.proyecto.servicios.repositorys.usuario.UsuarioRepository;
import com.proyecto.servicios.repositorys.usuario.UsuarioSpecification;
import com.proyecto.servicios.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, UsuarioMapper usuarioMapper) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
    }

    @Override
    @Transactional("sfTransactionManager")
    public UsuarioResponse crearUsuario(UsuarioCrearRequest request) {
        log.info("Iniciando creacion de usuario con username={}", request.getUsername());

        if (usuarioRepository.existsByUsername(request.getUsername().trim())) {
            throw new UsernameDuplicadoException(request.getUsername());
        }

        if (usuarioRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new CorreoDuplicadoException(request.getEmail());
        }

        Usuario entity = usuarioMapper.toEntity(request);
        // Garantizar que campos controlados por el servidor no provengan del cliente
        entity.setId(null);
        entity.setUsername(request.getUsername().trim());
        entity.setEmail(request.getEmail().trim().toLowerCase());
        entity.setEstado("ACTIVO");
        entity.setFechaCreacion(LocalDateTime.now());
        entity.setFechaActualizacion(LocalDateTime.now());

        Usuario guardado = usuarioRepository.save(entity);
        log.info("Usuario creado exitosamente con ID={}", guardado.getId());
        return usuarioMapper.toDto(guardado);
    }

    @Override
    @Transactional(value = "sfTransactionManager", readOnly = true)
    public UsuarioResponse obtenerUsuarioPorId(UsuarioIdRequest request) {
        log.info("Consultando usuario por ID={}", request.getId());
        Usuario usuario = usuarioRepository.findById(request.getId())
                .orElseThrow(() -> new UsuarioNoEncontradoException(request.getId()));
        return usuarioMapper.toDto(usuario);
    }

    @Override
    @Transactional("sfTransactionManager")
    public UsuarioResponse actualizarUsuario(UsuarioActualizarRequest request) {
        log.info("Actualizando usuario con ID={}", request.getId());
        Usuario usuario = usuarioRepository.findById(request.getId())
                .orElseThrow(() -> new UsuarioNoEncontradoException(request.getId()));

        if (StringUtils.isNotBlank(request.getEmail())) {
            String nuevoEmail = request.getEmail().trim().toLowerCase();
            if (!nuevoEmail.equalsIgnoreCase(usuario.getEmail())
                    && usuarioRepository.existsByEmailAndIdNot(nuevoEmail, request.getId())) {
                throw new CorreoDuplicadoException(request.getEmail());
            }
            usuario.setEmail(nuevoEmail);
        }

        usuarioMapper.updateEntityFromDto(request, usuario);
        // Servidor controla fecha de actualizacion
        usuario.setFechaActualizacion(LocalDateTime.now());

        Usuario actualizado = usuarioRepository.save(usuario);
        log.info("Usuario con ID={} actualizado correctamente", actualizado.getId());
        return usuarioMapper.toDto(actualizado);
    }

    @Override
    @Transactional("sfTransactionManager")
    public void eliminarUsuario(UsuarioIdRequest request) {
        log.info("Eliminando usuario con ID={}", request.getId());
        Usuario usuario = usuarioRepository.findById(request.getId())
                .orElseThrow(() -> new UsuarioNoEncontradoException(request.getId()));

        usuarioRepository.delete(usuario);
        log.info("Usuario con ID={} eliminado con exito", request.getId());
    }

    @Override
    @Transactional(value = "sfTransactionManager", readOnly = true)
    public List<UsuarioResponse> buscarUsuarios(UsuarioFiltroRequest filtro) {
        log.info("Ejecutando busqueda dinamica de usuarios");
        if (filtro != null && filtro.getFechaInicio() != null && filtro.getFechaFin() != null) {
            if (filtro.getFechaInicio().isAfter(filtro.getFechaFin())) {
                throw new ReglaNegocioException("La fechaInicio no puede ser posterior a la fechaFin");
            }
        }

        List<Usuario> usuarios = usuarioRepository.findAll(UsuarioSpecification.conFiltros(filtro));
        log.info("Busqueda completada, {} usuarios encontrados", usuarios.size());
        return usuarioMapper.toDtoList(usuarios);
    }
}
