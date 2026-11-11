package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cuenta.Cuenta;
import com.proyecto.servicios.entity.cuenta.EstadoCuenta;
import com.proyecto.servicios.entity.transaccion.EstadoTransaccion;
import com.proyecto.servicios.entity.transaccion.Transaccion;
import com.proyecto.servicios.exception.CuentaInactivaException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.model.xml.MensajeXml;
import com.proyecto.servicios.model.xml.RespuestaTransaccionXml;
import com.proyecto.servicios.model.xml.TransaccionXml;
import com.proyecto.servicios.repositorys.cuenta.CuentaRepository;
import com.proyecto.servicios.repositorys.transaccion.TransaccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransaccionOperacionServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private TransaccionRepository transaccionRepository;

    private TransaccionOperacionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TransaccionOperacionServiceImpl(cuentaRepository, transaccionRepository);
    }

    @Test
    void reservarSaldoYCrearTransaccion_cuandoCuentaActivaYSaldoSuficiente_reservaSaldoYGuardaTransaccion() {
        Cuenta cuenta = Cuenta.builder()
                .id(1L)
                .numeroCuenta("CTA-001")
                .saldo(new BigDecimal("500.00"))
                .saldoRetenido(BigDecimal.ZERO)
                .estado(EstadoCuenta.ACTIVA)
                .build();

        when(cuentaRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(cuenta));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(i -> {
            Transaccion t = i.getArgument(0);
            t.setId(10L);
            return t;
        });

        Transaccion tx = service.reservarSaldoYCrearTransaccion("TX-KEY-001", 1L, "56", "185",
                "Agua Cancun", new BigDecimal("100.50"), "9988776655", null);

        assertThat(tx).isNotNull();
        assertThat(tx.getId()).isEqualTo(10L);
        assertThat(tx.getEstado()).isEqualTo(EstadoTransaccion.EN_PROCESO);

        ArgumentCaptor<Cuenta> cuentaCaptor = ArgumentCaptor.forClass(Cuenta.class);
        verify(cuentaRepository).save(cuentaCaptor.capture());
        assertThat(cuentaCaptor.getValue().getSaldoRetenido()).isEqualByComparingTo(new BigDecimal("100.50"));
    }

    @Test
    void reservarSaldoYCrearTransaccion_cuandoCuentaNoExiste_lanzaCuentaNoEncontradaException() {
        when(cuentaRepository.findByIdConBloqueo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reservarSaldoYCrearTransaccion("KEY", 99L, "56", "185",
                "Agua", new BigDecimal("100"), "ref", null))
                .isInstanceOf(CuentaNoEncontradaException.class);
    }

    @Test
    void reservarSaldoYCrearTransaccion_cuandoCuentaEstaBloqueada_lanzaCuentaInactivaException() {
        Cuenta cuenta = Cuenta.builder()
                .id(2L)
                .numeroCuenta("CTA-002")
                .saldo(new BigDecimal("500.00"))
                .saldoRetenido(BigDecimal.ZERO)
                .estado(EstadoCuenta.BLOQUEADA)
                .build();

        when(cuentaRepository.findByIdConBloqueo(2L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> service.reservarSaldoYCrearTransaccion("KEY", 2L, "56", "185",
                "Agua", new BigDecimal("100"), "ref", null))
                .isInstanceOf(CuentaInactivaException.class);
    }

    @Test
    void reservarSaldoYCrearTransaccion_cuandoSaldoInsuficiente_lanzaSaldoInsuficienteException() {
        Cuenta cuenta = Cuenta.builder()
                .id(3L)
                .numeroCuenta("CTA-003")
                .saldo(new BigDecimal("50.00"))
                .saldoRetenido(BigDecimal.ZERO)
                .estado(EstadoCuenta.ACTIVA)
                .build();

        when(cuentaRepository.findByIdConBloqueo(3L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> service.reservarSaldoYCrearTransaccion("KEY", 3L, "56", "185",
                "Agua", new BigDecimal("100.00"), "ref", null))
                .isInstanceOf(SaldoInsuficienteException.class)
                .hasMessageContaining("Saldo disponible insuficiente");
    }

    @Test
    void liquidarTransaccionExitosa_descuentaSaldoYRetenidoYMarcaExitosa() {
        Cuenta cuenta = Cuenta.builder()
                .id(1L)
                .numeroCuenta("CTA-001")
                .saldo(new BigDecimal("500.00"))
                .saldoRetenido(new BigDecimal("100.50"))
                .estado(EstadoCuenta.ACTIVA)
                .build();

        Transaccion tx = Transaccion.builder()
                .id(10L)
                .cuenta(cuenta)
                .monto(new BigDecimal("100.50"))
                .estado(EstadoTransaccion.EN_PROCESO)
                .build();

        when(transaccionRepository.findById(10L)).thenReturn(Optional.of(tx));
        when(cuentaRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(cuenta));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(i -> i.getArgument(0));

        RespuestaTransaccionXml respuestaXml = new RespuestaTransaccionXml();
        MensajeXml mensaje = new MensajeXml();
        mensaje.setCodigo("01");
        mensaje.setTexto("Exito");
        TransaccionXml detalle = new TransaccionXml();
        detalle.setFolioAttr("GP-FOLIO-123");
        detalle.setAutorizacionAttr("AUTH-999");
        respuestaXml.setMensaje(mensaje);
        respuestaXml.setTransaccion(detalle);

        Transaccion liquidada = service.liquidarTransaccionExitosa(10L, respuestaXml);

        assertThat(liquidada.getEstado()).isEqualTo(EstadoTransaccion.EXITOSA);
        assertThat(liquidada.getFolioProveedor()).isEqualTo("GP-FOLIO-123");
        assertThat(liquidada.getAutorizacionProveedor()).isEqualTo("AUTH-999");

        ArgumentCaptor<Cuenta> cuentaCaptor = ArgumentCaptor.forClass(Cuenta.class);
        verify(cuentaRepository).save(cuentaCaptor.capture());
        Cuenta cuentaGuardada = cuentaCaptor.getValue();
        assertThat(cuentaGuardada.getSaldo()).isEqualByComparingTo(new BigDecimal("399.50"));
        assertThat(cuentaGuardada.getSaldoRetenido()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void revertirTransaccion_liberaRetenidoYMarcaFallida() {
        Cuenta cuenta = Cuenta.builder()
                .id(1L)
                .numeroCuenta("CTA-001")
                .saldo(new BigDecimal("500.00"))
                .saldoRetenido(new BigDecimal("100.50"))
                .estado(EstadoCuenta.ACTIVA)
                .build();

        Transaccion tx = Transaccion.builder()
                .id(10L)
                .cuenta(cuenta)
                .monto(new BigDecimal("100.50"))
                .estado(EstadoTransaccion.EN_PROCESO)
                .build();

        when(transaccionRepository.findById(10L)).thenReturn(Optional.of(tx));
        when(cuentaRepository.findByIdConBloqueo(1L)).thenReturn(Optional.of(cuenta));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(i -> i.getArgument(0));

        Transaccion revertida = service.revertirTransaccion(10L, "00", "Rechazo de prueba", false);

        assertThat(revertida.getEstado()).isEqualTo(EstadoTransaccion.FALLIDA);
        assertThat(revertida.getCodigoRespuestaProveedor()).isEqualTo("00");

        ArgumentCaptor<Cuenta> cuentaCaptor = ArgumentCaptor.forClass(Cuenta.class);
        verify(cuentaRepository).save(cuentaCaptor.capture());
        Cuenta cuentaGuardada = cuentaCaptor.getValue();
        assertThat(cuentaGuardada.getSaldo()).isEqualByComparingTo(new BigDecimal("500.00")); // Saldo intacto
        assertThat(cuentaGuardada.getSaldoRetenido()).isEqualByComparingTo(BigDecimal.ZERO); // Retención liberada
    }

    @Test
    void marcarPorConciliar_marcaTransaccionComoPorConciliarSinModificarSaldo() {
        Transaccion tx = Transaccion.builder()
                .id(20L)
                .estado(EstadoTransaccion.EN_PROCESO)
                .build();

        when(transaccionRepository.findById(20L)).thenReturn(Optional.of(tx));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(i -> i.getArgument(0));

        Transaccion porConciliar = service.marcarPorConciliar(20L, "Timeout detectado");

        assertThat(porConciliar.getEstado()).isEqualTo(EstadoTransaccion.POR_CONCILIAR);
        assertThat(porConciliar.getMensajeProveedor()).isEqualTo("Timeout detectado");
    }
}
