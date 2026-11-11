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
import com.proyecto.servicios.service.TransaccionOperacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
public class TransaccionOperacionServiceImpl implements TransaccionOperacionService {

    private final CuentaRepository cuentaRepository;
    private final TransaccionRepository transaccionRepository;

    public TransaccionOperacionServiceImpl(CuentaRepository cuentaRepository,
                                           TransaccionRepository transaccionRepository) {
        this.cuentaRepository = cuentaRepository;
        this.transaccionRepository = transaccionRepository;
    }

    @Override
    @Transactional("sfTransactionManager")
    public Transaccion reservarSaldoYCrearTransaccion(String idempotenciaKey, Long cuentaId, String idServicio,
                                                     String idProducto, String nombreProducto, BigDecimal monto,
                                                     String referencia, String digitoVerificador) {
        log.info("Iniciando reserva de saldo para cuentaId={}, monto={}", cuentaId, monto);

        // Bloqueo pesimista para evitar carreras o doble gasto concurrente
        Cuenta cuenta = cuentaRepository.findByIdConBloqueo(cuentaId)
                .orElseThrow(() -> new CuentaNoEncontradaException(cuentaId));

        if (cuenta.getEstado() != EstadoCuenta.ACTIVA) {
            log.warn("Intento de operación sobre cuenta inactiva o bloqueada. CuentaId={}, Estado={}", cuentaId, cuenta.getEstado());
            throw new CuentaInactivaException("La cuenta " + cuenta.getNumeroCuenta() + " no está activa (Estado: " + cuenta.getEstado() + ")");
        }

        BigDecimal disponible = cuenta.getSaldoDisponible();
        if (disponible.compareTo(monto) < 0) {
            log.warn("Saldo insuficiente para cuentaId={}. Disponible={}, Requerido={}", cuentaId, disponible, monto);
            throw new SaldoInsuficienteException("Saldo disponible insuficiente: $" + disponible + ", monto requerido: $" + monto);
        }

        // Se aisla el saldo en saldoRetenido
        cuenta.setSaldoRetenido(cuenta.getSaldoRetenido().add(monto));
        cuenta.setFechaActualizacion(LocalDateTime.now());
        cuentaRepository.save(cuenta);

        Transaccion tx = Transaccion.builder()
                .idempotenciaKey(idempotenciaKey)
                .cuenta(cuenta)
                .idServicio(idServicio)
                .idProducto(idProducto)
                .nombreProducto(nombreProducto)
                .monto(monto)
                .referencia(referencia)
                .digitoVerificador(digitoVerificador)
                .estado(EstadoTransaccion.EN_PROCESO)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        Transaccion guardada = transaccionRepository.save(tx);
        log.info("Saldo reservado exitosamente. TransaccionId={}, IdempotenciaKey={}", guardada.getId(), idempotenciaKey);
        return guardada;
    }

    @Override
    @Transactional("sfTransactionManager")
    public Transaccion liquidarTransaccionExitosa(Long transaccionId, RespuestaTransaccionXml respuestaXml) {
        log.info("Liquidando transacción exitosa txId={}", transaccionId);
        Transaccion tx = transaccionRepository.findById(transaccionId)
                .orElseThrow(() -> new IllegalStateException("Transacción no encontrada con ID " + transaccionId));

        Cuenta cuenta = cuentaRepository.findByIdConBloqueo(tx.getCuenta().getId())
                .orElseThrow(() -> new CuentaNoEncontradaException(tx.getCuenta().getId()));

        // Descuento definitivo del saldo y liberación de la retención
        cuenta.setSaldo(cuenta.getSaldo().subtract(tx.getMonto()));
        cuenta.setSaldoRetenido(cuenta.getSaldoRetenido().subtract(tx.getMonto()));
        cuenta.setFechaActualizacion(LocalDateTime.now());
        cuentaRepository.save(cuenta);

        tx.setEstado(EstadoTransaccion.EXITOSA);
        if (respuestaXml != null) {
            MensajeXml mensaje = respuestaXml.getMensaje();
            TransaccionXml detalle = respuestaXml.getTransaccion();
            if (mensaje != null) {
                tx.setCodigoRespuestaProveedor(mensaje.getCodigo());
                tx.setMensajeProveedor(mensaje.getTexto());
            }
            if (detalle != null) {
                tx.setFolioProveedor(detalle.getFolio());
                tx.setAutorizacionProveedor(detalle.getAutorizacion());
            }
        }
        tx.setFechaActualizacion(LocalDateTime.now());
        Transaccion liquidada = transaccionRepository.save(tx);
        log.info("Transacción txId={} liquidada exitosamente. Folio={}", transaccionId, tx.getFolioProveedor());
        return liquidada;
    }

    @Override
    @Transactional("sfTransactionManager")
    public Transaccion revertirTransaccion(Long transaccionId, String codigoError, String mensajeError, boolean esReversa) {
        log.info("Revirtiendo reserva de fondos para txId={}", transaccionId);
        Transaccion tx = transaccionRepository.findById(transaccionId)
                .orElseThrow(() -> new IllegalStateException("Transacción no encontrada con ID " + transaccionId));

        Cuenta cuenta = cuentaRepository.findByIdConBloqueo(tx.getCuenta().getId())
                .orElseThrow(() -> new CuentaNoEncontradaException(tx.getCuenta().getId()));

        // Se libera la retención devolviendo el disponible a la cuenta
        cuenta.setSaldoRetenido(cuenta.getSaldoRetenido().subtract(tx.getMonto()));
        cuenta.setFechaActualizacion(LocalDateTime.now());
        cuentaRepository.save(cuenta);

        tx.setEstado(esReversa ? EstadoTransaccion.REVERSADA : EstadoTransaccion.FALLIDA);
        tx.setCodigoRespuestaProveedor(codigoError);
        tx.setMensajeProveedor(mensajeError);
        tx.setFechaActualizacion(LocalDateTime.now());
        Transaccion revertida = transaccionRepository.save(tx);
        log.info("Transacción txId={} revertida. NuevoEstado={}", transaccionId, tx.getEstado());
        return revertida;
    }

    @Override
    @Transactional("sfTransactionManager")
    public Transaccion marcarPorConciliar(Long transaccionId, String detalle) {
        log.warn("Marcando transacción txId={} como POR_CONCILIAR debido a incertidumbre de red", transaccionId);
        Transaccion tx = transaccionRepository.findById(transaccionId)
                .orElseThrow(() -> new IllegalStateException("Transacción no encontrada con ID " + transaccionId));

        tx.setEstado(EstadoTransaccion.POR_CONCILIAR);
        tx.setMensajeProveedor(detalle);
        tx.setFechaActualizacion(LocalDateTime.now());
        return transaccionRepository.save(tx);
    }
}
