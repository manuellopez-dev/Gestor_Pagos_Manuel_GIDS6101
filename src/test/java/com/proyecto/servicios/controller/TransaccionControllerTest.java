package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.config.GlobalExceptionHandler;
import com.proyecto.servicios.entity.transaccion.EstadoTransaccion;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.exception.TransaccionDuplicadaException;
import com.proyecto.servicios.exception.TransaccionIndeterminadaException;
import com.proyecto.servicios.model.transaccion.ConciliacionRequest;
import com.proyecto.servicios.model.transaccion.TransaccionRequest;
import com.proyecto.servicios.model.transaccion.TransaccionResponse;
import com.proyecto.servicios.service.TransaccionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransaccionControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TransaccionService transaccionService;

    @InjectMocks
    private TransaccionController transaccionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transaccionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private TransaccionRequest crearRequestValido() {
        return TransaccionRequest.builder()
                .idempotenciaKey("IDEM-12345678")
                .cuentaId(1L)
                .idServicio("56")
                .idProducto("185")
                .monto(new BigDecimal("100.50"))
                .referencia("9988776655")
                .build();
    }

    @Test
    void enviarTransaccion_cuandoEntradaValida_retorna200() throws Exception {
        TransaccionRequest request = crearRequestValido();
        TransaccionResponse response = TransaccionResponse.builder()
                .id(1L)
                .idempotenciaKey("IDEM-12345678")
                .estado(EstadoTransaccion.EXITOSA)
                .monto(new BigDecimal("100.50"))
                .folioProveedor("GP-FOLIO-123")
                .build();

        when(transaccionService.procesarTransaccion(any(TransaccionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/transacciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exito").value(true))
                .andExpect(jsonPath("$.codigo").value(200))
                .andExpect(jsonPath("$.datos.folioProveedor").value("GP-FOLIO-123"));
    }

    @Test
    void enviarTransaccion_cuandoFallaValidacionBean_retorna400ConListaErrores() throws Exception {
        TransaccionRequest requestInvalido = TransaccionRequest.builder()
                .idempotenciaKey("") // Invalido
                .cuentaId(null) // Invalido
                .monto(new BigDecimal("-10.00")) // Invalido
                .referencia("") // Invalido
                .build();

        mockMvc.perform(post("/transacciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exito").value(false))
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.errores").isArray());
    }

    @Test
    void enviarTransaccion_cuandoSaldoInsuficiente_retorna422() throws Exception {
        TransaccionRequest request = crearRequestValido();

        when(transaccionService.procesarTransaccion(any(TransaccionRequest.class)))
                .thenThrow(new SaldoInsuficienteException("Saldo disponible insuficiente"));

        mockMvc.perform(post("/transacciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.exito").value(false))
                .andExpect(jsonPath("$.codigo").value(422))
                .andExpect(jsonPath("$.mensaje").value("Saldo disponible insuficiente"));
    }

    @Test
    void enviarTransaccion_cuandoTransaccionDuplicada_retorna409() throws Exception {
        TransaccionRequest request = crearRequestValido();

        when(transaccionService.procesarTransaccion(any(TransaccionRequest.class)))
                .thenThrow(new TransaccionDuplicadaException("IDEM-12345678"));

        mockMvc.perform(post("/transacciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.exito").value(false))
                .andExpect(jsonPath("$.codigo").value(409));
    }

    @Test
    void enviarTransaccion_cuandoTimeout_retorna504() throws Exception {
        TransaccionRequest request = crearRequestValido();

        when(transaccionService.procesarTransaccion(any(TransaccionRequest.class)))
                .thenThrow(new TransaccionIndeterminadaException("IDEM-12345678", "Timeout con GestoPago. Marcado POR_CONCILIAR"));

        mockMvc.perform(post("/transacciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.exito").value(false))
                .andExpect(jsonPath("$.codigo").value(504))
                .andExpect(jsonPath("$.mensaje").value("Timeout con GestoPago. Marcado POR_CONCILIAR"));
    }

    @Test
    void conciliarTransaccion_cuandoValido_retorna200() throws Exception {
        ConciliacionRequest request = ConciliacionRequest.builder()
                .idempotenciaKey("IDEM-CONCILIAR")
                .build();

        TransaccionResponse response = TransaccionResponse.builder()
                .idempotenciaKey("IDEM-CONCILIAR")
                .estado(EstadoTransaccion.EXITOSA)
                .build();

        when(transaccionService.conciliarTransaccion(any(ConciliacionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/transacciones/conciliar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exito").value(true))
                .andExpect(jsonPath("$.datos.estado").value("EXITOSA"));
    }
}
