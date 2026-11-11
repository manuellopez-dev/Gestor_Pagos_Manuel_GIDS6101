package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoTransaccionClient;
import com.proyecto.servicios.entity.producto.Producto;
import com.proyecto.servicios.entity.transaccion.EstadoTransaccion;
import com.proyecto.servicios.entity.transaccion.Transaccion;
import com.proyecto.servicios.exception.IntegracionErrorMapper;
import com.proyecto.servicios.exception.IntegracionException;
import com.proyecto.servicios.exception.MontoInvalidoException;
import com.proyecto.servicios.exception.ProductoNoEncontradoException;
import com.proyecto.servicios.exception.TipoErrorIntegracion;
import com.proyecto.servicios.exception.TransaccionDuplicadaException;
import com.proyecto.servicios.exception.TransaccionEnProcesoException;
import com.proyecto.servicios.exception.TransaccionIndeterminadaException;
import com.proyecto.servicios.exception.TransaccionRechazadaException;
import com.proyecto.servicios.exception.ValidacionTransaccionException;
import com.proyecto.servicios.mapper.TransaccionMapper;
import com.proyecto.servicios.model.transaccion.ConciliacionRequest;
import com.proyecto.servicios.model.transaccion.TransaccionRequest;
import com.proyecto.servicios.model.transaccion.TransaccionResponse;
import com.proyecto.servicios.model.xml.MensajeXml;
import com.proyecto.servicios.model.xml.RespuestaTransaccionXml;
import com.proyecto.servicios.repositorys.producto.ProductoRepository;
import com.proyecto.servicios.repositorys.transaccion.TransaccionRepository;
import com.proyecto.servicios.service.GetTransaccionXmlService;
import com.proyecto.servicios.service.TransaccionOperacionService;
import com.proyecto.servicios.service.TransaccionService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Slf4j
public class TransaccionServiceImpl implements TransaccionService {

    private final ProductoRepository productoRepository;
    private final TransaccionRepository transaccionRepository;
    private final GestoPagoTransaccionClient transaccionClient;
    private final GetTransaccionXmlService getTransaccionXmlService;
    private final TransaccionOperacionService transaccionOperacionService;
    private final TransaccionMapper transaccionMapper;
    private final IntegracionErrorMapper integracionErrorMapper;

    public TransaccionServiceImpl(ProductoRepository productoRepository,
                                  TransaccionRepository transaccionRepository,
                                  GestoPagoTransaccionClient transaccionClient,
                                  GetTransaccionXmlService getTransaccionXmlService,
                                  TransaccionOperacionService transaccionOperacionService,
                                  TransaccionMapper transaccionMapper,
                                  IntegracionErrorMapper integracionErrorMapper) {
        this.productoRepository = productoRepository;
        this.transaccionRepository = transaccionRepository;
        this.transaccionClient = transaccionClient;
        this.getTransaccionXmlService = getTransaccionXmlService;
        this.transaccionOperacionService = transaccionOperacionService;
        this.transaccionMapper = transaccionMapper;
        this.integracionErrorMapper = integracionErrorMapper;
    }

