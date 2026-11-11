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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioMapper usuarioMapper;
    private UsuarioServiceImpl usuarioService;

    @BeforeEach
    void setUp() {
        usuarioMapper = Mappers.getMapper(UsuarioMapper.class);
        usuarioService = new UsuarioServiceImpl(usuarioRepository, usuarioMapper);
    }

    @Test
    void crearUsuario_cuandoDatosSonValidos_creaUsuarioConCamposControladosPorServidor() {
        UsuarioCrearRequest request = UsuarioCrearRequest.builder()
                .username("juanperez")
                .nombre("Juan")
                .apellidos("Perez Lopez")
                .email("juan.perez@example.com")
                .telefono("5512345678")
                .roles(List.of("ROLE_USER"))
                .build();

        when(usuarioRepository.existsByUsername("juanperez")).thenReturn(false);
        when(usuarioRepository.existsByEmail("juan.perez@example.com")).thenReturn(false);

        Usuario entityGuardada = Usuario.builder()
                .id(1L)
                .username("juanperez")
                .nombre("Juan")
                .apellidos("Perez Lopez")
                .email("juan.perez@example.com")
                .telefono("5512345678")
                .estado("ACTIVO")
                .roles("ROLE_USER")
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        when(usuarioRepository.save(any(Usuario.class))).thenReturn(entityGuardada);

        UsuarioResponse response = usuarioService.crearUsuario(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("juanperez");
        assertThat(response.getEstado()).isEqualTo("ACTIVO");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario capturado = captor.getValue();
        assertThat(capturado.getId()).isNull();
        assertThat(capturado.getEstado()).isEqualTo("ACTIVO");
        assertThat(capturado.getFechaCreacion()).isNotNull();
    }

    @Test
    void crearUsuario_cuandoUsernameYaExiste_lanzaUsernameDuplicadoException() {
        UsuarioCrearRequest request = UsuarioCrearRequest.builder()
                .username("juanperez")
                .nombre("Juan")
                .apellidos("Perez")
                .email("juan.perez@example.com")
                .roles(List.of("ROLE_USER"))
                .build();

        when(usuarioRepository.existsByUsername("juanperez")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crearUsuario(request))
                .isInstanceOf(UsernameDuplicadoException.class)
                .hasMessageContaining("juanperez");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crearUsuario_cuandoEmailYaExiste_lanzaCorreoDuplicadoException() {
        UsuarioCrearRequest request = UsuarioCrearRequest.builder()
                .username("juanperez")
                .nombre("Juan")
                .apellidos("Perez")
                .email("juan.perez@example.com")
                .roles(List.of("ROLE_USER"))
                .build();

        when(usuarioRepository.existsByUsername("juanperez")).thenReturn(false);
        when(usuarioRepository.existsByEmail("juan.perez@example.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crearUsuario(request))
                .isInstanceOf(CorreoDuplicadoException.class)
                .hasMessageContaining("juan.perez@example.com");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void obtenerUsuarioPorId_cuandoExiste_retornaUsuarioResponse() {
        Usuario usuario = Usuario.builder()
                .id(10L)
                .username("mariag")
                .nombre("Maria")
                .apellidos("Gonzalez")
                .email("maria@example.com")
                .estado("ACTIVO")
                .roles("ROLE_USER")
                .build();

        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

        UsuarioResponse response = usuarioService.obtenerUsuarioPorId(UsuarioIdRequest.builder().id(10L).build());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getUsername()).isEqualTo("mariag");
    }

    @Test
    void obtenerUsuarioPorId_cuandoNoExiste_lanzaUsuarioNoEncontradoException() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtenerUsuarioPorId(UsuarioIdRequest.builder().id(99L).build()))
                .isInstanceOf(UsuarioNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void actualizarUsuario_cuandoExisteYDatosSonValidos_actualizaCorrectamente() {
        Usuario existente = Usuario.builder()
                .id(5L)
                .username("pedror")
                .nombre("Pedro")
                .apellidos("Ramirez")
                .email("pedro@example.com")
                .estado("ACTIVO")
                .roles("ROLE_USER")
                .fechaCreacion(LocalDateTime.now().minusDays(1))
                .fechaActualizacion(LocalDateTime.now().minusDays(1))
                .build();

        UsuarioActualizarRequest request = UsuarioActualizarRequest.builder()
                .id(5L)
                .nombre("Pedro Modificado")
                .apellidos("Ramirez Ruiz")
                .email("pedro.nuevo@example.com")
                .telefono("5599887766")
                .estado("ACTIVO")
                .roles(List.of("ROLE_ADMIN"))
                .build();

        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByEmailAndIdNot("pedro.nuevo@example.com", 5L)).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponse response = usuarioService.actualizarUsuario(request);

        assertThat(response).isNotNull();
        assertThat(response.getNombre()).isEqualTo("Pedro Modificado");
        assertThat(response.getApellidos()).isEqualTo("Ramirez Ruiz");
        assertThat(response.getEmail()).isEqualTo("pedro.nuevo@example.com");
        assertThat(response.getRoles()).contains("ROLE_ADMIN");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void actualizarUsuario_cuandoEmailPerteneceAOtroUsuario_lanzaCorreoDuplicadoException() {
        Usuario existente = Usuario.builder()
                .id(5L)
                .username("pedror")
                .nombre("Pedro")
                .apellidos("Ramirez")
                .email("pedro@example.com")
                .build();

        UsuarioActualizarRequest request = UsuarioActualizarRequest.builder()
                .id(5L)
                .nombre("Pedro")
                .apellidos("Ramirez")
                .email("otro.correo@example.com")
                .build();

        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByEmailAndIdNot("otro.correo@example.com", 5L)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.actualizarUsuario(request))
                .isInstanceOf(CorreoDuplicadoException.class)
                .hasMessageContaining("otro.correo@example.com");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void actualizarUsuario_cuandoUsuarioNoExiste_lanzaUsuarioNoEncontradoException() {
        UsuarioActualizarRequest request = UsuarioActualizarRequest.builder()
                .id(999L)
                .nombre("NoExiste")
                .apellidos("Persona")
                .build();

        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.actualizarUsuario(request))
                .isInstanceOf(UsuarioNoEncontradoException.class);
    }

    @Test
    void eliminarUsuario_cuandoExiste_eliminaDeRepositorio() {
        Usuario existente = Usuario.builder()
                .id(15L)
                .username("eliminarme")
                .nombre("Usuario")
                .apellidos("Prueba")
                .build();

        when(usuarioRepository.findById(15L)).thenReturn(Optional.of(existente));

        usuarioService.eliminarUsuario(UsuarioIdRequest.builder().id(15L).build());

        verify(usuarioRepository).delete(existente);
    }

    @Test
    void eliminarUsuario_cuandoNoExiste_lanzaUsuarioNoEncontradoException() {
        when(usuarioRepository.findById(15L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.eliminarUsuario(UsuarioIdRequest.builder().id(15L).build()))
                .isInstanceOf(UsuarioNoEncontradoException.class);

        verify(usuarioRepository, never()).delete(any(Usuario.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscarUsuarios_conFiltrosValidos_retornaResultados() {
        UsuarioFiltroRequest filtro = UsuarioFiltroRequest.builder()
                .nombre("Juan")
                .estado("ACTIVO")
                .build();

        Usuario u1 = Usuario.builder()
                .id(1L)
                .username("juan1")
                .nombre("Juan")
                .apellidos("Perez")
                .email("juan1@example.com")
                .estado("ACTIVO")
                .roles("ROLE_USER")
                .build();

        when(usuarioRepository.findAll(any(Specification.class))).thenReturn(List.of(u1));

        List<UsuarioResponse> resultados = usuarioService.buscarUsuarios(filtro);

        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).getUsername()).isEqualTo("juan1");
    }

    @Test
    void buscarUsuarios_cuandoRangoFechasEsIncoherente_lanzaReglaNegocioException() {
        UsuarioFiltroRequest filtro = UsuarioFiltroRequest.builder()
                .fechaInicio(LocalDateTime.now())
                .fechaFin(LocalDateTime.now().minusDays(1))
                .build();

        assertThatThrownBy(() -> usuarioService.buscarUsuarios(filtro))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("fechaInicio no puede ser posterior");
    }
}
