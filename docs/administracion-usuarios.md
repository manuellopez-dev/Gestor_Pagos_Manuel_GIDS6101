# Módulo — Administración de Usuarios

API REST para la administración de usuarios desarrollada con **Java 17** y **Spring Boot 3.3.6**, siguiendo arquitectura por capas, persistencia en **PostgreSQL** mediante **Spring Data JPA**, especificaciones dinámicas y manejo centralizado de excepciones y validaciones.

---

## 1. Cumplimiento de Restricciones Obligatorias

| Restricción | Implementación |
|---|---|
| **No `@PathVariable`** | Todos los endpoints reciben identificadores o criterios mediante objetos DTO en `@RequestBody`. |
| **No `@RequestParam`** | No se utiliza ningún parámetro de consulta por URL. |
| **Búsqueda dinámica unificada** | Endpoint único `POST /usuarios/buscar` que procesa `UsuarioFiltroRequest` y combina predicados mediante `JpaSpecificationExecutor`. |
| **Validaciones con `@Valid`** | Todos los métodos del controlador aplican `@Valid` a los DTOs de entrada. |
| **Bean Validation exhaustivo** | Uso de `@NotNull`, `@NotBlank`, `@NotEmpty`, `@Email`, `@Size` y `@Pattern`. |
| **`@RestControllerAdvice`** | `GlobalExceptionHandler` intercepta validaciones, excepciones de negocio y fallos del sistema sin exponer trazas técnicas ni SQL. |
| **Sin `try-catch` en Controller** | El controlador únicamente delega al servicio; las excepciones escalan directamente al manejador global. |
| **Desacoplamiento Entity / DTO** | Mapeo mediante **MapStruct** (`UsuarioMapper`). No se reciben ni retornan entidades JPA en la API. |
| **Campos controlados por servidor** | `id`, `fechaCreacion`, `fechaActualizacion` y `estado` por defecto son gestionados exclusivamente por JPA/Service. |

---

## 2. Endpoints y Contratos

Todos los endpoints usan `Content-Type: application/json` y responden en formato JSON uniforme `ApiResponse<T>`.

### 2.1. Crear Usuario
- **Método / URL:** `POST /usuarios`
- **Código de éxito:** `201 CREATED`
- **Request Body (`UsuarioCrearRequest`):**
```json
{
  "username": "juanperez",
  "nombre": "Juan",
  "apellidos": "Perez Lopez",
  "email": "juan.perez@example.com",
  "telefono": "5512345678",
  "roles": ["ROLE_USER"]
}
```

### 2.2. Obtener Usuario por ID
- **Método / URL:** `POST /usuarios/obtener`
- **Código de éxito:** `200 OK`
- **Request Body (`UsuarioIdRequest`):**
```json
{
  "id": 1
}
```

### 2.3. Búsqueda Dinámica con Filtros Combinados
- **Método / URL:** `POST /usuarios/buscar`
- **Código de éxito:** `200 OK`
- **Request Body (`UsuarioFiltroRequest` - todos los campos son opcionales):**
```json
{
  "username": "juan",
  "nombre": "Juan",
  "apellidos": "Perez",
  "email": "example.com",
  "telefono": "551234",
  "estado": "ACTIVO",
  "rol": "ROLE_USER",
  "fechaInicio": "2026-01-01T00:00:00",
  "fechaFin": "2026-12-31T23:59:59"
}
```

### 2.4. Actualizar Usuario
- **Método / URL:** `PUT /usuarios`
- **Código de éxito:** `200 OK`
- **Request Body (`UsuarioActualizarRequest`):**
```json
{
  "id": 1,
  "nombre": "Juan Carlos",
  "apellidos": "Perez Lopez",
  "email": "juancarlos@example.com",
  "telefono": "5587654321",
  "estado": "ACTIVO",
  "roles": ["ROLE_USER", "ROLE_ADMIN"]
}
```

### 2.5. Eliminar Usuario
- **Método / URL:** `DELETE /usuarios` (también disponible como `POST /usuarios/eliminar`)
- **Código de éxito:** `200 OK`
- **Request Body (`UsuarioIdRequest`):**
```json
{
  "id": 1
}
```

---

## 3. Estructura Uniforme de Respuestas

### Respuesta Exitosa
```json
{
  "exito": true,
  "codigo": 200,
  "mensaje": "Consulta realizada exitosamente",
  "timestamp": "2026-11-10 18:50:00",
  "datos": { ... }
}
```

### Respuesta de Error (ejemplo: validación)
```json
{
  "exito": false,
  "codigo": 400,
  "mensaje": "Error en las validaciones de entrada",
  "timestamp": "2026-11-10 18:50:00",
  "errores": [
    "username: El nombre de usuario es obligatorio",
    "email: El formato del correo electrónico no es válido"
  ]
}
```

---

## 4. Códigos de Estado y Manejo de Excepciones

| Excepción | Código HTTP | Descripción |
|---|---|---|
| `MethodArgumentNotValidException` | `400 BAD REQUEST` | Falla alguna validación Bean Validation con lista de campos. |
| `ReglaNegocioException` | `400 BAD REQUEST` | Inconsistencia lógica (ej. `fechaInicio > fechaFin`). |
| `HttpMessageNotReadableException` | `400 BAD REQUEST` | JSON malformado o tipos no convertibles. |
| `UsuarioNoEncontradoException` | `404 NOT FOUND` | ID de usuario no existente en BD. |
| `UsernameDuplicadoException` | `409 CONFLICT` | Nombre de usuario ya registrado. |
| `CorreoDuplicadoException` | `409 CONFLICT` | Correo electrónico ya registrado. |
| `Exception` | `500 INTERNAL SERVER ERROR` | Error no controlado sin filtrar trazas ni SQL. |
