# Integración — Procesamiento de Transacciones Financieras (`sendTx.do`)

Documento de arquitectura, análisis de riesgos y decisiones de diseño para la integración del endpoint:
`POST https://gestopago.portalventas.net/sistema/service/sendTx.do`

---

## 1. Análisis de Riesgos Financieros y Escenarios de Fallo

| Riesgo / Escenario | Impacto | Decisión de Diseño / Mitigación |
|---|---|---|
| **Doble gasto por solicitudes concurrentes** | Saldo negativo en la cuenta si dos hilos intentan debitar en paralelo. | **Bloqueo pesimista (`PESSIMISTIC_WRITE`)** en la entidad `Cuenta` (`SELECT FOR UPDATE`), garantizando exclusión mutua durante la verificación y reserva. |
| **Doble cargo por reintentos del cliente** | Cobrar dos veces al usuario por una misma recarga o pago. | **Clave de Idempotencia obligatoria (`idempotenciaKey`)** con índice único en BD. Si la transacción ya existe como `EXITOSA`, se retorna la respuesta idéntica sin re-ejecutar. |
| **Pérdida de dinero por Timeout con el proveedor** | Si la conexión se corta tras enviar `sendTx.do`, el proveedor pudo haber procesado la recarga pero el cliente no recibió respuesta. Si se revierte el dinero de inmediato, la plataforma asume la pérdida. | **Regla de Incertidumbre:** Un timeout **NUNCA** se asume como fallo. Se marca en estado `POR_CONCILIAR` y los fondos permanecen retenidos hasta ejecutar el proceso de conciliación (`queryTx.do`). |
| **Agotamiento de pool de conexiones BD por llamadas lentas** | Mantener una transacción de BD abierta durante una llamada HTTP externa bloquea conexiones físicas y degrada el sistema. | **Estrategia en Dos Fases:** La reserva se confirma en una transacción corta; la llamada HTTP se ejecuta con la conexión de BD liberada; la liquidación se ejecuta en una segunda transacción corta. |
| **Falsificación o manipulación de montos** | Un atacante envía un monto inferior al costo real del producto. | **Validación contra catálogo:** Se consulta MongoDB y se compara el monto contra el precio oficial del producto. Si no coincide, se rechaza de inmediato con `MontoInvalidoException`. |
| **Exposición de credenciales** | Filtración del Token Bearer o API Key en logs o respuestas HTTP. | El Bearer token es inyectado exclusivamente en memoria por Feign (`ProductoFeignConfig`). Los logs y excepciones ocultan cabeceras sensibles y cuerpos crudos. |

---

## 2. Estrategia de Descuento de Dinero: Reserva en Dos Fases (Hold & Settle)

```
[Cliente] ──▶ POST /transacciones (idempotenciaKey, cuentaId, idProducto, monto, ...)
                  │
                  ▼
          [1. Pre-validaciones] ──▶ ¿Producto existe en MongoDB? ¿Precio coincide? ¿Servicio coincide?
                  │
                  ▼
          [2. Fase 1: Reserva de Saldo] (Transacción BD corta con PESSIMISTIC_WRITE)
                  │  - Valida cuenta ACTIVA y saldoDisponible >= monto
                  │  - Incrementa saldoRetenido += monto
                  │  - Guarda Transaccion con estado EN_PROCESO
                  │  - COMMITS & Libera lock de BD
                  ▼
          [3. Fase 2: Invocación Externa Feign] ──▶ POST /sistema/service/sendTx.do
                  │
                  ├───▶ A) Proveedor responde CODIGO="01" (Éxito)
                  │         ▼
                  │     [Fase 3A: Liquidación Definitiva]
                  │     saldo -= monto, saldoRetenido -= monto
                  │     estado = EXITOSA, guarda Folio y Autorización
                  │     HTTP 200 OK
                  │
                  ├───▶ B) Proveedor responde Rechazo Explícito (ej. CODIGO="00")
                  │         ▼
                  │     [Fase 3B: Reversión de Reserva]
                  │     saldoRetenido -= monto (devuelve disponible)
                  │     estado = FALLIDA
                  │     HTTP 422 Unprocessable Entity
                  │
                  └───▶ C) Timeout / SocketTimeoutException / 504 / Caída de Red
                            ▼
                        [Fase 3C: Protección e Incertidumbre]
                        NO se modifica saldoRetenido (fondos protegidos)
                        estado = POR_CONCILIAR
                        HTTP 504 Gateway Timeout
                            │
                            ▼
                        [Conciliación Posterior: POST /transacciones/conciliar]
                        Consulta queryTx.do ──▶ si existe: liquida / si no: revierte
```

