package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoTransaccionClient;
import com.proyecto.servicios.config.GestoPagoAuthProperties;
import com.proyecto.servicios.entity.cuenta.Cuenta;
import com.proyecto.servicios.entity.producto.Producto;
import com.proyecto.servicios.entity.transaccion.EstadoTransaccion;
import com.proyecto.servicios.entity.transaccion.Transaccion;
import com.proyecto.servicios.exception.IntegracionErrorMapper;
import com.proyecto.servicios.exception.MontoInvalidoException;
import com.proyecto.servicios.exception.PassSeguroInvalidoException;
import com.proyecto.servicios.exception.ProductoNoEncontradoException;
import com.proyecto.servicios.exception.TransaccionDuplicadaException;
import com.proyecto.servicios.exception.TransaccionEnProcesoException;
import com.proyecto.servicios.exception.TransaccionIndeterminadaException;
import com.proyecto.servicios.exception.TransaccionRechazadaException;
import com.proyecto.servicios.exception.ValidacionTransaccionException;
import com.proyecto.servicios.mapper.TransaccionMapper;
import com.proyecto.servicios.model.transaccion.ConciliacionRequest;
import com.proyecto.servicios.model.transaccion.TransaccionRequest;
import com.proyecto.servicios.model.transaccion.TransaccionResponse;
import com.proyecto.servicios.model.transaccion.TransaccionVentaRequest;
import com.proyecto.servicios.model.xml.MensajeXml;
import com.proyecto.servicios.model.xml.RespuestaTransaccionXml;
import com.proyecto.servicios.model.xml.TransaccionXml;
import com.proyecto.servicios.repositorys.producto.ProductoRepository;
import com.proyecto.servicios.repositorys.transaccion.TransaccionRepository;
import com.proyecto.servicios.service.CryptoService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.GetTransaccionXmlService;
import com.proyecto.servicios.service.TransaccionOperacionService;
import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransaccionServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private TransaccionRepository transaccionRepository;

    @Mock
    private GestoPagoTransaccionClient transaccionClient;

    @Mock
    private GetTransaccionXmlService getTransaccionXmlService;

    @Mock
    private TransaccionOperacionService transaccionOperacionService;

    @Mock
    private CryptoService cryptoService;

    @Mock
    private GestoPagoTokenService gestoPagoTokenService;

    private GestoPagoAuthProperties authProperties;
    private TransaccionMapper transaccionMapper;
    private TransaccionServiceImpl transaccionService;

    @BeforeEach
    void setUp() {
        authProperties = new GestoPagoAuthProperties();
        authProperties.setIdDistribuidor(83);
        authProperties.setCodigoDispositivo("GPS83-TPV-17");

        transaccionMapper = Mappers.getMapper(TransaccionMapper.class);
        transaccionService = new TransaccionServiceImpl(
                productoRepository,
                transaccionRepository,
                transaccionClient,
                getTransaccionXmlService,
                transaccionOperacionService,
                transaccionMapper,
                new IntegracionErrorMapper(),
                cryptoService,
                gestoPagoTokenService,
                authProperties
        );
    }

    private TransaccionRequest crearRequest() {
        return TransaccionRequest.builder()
                .idempotenciaKey("IDEM-12345678")
                .cuentaId(1L)
                .idServicio("56")
                .idProducto("185")
                .monto(new BigDecimal("100.50"))
                .referencia("9988776655")
                .build();
    }

    private Producto crearProducto() {
        Producto p = new Producto();
        p.setIdProducto("185");
        p.setIdServicio("56");
        p.setNombre("Agua Cancun");
        p.setPrecio("100.50");
        p.setHasDigitoVerificador("false");
        return p;
    }

    @Test
    void procesarTransaccion_cuandoTodoEsValidoYProveedorConfirma_liquidaTransaccion() {
        TransaccionRequest request = crearRequest();
        Producto producto = crearProducto();

        when(transaccionRepository.findByIdempotenciaKey("IDEM-12345678")).thenReturn(Optional.empty());
        when(productoRepository.findByIdProducto("185")).thenReturn(Optional.of(producto));

        Cuenta cuenta = Cuenta.builder().id(1L).numeroCuenta("CTA-001").build();
        Transaccion txCreada = Transaccion.builder()
                .id(100L)
                .idempotenciaKey("IDEM-12345678")
                .cuenta(cuenta)
                .estado(EstadoTransaccion.EN_PROCESO)
                .monto(new BigDecimal("100.50"))
                .build();

        when(transaccionOperacionService.reservarSaldoYCrearTransaccion(
                anyString(), anyLong(), anyString(), anyString(), anyString(), any(), anyString(), any()))
                .thenReturn(txCreada);

        String xmlExito = "<RESPONSE><MENSAJE><CODIGO>01</CODIGO><TEXTO>OK</TEXTO></MENSAJE></RESPONSE>";
        when(transaccionClient.sendTx(eq("56"), eq("185"), eq("100.50"), eq("9988776655"), eq("IDEM-12345678"), any()))
                .thenReturn(xmlExito);

        RespuestaTransaccionXml respuestaXml = new RespuestaTransaccionXml();
        MensajeXml mensaje = new MensajeXml();
        mensaje.setCodigo("01");
        mensaje.setTexto("OK");
        TransaccionXml detalle = new TransaccionXml();
        detalle.setFolioAttr("GP-FOLIO-123");
        detalle.setAutorizacionAttr("AUTH-123");
        respuestaXml.setMensaje(mensaje);
        respuestaXml.setTransaccion(detalle);

        when(getTransaccionXmlService.parsear(xmlExito)).thenReturn(respuestaXml);

        Transaccion txLiquidada = Transaccion.builder()
                .id(100L)
                .idempotenciaKey("IDEM-12345678")
                .cuenta(cuenta)
                .estado(EstadoTransaccion.EXITOSA)
                .monto(new BigDecimal("100.50"))
                .folioProveedor("GP-FOLIO-123")
                .autorizacionProveedor("AUTH-123")
                .build();

        when(transaccionOperacionService.liquidarTransaccionExitosa(eq(100L), eq(respuestaXml)))
                .thenReturn(txLiquidada);

        TransaccionResponse response = transaccionService.procesarTransaccion(request);

        assertThat(response).isNotNull();
        assertThat(response.getEstado()).isEqualTo(EstadoTransaccion.EXITOSA);
        assertThat(response.getFolioProveedor()).isEqualTo("GP-FOLIO-123");
        verify(transaccionOperacionService).liquidarTransaccionExitosa(eq(100L), eq(respuestaXml));
    }

    @Test
    void procesarVenta_flujoCompleto_cifraFirmaValidaPinYDescuentaSaldo() {
        TransaccionVentaRequest ventaRequest = TransaccionVentaRequest.builder()
                .numCuenta("CTA-001")
                .idServicio("56")
                .idpermiso("PERM-ADMIN")
                .precio(new BigDecimal("150.00"))
                .distribuidor("DIST-83")
                .telefono("5544332211")
                .bandera("VENTA_DIRECTA")
                .pin("1234")
                .idProducto("185")
                .idempotenciaKey("IDEM-VTA-1")
                .build();

        when(transaccionRepository.findByIdempotenciaKey("IDEM-VTA-1")).thenReturn(Optional.empty());

        Producto producto = crearProducto();
        when(productoRepository.findByIdProducto("185")).thenReturn(Optional.of(producto));

        when(cryptoService.firmar(anyString())).thenReturn("FIRMA_HMAC_BASE64");
        when(cryptoService.cifrarAes(anyString())).thenReturn("AES_ENCRYPTED_BASE64");

        Cuenta cuenta = Cuenta.builder().id(1L).numeroCuenta("CTA-001").build();
        Transaccion txVenta = Transaccion.builder()
                .id(500L)
                .idempotenciaKey("IDEM-VTA-1")
                .cuenta(cuenta)
                .estado(EstadoTransaccion.EN_PROCESO)
                .monto(new BigDecimal("150.00"))
                .build();

        when(transaccionOperacionService.reservarSaldoVenta(
                eq("IDEM-VTA-1"), eq("CTA-001"), eq("1234"), eq("56"), eq("185"),
                anyString(), eq(new BigDecimal("150.00")), eq("5544332211"), eq("DIST-83"),
                eq("PERM-ADMIN"), eq("VENTA_DIRECTA"), anyString(), eq("1"),
                eq("FIRMA_HMAC_BASE64"), eq("AES_ENCRYPTED_BASE64")))
                .thenReturn(txVenta);

        String xmlExito = "<RESPONSE><MENSAJE><CODIGO>01</CODIGO><TEXTO>OK</TEXTO></MENSAJE></RESPONSE>";
        when(transaccionClient.sendTx(eq("56"), eq("185"), eq("150.00"), eq("5544332211"), eq("IDEM-VTA-1"), any()))
                .thenReturn(xmlExito);

        RespuestaTransaccionXml respuestaXml = new RespuestaTransaccionXml();
        MensajeXml mensaje = new MensajeXml();
        mensaje.setCodigo("01");
        mensaje.setTexto("OK");
        TransaccionXml detalle = new TransaccionXml();
        detalle.setFolioAttr("GP-VTA-999");
        respuestaXml.setMensaje(mensaje);
        respuestaXml.setTransaccion(detalle);

        when(getTransaccionXmlService.parsear(xmlExito)).thenReturn(respuestaXml);

        Transaccion txLiquidada = Transaccion.builder()
                .id(500L)
                .idempotenciaKey("IDEM-VTA-1")
                .cuenta(cuenta)
                .estado(EstadoTransaccion.EXITOSA)
                .monto(new BigDecimal("150.00"))
                .folioProveedor("GP-VTA-999")
                .build();

        when(transaccionOperacionService.liquidarTransaccionExitosa(eq(500L), eq(respuestaXml)))
                .thenReturn(txLiquidada);

        TransaccionResponse response = transaccionService.procesarVenta(ventaRequest);

        assertThat(response).isNotNull();
        assertThat(response.getEstado()).isEqualTo(EstadoTransaccion.EXITOSA);
        assertThat(response.getFolioProveedor()).isEqualTo("GP-VTA-999");

        verify(cryptoService).firmar(anyString());
        verify(cryptoService).cifrarAes(anyString());
        verify(transaccionOperacionService).reservarSaldoVenta(
                eq("IDEM-VTA-1"), eq("CTA-001"), eq("1234"), eq("56"), eq("185"),
                anyString(), eq(new BigDecimal("150.00")), eq("5544332211"), eq("DIST-83"),
                eq("PERM-ADMIN"), eq("VENTA_DIRECTA"), anyString(), eq("1"),
                eq("FIRMA_HMAC_BASE64"), eq("AES_ENCRYPTED_BASE64"));
        verify(transaccionOperacionService).liquidarTransaccionExitosa(eq(500L), eq(respuestaXml));
    }

    @Test
    void procesarVenta_cuandoPinEsInvalido_lanzaPassSeguroInvalidoException() {
        TransaccionVentaRequest ventaRequest = TransaccionVentaRequest.builder()
                .numCuenta("CTA-001")
                .idServicio("56")
                .idpermiso("PERM-ADMIN")
                .precio(new BigDecimal("150.00"))
                .distribuidor("DIST-83")
                .telefono("5544332211")
                .bandera("VENTA")
                .pin("0000") // PIN Incorrecto
                .idProducto("185")
                .idempotenciaKey("IDEM-PIN-FAIL")
                .build();

        when(transaccionRepository.findByIdempotenciaKey("IDEM-PIN-FAIL")).thenReturn(Optional.empty());
        when(cryptoService.firmar(anyString())).thenReturn("FIRMA");
        when(cryptoService.cifrarAes(anyString())).thenReturn("AES");

        when(transaccionOperacionService.reservarSaldoVenta(
                anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new PassSeguroInvalidoException("La credencial de seguridad (Pass Seguro / PIN) es incorrecta"));

        assertThatThrownBy(() -> transaccionService.procesarVenta(ventaRequest))
                .isInstanceOf(PassSeguroInvalidoException.class)
                .hasMessageContaining("Pass Seguro / PIN");

        verify(transaccionClient, never()).sendTx(any(), any(), any(), any(), any(), any());
    }

    @Test
    void procesarTransaccion_cuandoIdempotenciaYaExisteExitosa_retornaSinVolverACobrar() {
        TransaccionRequest request = crearRequest();
        Cuenta cuenta = Cuenta.builder().id(1L).build();
        Transaccion existente = Transaccion.builder()
                .id(99L)
                .idempotenciaKey("IDEM-12345678")
                .cuenta(cuenta)
                .estado(EstadoTransaccion.EXITOSA)
                .monto(new BigDecimal("100.50"))
                .folioProveedor("GP-PREVIO")
                .build();

        when(transaccionRepository.findByIdempotenciaKey("IDEM-12345678")).thenReturn(Optional.of(existente));

        TransaccionResponse response = transaccionService.procesarTransaccion(request);

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getEstado()).isEqualTo(EstadoTransaccion.EXITOSA);
        verify(transaccionClient, never()).sendTx(any(), any(), any(), any(), any(), any());
    }

    @Test
    void procesarTransaccion_cuandoIdempotenciaYaExisteEnProceso_lanzaTransaccionEnProcesoException() {
        TransaccionRequest request = crearRequest();
        Transaccion enProceso = Transaccion.builder()
                .id(99L)
                .idempotenciaKey("IDEM-12345678")
                .estado(EstadoTransaccion.EN_PROCESO)
                .build();

        when(transaccionRepository.findByIdempotenciaKey("IDEM-12345678")).thenReturn(Optional.of(enProceso));

        assertThatThrownBy(() -> transaccionService.procesarTransaccion(request))
                .isInstanceOf(TransaccionEnProcesoException.class);
    }

    @Test
    void procesarTransaccion_cuandoProductoNoExiste_lanzaProductoNoEncontradoException() {
        TransaccionRequest request = crearRequest();

        when(transaccionRepository.findByIdempotenciaKey("IDEM-12345678")).thenReturn(Optional.empty());
        when(productoRepository.findByIdProducto("185")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transaccionService.procesarTransaccion(request))
                .isInstanceOf(ProductoNoEncontradoException.class);
    }

    @Test
    void procesarTransaccion_cuandoServicioNoCoincideConProducto_lanzaValidacionTransaccionException() {
        TransaccionRequest request = crearRequest();
        Producto producto = crearProducto();
        producto.setIdServicio("999"); // Diferente al "56" del request

        when(transaccionRepository.findByIdempotenciaKey("IDEM-12345678")).thenReturn(Optional.empty());
        when(productoRepository.findByIdProducto("185")).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> transaccionService.procesarTransaccion(request))
                .isInstanceOf(ValidacionTransaccionException.class)
                .hasMessageContaining("no corresponde al servicio");
    }

    @Test
    void procesarTransaccion_cuandoMontoNoCoincideConPrecioFijo_lanzaMontoInvalidoException() {
        TransaccionRequest request = crearRequest();
        request.setMonto(new BigDecimal("50.00")); // Precio fijo en producto es 100.50
        Producto producto = crearProducto();

        when(transaccionRepository.findByIdempotenciaKey("IDEM-12345678")).thenReturn(Optional.empty());
        when(productoRepository.findByIdProducto("185")).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> transaccionService.procesarTransaccion(request))
                .isInstanceOf(MontoInvalidoException.class)
                .hasMessageContaining("no coincide con el precio fijo");
    }

    @Test
    void procesarTransaccion_cuandoProductoRequiereDigitoVerificadorYNoSeEnvia_lanzaValidacionTransaccionException() {
        TransaccionRequest request = crearRequest();
        request.setDigitoVerificador(null);
        Producto producto = crearProducto();
        producto.setHasDigitoVerificador("true");

        when(transaccionRepository.findByIdempotenciaKey("IDEM-12345678")).thenReturn(Optional.empty());
        when(productoRepository.findByIdProducto("185")).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> transaccionService.procesarTransaccion(request))
                .isInstanceOf(ValidacionTransaccionException.class)
                .hasMessageContaining("requiere dígito verificador");
    }

    @Test
    void procesarTransaccion_cuandoProveedorRechaza_revierteSaldoYLanzaTransaccionRechazadaException() {
        TransaccionRequest request = crearRequest();
        Producto producto = crearProducto();

        when(transaccionRepository.findByIdempotenciaKey("IDEM-12345678")).thenReturn(Optional.empty());
        when(productoRepository.findByIdProducto("185")).thenReturn(Optional.of(producto));

        Cuenta cuenta = Cuenta.builder().id(1L).build();
        Transaccion txCreada = Transaccion.builder().id(105L).cuenta(cuenta).build();
        when(transaccionOperacionService.reservarSaldoYCrearTransaccion(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(txCreada);

        String xmlError = "<RESPONSE><MENSAJE><CODIGO>00</CODIGO><TEXTO>Saldo insuficiente distribuidor</TEXTO></MENSAJE></RESPONSE>";
        when(transaccionClient.sendTx(any(), any(), any(), any(), any(), any())).thenReturn(xmlError);

        RespuestaTransaccionXml respuestaXml = new RespuestaTransaccionXml();
        MensajeXml mensaje = new MensajeXml();
        mensaje.setCodigo("00");
        mensaje.setTexto("Saldo insuficiente distribuidor");
        respuestaXml.setMensaje(mensaje);

        when(getTransaccionXmlService.parsear(xmlError)).thenReturn(respuestaXml);

        assertThatThrownBy(() -> transaccionService.procesarTransaccion(request))
                .isInstanceOf(TransaccionRechazadaException.class)
                .hasMessageContaining("Saldo insuficiente distribuidor");

        verify(transaccionOperacionService).revertirTransaccion(eq(105L), eq("00"), eq("Saldo insuficiente distribuidor"), eq(false));
    }

    @Test
    void procesarTransaccion_cuandoOcurreTimeout_marcaPorConciliarYNoRevierteSaldo_lanzaTransaccionIndeterminadaException() {
        TransaccionRequest request = crearRequest();
        Producto producto = crearProducto();

        when(transaccionRepository.findByIdempotenciaKey("IDEM-12345678")).thenReturn(Optional.empty());
        when(productoRepository.findByIdProducto("185")).thenReturn(Optional.of(producto));

        Cuenta cuenta = Cuenta.builder().id(1L).build();
        Transaccion txCreada = Transaccion.builder().id(106L).cuenta(cuenta).build();
        when(transaccionOperacionService.reservarSaldoYCrearTransaccion(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(txCreada);

        Request feignReq = Request.create(Request.HttpMethod.POST, "http://test", Collections.emptyMap(), null, StandardCharsets.UTF_8);
        RetryableException timeoutException = new RetryableException(504, "Read timed out", Request.HttpMethod.POST, -1L, feignReq);
        timeoutException.initCause(new SocketTimeoutException("Read timed out"));

        when(transaccionClient.sendTx(any(), any(), any(), any(), any(), any()))
                .thenThrow(timeoutException);

        assertThatThrownBy(() -> transaccionService.procesarTransaccion(request))
                .isInstanceOf(TransaccionIndeterminadaException.class)
                .hasMessageContaining("POR_CONCILIAR");

        verify(transaccionOperacionService).marcarPorConciliar(eq(106L), anyString());
        // NO se debe revertir el saldo!
        verify(transaccionOperacionService, never()).revertirTransaccion(anyLong(), anyString(), anyString(), anyBoolean());
    }

    @Test
    void conciliarTransaccion_cuandoProveedorConfirmaExito_liquidaTransaccion() {
        Cuenta cuenta = Cuenta.builder().id(1L).build();
        Transaccion tx = Transaccion.builder()
                .id(200L)
                .idempotenciaKey("IDEM-CONCILIAR")
                .cuenta(cuenta)
                .estado(EstadoTransaccion.POR_CONCILIAR)
                .build();

        when(transaccionRepository.findByIdempotenciaKey("IDEM-CONCILIAR")).thenReturn(Optional.of(tx));

        String xml = "<RESPONSE><MENSAJE><CODIGO>01</CODIGO><TEXTO>OK</TEXTO></MENSAJE></RESPONSE>";
        when(transaccionClient.queryTx("IDEM-CONCILIAR")).thenReturn(xml);

        RespuestaTransaccionXml respuestaXml = new RespuestaTransaccionXml();
        MensajeXml mensaje = new MensajeXml();
        mensaje.setCodigo("01");
        mensaje.setTexto("OK");
        respuestaXml.setMensaje(mensaje);
        when(getTransaccionXmlService.parsear(xml)).thenReturn(respuestaXml);

        Transaccion txExitosa = Transaccion.builder()
                .id(200L)
                .cuenta(cuenta)
                .estado(EstadoTransaccion.EXITOSA)
                .build();
        when(transaccionOperacionService.liquidarTransaccionExitosa(eq(200L), eq(respuestaXml)))
                .thenReturn(txExitosa);

        TransaccionResponse response = transaccionService.conciliarTransaccion(
                ConciliacionRequest.builder().idempotenciaKey("IDEM-CONCILIAR").build());

        assertThat(response.getEstado()).isEqualTo(EstadoTransaccion.EXITOSA);
        verify(transaccionOperacionService).liquidarTransaccionExitosa(eq(200L), eq(respuestaXml));
    }
}
