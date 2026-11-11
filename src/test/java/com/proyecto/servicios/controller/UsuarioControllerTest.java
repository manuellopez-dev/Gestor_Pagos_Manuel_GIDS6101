package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.GlobalExceptionHandler;
import com.proyecto.servicios.exception.CorreoDuplicadoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.UsernameDuplicadoException;
import com.proyecto.servicios.exception.UsuarioNoEncontradoException;
import com.proyecto.servicios.model.usuario.UsuarioActualizarRequest;
import com.proyecto.servicios.model.usuario.UsuarioCrearRequest;
import com.proyecto.servicios.model.usuario.UsuarioFiltroRequest;
import com.proyecto.servicios.model.usuario.UsuarioIdRequest;
import com.proyecto.servicios.model.usuario.UsuarioResponse;
import com.proyecto.servicios.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController usuarioController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(usuarioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void crearUsuario_cuandoEntradaValida_retorna201() throws Exception {
        UsuarioCrearRequest request = UsuarioCrearRequest.builder()
                .username("carlosm")
                .nombre("Carlos")
                .apellidos("Mendoza")
                .email("carlos@example.com")
                .telefono("5511223344")
                .roles(List.of("ROLE_USER"))
                .build();

        UsuarioResponse response = UsuarioResponse.builder()
                .id(1L)
                .username("carlosm")
                .nombre("Carlos")
                .apellidos("Mendoza")
                .email("carlos@example.com")
                .estado("ACTIVO")
                .roles(List.of("ROLE_USER"))
                .build();

        when(usuarioService.crearUsuario(any(UsuarioCrearRequest.class))).thenReturn(response);

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exito").value(true))
                .andExpect(jsonPath("$.codigo").value(201))
                .andExpect(jsonPath("$.datos.id").value(1))
                .andExpect(jsonPath("$.datos.username").value("carlosm"));
    }

    @Test
    void crearUsuario_cuandoValidacionFalla_retorna400ConListaDeErrores() throws Exception {
        UsuarioCrearRequest requestInvalido = UsuarioCrearRequest.builder()
                .username("")
                .nombre("")
                .apellidos("")
                .email("correo-invalido")
                .roles(List.of())
                .build();

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exito").value(false))
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").value("Error en las validaciones de entrada"))
                .andExpect(jsonPath("$.errores").isArray());
    }

    @Test
    void crearUsuario_cuandoUsernameDuplicado_retorna409() throws Exception {
        UsuarioCrearRequest request = UsuarioCrearRequest.builder()
                .username("carlosm")
                .nombre("Carlos")
                .apellidos("Mendoza")
                .email("carlos@example.com")
                .roles(List.of("ROLE_USER"))
                .build();

        when(usuarioService.crearUsuario(any(UsuarioCrearRequest.class)))
                .thenThrow(new UsernameDuplicadoException("carlosm"));

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.exito").value(false))
                .andExpect(jsonPath("$.codigo").value(409))
                .andExpect(jsonPath("$.mensaje").value("El nombre de usuario 'carlosm' ya se encuentra en uso"));
    }

    @Test
    void crearUsuario_cuandoCorreoDuplicado_retorna409() throws Exception {
        UsuarioCrearRequest request = UsuarioCrearRequest.builder()
                .username("carlosm")
                .nombre("Carlos")
                .apellidos("Mendoza")
                .email("carlos@example.com")
                .roles(List.of("ROLE_USER"))
                .build();

        when(usuarioService.crearUsuario(any(UsuarioCrearRequest.class)))
                .thenThrow(new CorreoDuplicadoException("carlos@example.com"));

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.exito").value(false))
                .andExpect(jsonPath("$.codigo").value(409));
    }

    @Test
    void obtenerUsuarioPorId_cuandoExiste_retorna200() throws Exception {
        UsuarioIdRequest request = UsuarioIdRequest.builder().id(5L).build();
        UsuarioResponse response = UsuarioResponse.builder()
                .id(5L)
                .username("usuario5")
                .build();

        when(usuarioService.obtenerUsuarioPorId(any(UsuarioIdRequest.class))).thenReturn(response);

        mockMvc.perform(post("/usuarios/obtener")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exito").value(true))
                .andExpect(jsonPath("$.datos.id").value(5));
    }

    @Test
    void obtenerUsuarioPorId_cuandoNoExiste_retorna404() throws Exception {
        UsuarioIdRequest request = UsuarioIdRequest.builder().id(999L).build();

        when(usuarioService.obtenerUsuarioPorId(any(UsuarioIdRequest.class)))
                .thenThrow(new UsuarioNoEncontradoException(999L));

        mockMvc.perform(post("/usuarios/obtener")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exito").value(false))
                .andExpect(jsonPath("$.codigo").value(404));
    }

    @Test
    void buscarUsuarios_conCriteriosEnBody_retorna200() throws Exception {
        UsuarioFiltroRequest filtro = UsuarioFiltroRequest.builder()
                .estado("ACTIVO")
                .build();

        when(usuarioService.buscarUsuarios(any(UsuarioFiltroRequest.class)))
                .thenReturn(List.of(UsuarioResponse.builder().id(1L).username("activo1").build()));

        mockMvc.perform(post("/usuarios/buscar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filtro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exito").value(true))
                .andExpect(jsonPath("$.datos").isArray());
    }

    @Test
    void buscarUsuarios_cuandoReglaNegocioFalla_retorna400() throws Exception {
        UsuarioFiltroRequest filtro = UsuarioFiltroRequest.builder().build();

        when(usuarioService.buscarUsuarios(any(UsuarioFiltroRequest.class)))
                .thenThrow(new ReglaNegocioException("La fechaInicio no puede ser posterior a la fechaFin"));

        mockMvc.perform(post("/usuarios/buscar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filtro)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exito").value(false))
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.mensaje").value("La fechaInicio no puede ser posterior a la fechaFin"));
    }

    @Test
    void actualizarUsuario_cuandoValido_retorna200() throws Exception {
        UsuarioActualizarRequest request = UsuarioActualizarRequest.builder()
                .id(1L)
                .nombre("Nuevo Nombre")
                .apellidos("Nuevos Apellidos")
                .build();

        UsuarioResponse response = UsuarioResponse.builder()
                .id(1L)
                .nombre("Nuevo Nombre")
                .build();

        when(usuarioService.actualizarUsuario(any(UsuarioActualizarRequest.class))).thenReturn(response);

        mockMvc.perform(put("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exito").value(true))
                .andExpect(jsonPath("$.datos.nombre").value("Nuevo Nombre"));
    }

    @Test
    void actualizarUsuario_cuandoIdEsNulo_retorna400() throws Exception {
        UsuarioActualizarRequest requestInvalido = UsuarioActualizarRequest.builder()
                .id(null)
                .nombre("Nombre")
                .apellidos("Apellidos")
                .build();

        mockMvc.perform(put("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exito").value(false))
                .andExpect(jsonPath("$.codigo").value(400));
    }

    @Test
    void eliminarUsuario_cuandoExiste_retorna200() throws Exception {
        UsuarioIdRequest request = UsuarioIdRequest.builder().id(7L).build();
        doNothing().when(usuarioService).eliminarUsuario(any(UsuarioIdRequest.class));

        mockMvc.perform(delete("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exito").value(true))
                .andExpect(jsonPath("$.mensaje").value("Usuario eliminado exitosamente"));
    }

    @Test
    void eliminarUsuario_cuandoNoExiste_retorna404() throws Exception {
        UsuarioIdRequest request = UsuarioIdRequest.builder().id(77L).build();
        doThrow(new UsuarioNoEncontradoException(77L))
                .when(usuarioService).eliminarUsuario(any(UsuarioIdRequest.class));

        mockMvc.perform(delete("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.exito").value(false))
                .andExpect(jsonPath("$.codigo").value(404));
    }
}