---

## 3. Endpoints Implementados

### 3.1. Procesar Transacción Financiera
- **Método / URL:** `POST /transacciones`
- **Request Body (`TransaccionRequest`):**
```json
{
  "idempotenciaKey": "TX-PAY-2026-998811",
  "cuentaId": 1,
  "idServicio": "56",
  "idProducto": "185",
  "monto": 100.50,
  "referencia": "9988776655",
  "digitoVerificador": null
}
```
- **Respuesta Exitosa (HTTP 200):**
```json
{
  "exito": true,
  "codigo": 200,
  "mensaje": "Transacción procesada exitosamente",
  "timestamp": "2026-11-10 19:00:00",
  "datos": {
    "id": 1,
    "idempotenciaKey": "TX-PAY-2026-998811",
    "cuentaId": 1,
    "idServicio": "56",
    "idProducto": "185",
    "nombreProducto": "Agua Cancun (Mun. de Benito Juarez)",
    "monto": 100.50,
    "referencia": "9988776655",
    "estado": "EXITOSA",
    "folioProveedor": "GP-554433",
    "autorizacionProveedor": "AUTH-991122",
    "codigoRespuestaProveedor": "01",
    "mensajeProveedor": "Operacion realizada con exito",
    "fechaCreacion": "2026-11-10 19:00:00",
    "fechaActualizacion": "2026-11-10 19:00:02"
  }
}
```

### 3.2. Conciliar Transacción Indeterminada
- **Método / URL:** `POST /transacciones/conciliar`
- **Request Body (`ConciliacionRequest`):**
```json
{
  "idempotenciaKey": "TX-PAY-2026-998811"
}
```

### 3.3. Consultar Estado de Transacción
- **Método / URL:** `POST /transacciones/consultar`
- **Request Body (`ConciliacionRequest`):**
```json
{
  "idempotenciaKey": "TX-PAY-2026-998811"
}
```

---

## 4. Códigos HTTP y Tratamiento Centralizado de Excepciones

| Escenario | Excepción | Código HTTP |
|---|---|---|
| Parámetros inválidos / incompletos | `MethodArgumentNotValidException`, `ValidacionTransaccionException` | `400 BAD REQUEST` |
| Monto manipulado que no coincide con catálogo | `MontoInvalidoException` | `400 BAD REQUEST` |
| Producto no existe en catálogo | `ProductoNoEncontradoException` | `404 NOT FOUND` |
| Cuenta no existe | `CuentaNoEncontradaException` | `404 NOT FOUND` |
| Cuenta inactiva o bloqueada | `CuentaInactivaException` | `422 UNPROCESSABLE ENTITY` |
| Saldo disponible insuficiente | `SaldoInsuficienteException` | `422 UNPROCESSABLE ENTITY` |
| Solicitud duplicada / Clave ya procesada | `TransaccionDuplicadaException`, `TransaccionEnProcesoException` | `409 CONFLICT` |
| Rechazo explícito del proveedor | `TransaccionRechazadaException` | `422 UNPROCESSABLE ENTITY` |
| Timeout / Incertidumbre de red | `TransaccionIndeterminadaException` | `504 GATEWAY TIMEOUT` |