    @Override
    public TransaccionResponse procesarTransaccion(TransaccionRequest request) {
        log.info("Iniciando procesamiento de transacción con clave de idempotencia={}", request.getIdempotenciaKey());

        // 1. Control de Idempotencia y prevención de solicitudes duplicadas
        Optional<Transaccion> txExistente = transaccionRepository.findByIdempotenciaKey(request.getIdempotenciaKey());
        if (txExistente.isPresent()) {
            Transaccion existente = txExistente.get();
            if (existente.getEstado() == EstadoTransaccion.EXITOSA) {
                log.info("Operación ya procesada previamente con éxito. ClaveIdempotencia={}", request.getIdempotenciaKey());
                return transaccionMapper.toDto(existente);
            }
            if (existente.getEstado() == EstadoTransaccion.EN_PROCESO
                    || existente.getEstado() == EstadoTransaccion.POR_CONCILIAR) {
                log.warn("Transacción en curso o pendiente de conciliación. ClaveIdempotencia={}", request.getIdempotenciaKey());
                throw new TransaccionEnProcesoException(request.getIdempotenciaKey());
            }
            if (existente.getEstado() == EstadoTransaccion.FALLIDA) {
                log.warn("Transacción previamente fallida con la misma clave de idempotencia={}", request.getIdempotenciaKey());
                throw new TransaccionDuplicadaException(request.getIdempotenciaKey());
            }
        }

        // 2. Validación de Catálogo, Servicio y Monto
        Producto producto = validarProductoYMontos(request);

        // 3. Fase 1: Reserva de Fondos en Cuenta y Creación de Transacción en estado EN_PROCESO
        Transaccion transaccion = transaccionOperacionService.reservarSaldoYCrearTransaccion(
                request.getIdempotenciaKey(),
                request.getCuentaId(),
                request.getIdServicio(),
                request.getIdProducto(),
                producto.getNombre(),
                request.getMonto(),
                request.getReferencia(),
                request.getDigitoVerificador()
        );

        Long txId = transaccion.getId();

        // 4. Fase 2: Integración con GestoPago (POST /sistema/service/sendTx.do)
        RespuestaTransaccionXml respuestaXml;
        try {
            log.info("Invocando endpoint externo sendTx.do para txId={}", txId);
            String xmlResponse = transaccionClient.sendTx(
                    request.getIdServicio(),
                    request.getIdProducto(),
                    request.getMonto().toPlainString(),
                    request.getReferencia(),
                    request.getIdempotenciaKey(),
                    request.getDigitoVerificador()
            );
            respuestaXml = getTransaccionXmlService.parsear(xmlResponse);
        } catch (FeignException ex) {
            IntegracionException errorIntegracion = integracionErrorMapper.mapear(ex);
            return manejarFalloComunicacion(txId, request.getIdempotenciaKey(), errorIntegracion, ex);
        } catch (IntegracionException ex) {
            return manejarFalloComunicacion(txId, request.getIdempotenciaKey(), ex, ex);
        } catch (Exception ex) {
            IntegracionException errorGeneral = new IntegracionException(TipoErrorIntegracion.ERROR_COMUNICACION,
                    "Error inesperado al comunicarse con el proveedor externo", ex);
            return manejarFalloComunicacion(txId, request.getIdempotenciaKey(), errorGeneral, ex);
        }

        // 5. Fase 3: Evaluación de la respuesta del proveedor y Liquidación / Reversión
        MensajeXml mensaje = respuestaXml != null ? respuestaXml.getMensaje() : null;
        String codigo = mensaje != null ? mensaje.getCodigo() : "SIN_CODIGO";
        String texto = mensaje != null ? mensaje.getTexto() : "Sin descripción";

        if ("01".equals(codigo)) {
            log.info("Proveedor confirmó éxito para txId={}. Liquidando operación.", txId);
            Transaccion liquidada = transaccionOperacionService.liquidarTransaccionExitosa(txId, respuestaXml);
            return transaccionMapper.toDto(liquidada);
        } else {
            log.warn("Proveedor rechazó la transacción txId={}. Código={}, Detalle={}", txId, codigo, texto);
            transaccionOperacionService.revertirTransaccion(txId, codigo, texto, false);
            throw new TransaccionRechazadaException(codigo, texto);
        }
    }

    @Override
    public TransaccionResponse conciliarTransaccion(ConciliacionRequest request) {
        log.info("Iniciando conciliación para clave={}", request.getIdempotenciaKey());
        Transaccion tx = transaccionRepository.findByIdempotenciaKey(request.getIdempotenciaKey())
                .orElseThrow(() -> new ValidacionTransaccionException("No se encontró transacción con la clave " + request.getIdempotenciaKey()));

        if (tx.getEstado() == EstadoTransaccion.EXITOSA) {
            log.info("Transacción txId={} ya se encuentra EXITOSA", tx.getId());
            return transaccionMapper.toDto(tx);
        }

        if (tx.getEstado() == EstadoTransaccion.FALLIDA || tx.getEstado() == EstadoTransaccion.REVERSADA) {
            log.info("Transacción txId={} ya se encuentra en estado definitivo={}", tx.getId(), tx.getEstado());
            return transaccionMapper.toDto(tx);
        }

        // Consultar estado con el proveedor
        try {
            log.info("Consultando estado de transacción en proveedor para idempotenciaKey={}", tx.getIdempotenciaKey());
            String xmlQuery = transaccionClient.queryTx(tx.getIdempotenciaKey());
            RespuestaTransaccionXml respuesta = getTransaccionXmlService.parsear(xmlQuery);
            MensajeXml mensaje = respuesta != null ? respuesta.getMensaje() : null;
            if (mensaje != null && "01".equals(mensaje.getCodigo())) {
                log.info("Proveedor confirmó transacción exitosa en conciliación para txId={}", tx.getId());
                Transaccion exitosa = transaccionOperacionService.liquidarTransaccionExitosa(tx.getId(), respuesta);
                return transaccionMapper.toDto(exitosa);
            } else {
                log.info("Proveedor confirmó transacción no aplicada o rechazada en conciliación para txId={}", tx.getId());
                Transaccion reversada = transaccionOperacionService.revertirTransaccion(tx.getId(),
                        mensaje != null ? mensaje.getCodigo() : "REVERSA_CONCILIACION",
                        mensaje != null ? mensaje.getTexto() : "Transacción reversada tras confirmación del proveedor",
                        true);
                return transaccionMapper.toDto(reversada);
            }
        } catch (Exception ex) {
            log.error("Fallo durante la consulta de conciliación con el proveedor: {}", ex.getMessage());
            throw new TransaccionIndeterminadaException(tx.getIdempotenciaKey(),
                    "No fue posible determinar el estado con el proveedor durante la conciliación. Permanece en estado POR_CONCILIAR");
        }
    }

