package com.proyecto.servicios.service;

import com.proyecto.servicios.model.usuario.UsuarioActualizarRequest;
import com.proyecto.servicios.model.usuario.UsuarioCrearRequest;
import com.proyecto.servicios.model.usuario.UsuarioFiltroRequest;
import com.proyecto.servicios.model.usuario.UsuarioIdRequest;
import com.proyecto.servicios.model.usuario.UsuarioResponse;

import java.util.List;

public interface UsuarioService {

    UsuarioResponse crearUsuario(UsuarioCrearRequest request);

    UsuarioResponse obtenerUsuarioPorId(UsuarioIdRequest request);

    UsuarioResponse actualizarUsuario(UsuarioActualizarRequest request);

    void eliminarUsuario(UsuarioIdRequest request);

    List<UsuarioResponse> buscarUsuarios(UsuarioFiltroRequest filtro);
}
