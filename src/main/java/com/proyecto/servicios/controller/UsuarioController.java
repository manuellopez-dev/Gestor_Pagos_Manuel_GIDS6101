package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.common.ApiResponse;
import com.proyecto.servicios.model.usuario.UsuarioActualizarRequest;
import com.proyecto.servicios.model.usuario.UsuarioCrearRequest;
import com.proyecto.servicios.model.usuario.UsuarioFiltroRequest;
import com.proyecto.servicios.model.usuario.UsuarioIdRequest;
import com.proyecto.servicios.model.usuario.UsuarioResponse;
import com.proyecto.servicios.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/usuarios", produces = MediaType.APPLICATION_JSON_VALUE)
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UsuarioResponse>> crearUsuario(
            @Valid @RequestBody UsuarioCrearRequest request) {
        UsuarioResponse creado = usuarioService.crearUsuario(request);
        return new ResponseEntity<>(
                ApiResponse.exitoso(HttpStatus.CREATED.value(), "Usuario registrado exitosamente", creado),
                HttpStatus.CREATED
        );
    }

    @PostMapping(value = "/obtener", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtenerUsuarioPorId(
            @Valid @RequestBody UsuarioIdRequest request) {
        UsuarioResponse usuario = usuarioService.obtenerUsuarioPorId(request);
        return ResponseEntity.ok(ApiResponse.exitoso("Usuario obtenido exitosamente", usuario));
    }

    @PostMapping(value = "/buscar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> buscarUsuarios(
            @Valid @RequestBody UsuarioFiltroRequest filtro) {
        List<UsuarioResponse> resultados = usuarioService.buscarUsuarios(filtro);
        return ResponseEntity.ok(ApiResponse.exitoso("Consulta realizada exitosamente", resultados));
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizarUsuario(
            @Valid @RequestBody UsuarioActualizarRequest request) {
        UsuarioResponse actualizado = usuarioService.actualizarUsuario(request);
        return ResponseEntity.ok(ApiResponse.exitoso("Usuario actualizado exitosamente", actualizado));
    }

    @DeleteMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> eliminarUsuario(
            @Valid @RequestBody UsuarioIdRequest request) {
        usuarioService.eliminarUsuario(request);
        return ResponseEntity.ok(ApiResponse.exitoso("Usuario eliminado exitosamente", null));
    }

    @PostMapping(value = "/eliminar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> eliminarUsuarioPost(
            @Valid @RequestBody UsuarioIdRequest request) {
        usuarioService.eliminarUsuario(request);
        return ResponseEntity.ok(ApiResponse.exitoso("Usuario eliminado exitosamente", null));
    }
}