    @Override
    public TransaccionResponse consultarTransaccion(String idempotenciaKey) {
        Transaccion tx = transaccionRepository.findByIdempotenciaKey(idempotenciaKey)
                .orElseThrow(() -> new ValidacionTransaccionException("No se encontró transacción con la clave " + idempotenciaKey));
        return transaccionMapper.toDto(tx);
    }

    private Producto validarProductoYMontos(TransaccionRequest request) {
        Producto producto = productoRepository.findByIdProducto(request.getIdProducto().trim())
                .orElseThrow(() -> new ProductoNoEncontradoException(request.getIdProducto()));

        if (!producto.getIdServicio().trim().equals(request.getIdServicio().trim())) {
            throw new ValidacionTransaccionException("El producto '" + producto.getNombre() +
                    "' (idProducto: " + request.getIdProducto() + ") no corresponde al servicio idServicio: " + request.getIdServicio() +
                    " (servicio registrado: " + producto.getIdServicio() + ")");
        }

        if (StringUtils.isNotBlank(producto.getPrecio())) {
            try {
                BigDecimal precioCatalogo = new BigDecimal(producto.getPrecio().trim());
                if (precioCatalogo.compareTo(BigDecimal.ZERO) > 0 && request.getMonto().compareTo(precioCatalogo) != 0) {
                    throw new MontoInvalidoException("El monto solicitado ($" + request.getMonto() +
                            ") no coincide con el precio fijo del producto ($" + precioCatalogo + ")");
                }
            } catch (NumberFormatException ignored) {
                // Producto con monto variable o leyenda no numérica
            }
        }

        boolean requiereDigito = Boolean.parseBoolean(producto.getHasDigitoVerificador());
        if (requiereDigito && StringUtils.isBlank(request.getDigitoVerificador())) {
            throw new ValidacionTransaccionException("El producto '" + producto.getNombre() + "' requiere dígito verificador obligatoriamente");
        }

        return producto;
    }

    private TransaccionResponse manejarFalloComunicacion(Long txId, String idempotenciaKey,
                                                         IntegracionException errorIntegracion, Exception ex) {
        TipoErrorIntegracion tipo = errorIntegracion.getTipoError();

        // En caso de TIMEOUT o ERROR_COMUNICACION no se asume que falló; se protege contra doble cargo y se marca POR_CONCILIAR
        if (tipo == TipoErrorIntegracion.TIMEOUT || tipo == TipoErrorIntegracion.ERROR_COMUNICACION) {
            log.error("Fallo de comunicación/timeout con el proveedor para txId={}. Marcando como POR_CONCILIAR: {}",
                    txId, errorIntegracion.getMessage());
            transaccionOperacionService.marcarPorConciliar(txId,
                    "Incertidumbre de red (" + tipo + "): requiere proceso de conciliación antes de liberar fondos");
            throw new TransaccionIndeterminadaException(idempotenciaKey,
                    "Tiempo de espera agotado o error de comunicación al contactar a GestoPago. " +
                            "La transacción ha sido marcada como POR_CONCILIAR y el saldo permanecerá reservado hasta conciliar la operación.",
                    ex);
        }

        // Si el fallo es un error definitivo local o autenticación fallida antes de procesar
        log.warn("Fallo definitivo de integración para txId={}. Revirtiendo reserva: {}", txId, errorIntegracion.getMessage());
        transaccionOperacionService.revertirTransaccion(txId, tipo.name(), errorIntegracion.getMessage(), false);
        throw errorIntegracion;
    }
}
