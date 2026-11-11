# Gestor de Pagos y Servicios — API REST

[![Java](https://img.shields.io/badge/Java-17-orange.svg?logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.6-brightgreen.svg?logo=springboot)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-9.6-blue.svg?logo=gradle)](https://gradle.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-42.7.3-336791.svg?logo=postgresql)](https://www.postgresql.org/)
[![MongoDB](https://img.shields.io/badge/MongoDB-Persistencia%20Catálogo-47A248.svg?logo=mongodb)](https://www.mongodb.com/)
[![Tests](https://img.shields.io/badge/Tests-76%20Passed-success.svg)](https://junit.org/junit5/)

Plataforma backend empresarial desarrollada con **Java 17** y **Spring Boot 3.3.6** para la administración de usuarios, sincronización inteligente de catálogos y procesamiento transaccional financiero integrado con la pasarela de servicios **GestoPago**.

Diseñada bajo principios **SOLID**, arquitectura limpia en capas, control estricto de concurrencia, idempotencia y protocolos criptográficos simétricos (**AES-256**) y firmas digitales (**HMAC-SHA256**).

---

## 📑 Tabla de Contenidos

- [Características Principales](#-características-principales)
- [Arquitectura del Sistema](#-arquitectura-del-sistema)
- [Flujo de Procesamiento Transaccional (4 Bloques)](#-flujo-de-procesamiento-transaccional-4-bloques)
- [Estrategia de Integridad Financiera](#-estrategia-de-integridad-financiera)
- [Tecnologías Utilizadas](#-tecnologías-utilizadas)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Configuración y Variables de Entorno](#-configuración-y-variables-de-entorno)
- [Catálogo de Endpoints](#-catálogo-de-endpoints)
- [Ejecución y Pruebas](#-ejecución-y-pruebas)

---

## 🚀 Características Principales

1. **Administración de Usuarios:**
   - Cumplimiento estricto de restricciones REST (sin `@PathVariable` ni `@RequestParam`, recepción exclusiva por Request DTO con `@RequestBody`).
   - Validaciones declarativas exhaustivas con Bean Validation (`@NotNull`, `@NotBlank`, `@NotEmpty`, `@Email`, `@Size`, `@Pattern`).
   - Búsqueda dinámica multicriterio combinada mediante un único endpoint usando `JpaSpecificationExecutor`.
   - Protección de campos controlados por el servidor (`id`, auditoría, fechas, estados).

2. **Catálogo de Productos y Sincronización (GestoPago):**
   - Consumo del endpoint `getProductList.do` (XML) autenticado con Token Bearer mediante Spring Cloud OpenFeign.
   - Parseo XML de alto rendimiento con **JAXB**.
   - Almacenamiento no relacional en **MongoDB** con estrategia de upsert.
   - Sincronización automática programada (`@Scheduled` cron a las 06:00 AM) y política de frescura bajo demanda (re-sincronización si la antigüedad supera 24 horas).

3. **Autenticación Segura y Rotación de Tokens:**
   - Autenticación automatizada ante GestoPago con renovación programada cada hora.
   - Persistencia y consulta en base de datos PostgreSQL (`gestopago_tokens`).
   - Inyección segura de credenciales en memoria vía `RequestInterceptor` (sin exposición en logs ni respuestas HTTP).

4. **Motor Transaccional y Pagos de Servicios (`sendTx.do`):**
   - Arquitectura en 4 bloques: Validación inicial (`Pass Seguro` + `Cuenta Activa -> Saldo`), construcción de Payload, Cifrado/Firma y Envío/Liquidación.
   - Control de concurrencia mediante bloqueo pesimista (`PESSIMISTIC_WRITE`).
   - Estrategia de descuento en dos fases (*Hold & Settle*).
   - Tolerancia a fallos y manejo de incertidumbre: Timeouts catalogados como `POR_CONCILIAR` sin reversión prematura ni cobro doble.
   - Servicio de conciliación asíncrona mediante `queryTx.do`.

---

## 🏛 Arquitectura del Sistema

```
                      ┌─────────────────────────────────────────┐
                      │            Cliente / Frontend           │
                      └────────────────────┬────────────────────┘
                                           │ HTTP / JSON
                                           ▼
                      ┌─────────────────────────────────────────┐
                      │           Controladores REST            │
                      │  (Usuario, Producto, Transaccion)       │
                      └────────────────────┬────────────────────┘
                                           │ DTOs / @Valid
                                           ▼
                      ┌─────────────────────────────────────────┐
                      │            Capa de Servicios            │
                      │   (Lógica, Crypto AES/HMAC, Orquestador)│
                      └───────┬─────────────────────────┬───────┘
                              │                         │
            ┌─────────────────┴─────────┐     ┌─────────┴─────────────────┐
            │   Persistencia Híbrida    │     │    Integración Externa    │
            ├──────────────┬────────────┤     ├───────────────────────────┤
            │  PostgreSQL  │  MongoDB   │     │  Feign Client GestoPago   │
            │  (JPA / Fly) │  (Catálogo)│     │  (Auth, Products, SendTx) │
            └──────────────┴────────────┘     └───────────────────────────┘
```

---

## 🔄 Flujo de Procesamiento Transaccional (4 Bloques)

```
[1. Bloque Inicial: Entrada y Validaciones]
  POST /transacciones/venta
  ├── NumCuenta, IdServicio, idpermiso, Precio, Distribuidor, Telefono/Referencia, Bandera, Pin
  │
  ├──▶ [Pass Seguro]: Validación criptográfica del PIN de la cuenta.
  │
  └──▶ [Cuenta Activa ──▶ Saldo]: Bloqueo pesimista en PostgreSQL (PESSIMISTIC_WRITE).
            ├── Valida estado ACTIVA y saldoDisponible >= Precio.
            └── Reserva temporal de fondos en saldo_retenido.
                  │
                  ▼
[2. Bloque Central: Payload Canónico]
  Estructuración de datos:
  IdCuenta, IDServicio, Telefono/Referencia, hora local, Monto Pago, UPE, Unidad
                  │
                  ▼
[3. Cifrado y Autenticación]
  ├── Firmar: Generación de firma digital HMAC-SHA256.
  ├── AES: Cifrado simétrico AES-256 (CBC/PKCS5Padding con IV aleatorio).
  └── Obtener Token & BD: Registro de la transacción con estado EN_PROCESO.
                  │
                  ▼
[4. Envío y Respuesta: Post Send Tx ──▶ Código 01 / Descontar Saldo]
  Invocación a GestoPago sendTx.do con Bearer Token
  │
  ├──▶ Código 01 (Éxito):
  │     ├── Descontar Saldo definitivo: saldo -= monto, saldoRetenido -= monto.
  │     ├── Estado: EXITOSA con Folio y Autorización.
  │     └── HTTP 200 OK.
  │
  ├──▶ Rechazo del Proveedor:
  │     ├── Libera reserva: saldoRetenido -= monto.
  │     ├── Estado: FALLIDA.
  │     └── HTTP 422 Unprocessable Entity.
  │
  └──▶ Timeout / Incertidumbre de red:
        ├── Saldo permanece retenido (protección anti doble cobro).
        ├── Estado: POR_CONCILIAR.
        └── HTTP 504 Gateway Timeout (conciliable vía POST /transacciones/conciliar).
```

---

## 🛡 Estrategia de Integridad Financiera

* **Reserva en Dos Fases (*Hold & Settle*):** La reserva de saldo se procesa en una transacción de base de datos corta. El bloqueo de fila en PostgreSQL se libera antes de realizar la llamada HTTP a GestoPago, evitando la saturación del pool de conexiones durante operaciones de red.
* **Idempotencia Garantizada:** Cada transacción exige o genera una clave de idempotencia única. Peticiones repetidas retornan el resultado consolidado sin duplicar consumos de saldo ni llamadas al proveedor.
* **Prevención de Carrera de Saldos:** El uso de `SELECT ... FOR UPDATE` a nivel de base de datos asegura que solicitudes simultáneas para una misma cuenta se serialicen correctamente, imposibilitando sobregiros o dobles gastos.

---

## 🛠 Tecnologías Utilizadas

* **Lenguaje:** Java 17
* **Framework:** Spring Boot 3.3.6 (Web, Data JPA, Data MongoDB, Validation, Scheduling, Actuator)
* **Cloud & HTTP Clients:** Spring Cloud OpenFeign 4.1.4, Apache HttpClient5
* **Bases de Datos:**
  * PostgreSQL 15+ (conector oficial 42.7.3)
  * MongoDB (persistencia documental de catálogos)
* **Migraciones de Base de Datos:** Flyway (esquema `app` configurado)
* **Mapeo y Serialización:** MapStruct 1.6.3, Lombok 1.18.34, JAXB (Jakarta XML Bind)
* **Seguridad Criptográfica:** AES-256/CBC con IV dinámico, HMAC-SHA256, Java JWT, JJWT
* **Pruebas:** JUnit 5, Mockito, MockMvc, AssertJ

---

## 📁 Estructura del Proyecto

```
src/
├── main/
│   ├── java/com/proyecto/servicios/
│   │   ├── client/          # Clientes OpenFeign (Auth, Productos, Transacciones)
│   │   ├── config/          # Configuraciones (DB, Mongo, Flyway, GlobalExceptionHandler)
│   │   ├── controller/      # Controladores REST (Usuario, Producto, Transaccion)
│   │   ├── entity/          # Entidades JPA y MongoDB (usuario, cuenta, transaccion, producto)
│   │   ├── exception/       # Jerarquía de excepciones de negocio y mapeador de integración
│   │   ├── mapper/          # Mappers MapStruct (Usuario, Producto, Transaccion)
│   │   ├── model/           # DTOs Request/Response, envoltorio uniforme ApiResponse<T>, modelos XML
│   │   ├── repositorys/     # Repositorios JPA y Mongo con Specifications dinámicas
│   │   └── service/         # Interfaces e implementaciones de servicios de negocio
│   └── resources/
│       ├── application.properties
│       └── db/migration/    # Scripts SQL versionados de Flyway (V1, V2, V3, V4)
└── test/
    └── java/com/proyecto/servicios/ # 76 Pruebas Unitarias y de Integración
```

---

## ⚙ Configuración y Variables de Entorno

Puedes configurar el sistema mediante variables de entorno o sobrescribiendo `application.properties`:

| Propiedad | Variable de Entorno | Descripción | Valor por Defecto |
|---|---|---|---|
| `spring.datasource.url` | `DB_URL` | URL de conexión a PostgreSQL | `jdbc:postgresql://...` |
| `spring.datasource.username` | `DB_USER` | Usuario de PostgreSQL | `${DB_USER}` |
| `spring.datasource.password` | `DB_PASSWORD` | Contraseña de PostgreSQL | `${DB_PASSWORD}` |
| `spring.data.mongodb.uri` | `MONGO_URI` | URI de conexión a MongoDB | `mongodb://...` |
| `gestopago.auth.url` | - | URL base de la pasarela GestoPago | `https://gestopago.portalventas.net` |
| `gestopago.seguridad.aes-secret-key` | `AES_SECRET_KEY` | Clave secreta para cifrado AES-256 | (Clave por defecto segura) |
| `gestopago.seguridad.firma-secret-key` | `FIRMA_SECRET_KEY` | Clave secreta para firma HMAC-SHA256 | (Clave por defecto segura) |

---

## 📡 Catálogo de Endpoints

Todas las respuestas exitosas y de error siguen el modelo uniforme `ApiResponse<T>`:

```json
{
  "exito": true,
  "codigo": 200,
  "mensaje": "Operación ejecutada exitosamente",
  "timestamp": "2026-11-10 19:30:00",
  "datos": { ... }
}
```

### 1. Transacciones y Pagos de Servicios

| Método | Endpoint | Descripción | Body Requerido | Código Éxito |
|---|---|---|---|---|
| `POST` | `/transacciones/venta` | Flujo de 4 bloques (Pass Seguro, Payload, AES/Firma, Código 01) | `TransaccionVentaRequest` | `200 OK` |
| `POST` | `/transacciones` | Procesamiento estándar de transacción | `TransaccionRequest` | `200 OK` |
| `POST` | `/transacciones/conciliar` | Conciliación de transacciones en estado `POR_CONCILIAR` | `ConciliacionRequest` | `200 OK` |
| `POST` | `/transacciones/consultar` | Consulta de transacción por clave de idempotencia | `ConciliacionRequest` | `200 OK` |

#### Ejemplo de Petición (`POST /transacciones/venta`):
```json
{
  "numCuenta": "CTA-001",
  "idServicio": "56",
  "idpermiso": "PERM-ADMIN",
  "precio": 100.50,
  "distribuidor": "DIST-83",
  "telefono": "5544332211",
  "bandera": "VENTA_DIRECTA",
  "pin": "1234",
  "idProducto": "185",
  "upe": "UPE-56",
  "unidad": "1"
}
```

### 2. Administración de Usuarios

| Método | Endpoint | Descripción | Body Requerido | Código Éxito |
|---|---|---|---|---|
| `POST` | `/usuarios` | Registro de nuevo usuario | `UsuarioCrearRequest` | `201 CREATED` |
| `POST` | `/usuarios/obtener` | Consulta de usuario por ID (sin `@PathVariable`) | `UsuarioIdRequest` | `200 OK` |
| `POST` | `/usuarios/buscar` | Búsqueda multicriterio combinable | `UsuarioFiltroRequest` | `200 OK` |
| `PUT` | `/usuarios` | Actualización de usuario | `UsuarioActualizarRequest` | `200 OK` |
| `DELETE`| `/usuarios` | Eliminación de usuario | `UsuarioIdRequest` | `200 OK` |

### 3. Catálogo y Productos

| Método | Endpoint | Descripción | Código Éxito |
|---|---|---|---|
| `GET` | `/productos` | Obtiene productos del proveedor externo en crudo | `200 OK` |
| `GET` | `/productos/consulta` | Consulta inteligente en MongoDB con re-sincronización automática | `200 OK` |

---

## 🧪 Ejecución y Pruebas

### Requisitos Previos
* Java JDK 17 instalado (`java -version`).
* Acceso a internet o credenciales configuradas para PostgreSQL y MongoDB.

### Compilar el Proyecto
```bash
./gradlew clean compileJava
```

### Ejecutar la Suite de Pruebas Automatizadas
El proyecto cuenta con **76 pruebas unitarias y de integración** automatizadas:

```bash
./gradlew test
```

### Iniciar la Aplicación en Local
```bash
./gradlew bootRun
```

La aplicación arrancará por defecto en el puerto `8081` (o puerto configurado). La documentación interactiva de OpenAPI / Swagger se encuentra disponible en:
`http://localhost:8081/swagger-ui.html`

---

## 📄 Licencia

Este proyecto se encuentra bajo los lineamientos y especificaciones del curso y la organización. Todos los derechos reservados.
