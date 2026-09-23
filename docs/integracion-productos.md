# Integración — Productos (Sincronización + Consulta)

Consumo del sistema externo `GET /sistema/service/getProductList.do` (XML) con autenticación `Bearer Token`,
persistencia en **MongoDB** y servicio de consulta para consumidores externos.

## Arquitectura / Flujo (pizarra)

```
[Cron 06:00] ──▶ [Client / SincronizacionProductoService]
                     │ 1. Api/Endpoint (Feign: GestoPagoProductoClient)
                     ▼
                [Get Product XML ← GetProductoXmlService]  (parseo JAXB)
                     │
                     ▼
                [MongoDB ← ProductoRepository]

[Consumidor] ──▶ [Servicio "Consultar" / ConsultaProductoService ──▶ rombo de decisión]
                     │  datos vacíos o vencidos (> N horas) → re-sincroniza
                     │  datos vigentes                    → devuelve desde MongoDB
                     ▼
                [ConsultaProductoController  GET /productos/consulta]
```

| Capa | Clase | Paquete |
|------|-------|---------|
| Controlador | `ProductoConsultaController` | `controller` |
| Cliente / API | `GestoPagoProductoClient`, `ProductoFeignConfig` | `client` |
| Parser XML | `GetProductoXmlService` / Impl | `service(.Impl)` |
| Sincronización (cron) | `SincronizacionProductoService` / Impl | `service(.Impl)` |
| Consulta / validación | `ConsultaProductoService` / Impl | `service(.Impl)` |
| Persistencia | `entity.producto.Producto`, `repositorys.producto.ProductoRepository`, `config.MongoConfig` | `entity/repositorys/config` |
| Modelos | `model.xml.ProductoXml/ProductosXml`, `model.xml` | `model.xml` |
| Mapeo | `mapper.ProductoMapper` (MapStruct) | `mapper` |
| Errores | `IntegracionException`, `TipoErrorIntegracion`, `IntegracionErrorMapper`, `GlobalExceptionHandler` | `exception`, `config` |

## Configuración de propiedades

```properties
gestopago.productos.url=${gestopago.auth.url:}         # URL base
gestopago.productos.token=                             # Bearer Token (no hardcodeado)
gestopago.productos.connect-timeout-ms=5000
gestopago.productos.read-timeout-ms=10000
gestopago.productos.cron=0 0 6 * * *                    # disparador 06:00 AM
gestopago.productos.consulta.max-antiguedad-horas=24    # frescura p/ decisión
spring.data.mongodb.uri=${MONGO_URI:mongodb://localhost:27017/gestopago}
spring.data.mongodb.database=${MONGO_DB:gestopago}
```

El token se inyecta por configuración (`@ConfigurationProperties` → `RequestInterceptor` Feign); nunca en el código.

## Manejo de errores

`IntegracionErrorMapper` (compartido por todos los servicios) clasifica los fallos del cliente Feign:

| Escenario | Tipo de error | HTTP |
|-----------|---------------|------|
| Timeout (SocketTimeout) | `TIMEOUT` | 504 |
| Error de conexión | `ERROR_COMUNICACION` | 502 |
| HTTP 401/403 | `ERROR_AUTENTICACION` | 401 |
| HTTP 4xx/5xx, XML vacío/inalidado | `RESPUESTA_NO_EXITOSA` | 502 |

El `@Scheduled` de la sincronización atrapa `IntegracionException` para no romper el ciclo programado;
el resto de flujos propagan hacia `GlobalExceptionHandler`.

## Registro y monitoreo

Logs de inicio/fin por invocación, conteo de productos procesados y errores de integración con mensajes propios
(sin cuerpo de respuesta ni encabezados → no se exponen tokens ni datos sensibles).

## Pruebas (18)

- `ProductoServiceImplTest` (6): éxito parseando XML, 401, 403, timeout, conexión, HTTP 5xx.
- `SincronizacionProductoServiceImplTest` (5): guardado nuevo, actualización conservando id, 401, timeout, conexión.
- `ConsultaProductoServiceImplTest` (4): sin datos → sincroniza; vigentes → sin sincronizar; vencidos → sincroniza; sin fecha → sincroniza.
- `GetProductoXmlServiceImplTest` (3): XML válido, XML vacío, XML inválido.

## Decisiones técnicas

1. **XML real**: el endpoint devuelve XML; el módulo "Get Product XML" lo parsea con **JAXB** (jaxb-runtime ya existía como dependencia).
2. **Feign** con `produces = application/xml`; retorno `String` crudo que alimenta el parser.
3. **Cron configurable**: `@Scheduled(cron="...")` (06:00 AM por default) sobre el servicio de sincronización.
4. **MongoDB**: nueva capacidad `spring-boot-starter-data-mongodb` + `@EnableMongoRepositories`; upsert por `idProducto`.
5. **Rombo de decisión**: `ConsultaProductoServiceImpl` valida datos vacíos o vencidos (`max-antiguedad-horas`) y dispara la re-sincronización reutilizando el mismo servicio del cron (sin duplicar lógica).
6. **Mapeo**: MapStruct (`ProductoMapper`) para `ProductoXml → Producto → ProductoDTO`; consistente con `GestoPagoTokenMapper`.
7. **Sin duplicación**: la clasificación de errores se extrajo a `IntegracionErrorMapper` usado por `ProductoService` y la sincronización.
8. Los campos del XML/DTO siguen la estructura `<productos><producto>…</producto></productos>`; si el contrato real difiere, ajustar los modelos XML (`model.xml`) — `@JsonIgnoreProperties`/JAXB toleran extraños.