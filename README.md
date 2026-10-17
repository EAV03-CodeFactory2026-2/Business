# Business — API REST de Reglas de Negocio y Catálogo

Microservicio independiente extraído del monolito modular `com.udea.Backend` (Spring Modulith).  
Gestiona las **entidades de negocio principales** de la plataforma de reservas: negocios de propietarios y su catálogo de servicios.  
Paquete raíz: `com.udea.business`

## Contexto en la arquitectura

Este servicio corresponde a los módulos `Negocios` y `CatalogoServicios` del monolito original.  
Opera desacoplado de las tablas maestras mediante identificadores lógicos, sin FK directas a los esquemas de **Masters** ni **WebAuth**:

- `propietarioId` → ID de usuario proveniente del claim `sub` del JWT
- `monedaCodigoIso` → código ISO 4217 (ej. `COP`, `USD`) referenciado por valor
- `modalidadId` → ID de modalidad (Presencial, Virtual, Híbrido) referenciado por valor

```
Cliente → POST /api/v1/auth/login  → WebAuth (emite JWT)
Cliente → POST /api/v1/negocios    → Business (usa propietarioId del claim JWT)
Cliente → POST /api/v1/servicios   → Business (usa modalidadId y monedaCodigoIso como claves lógicas)
```

## Tech Stack

| Capa               | Tecnología                                                |
|--------------------|-----------------------------------------------------------|
| Lenguaje           | Java 21                                                   |
| Framework          | Spring Boot 4.1.1 (WebMVC)                                |
| Seguridad          | Spring Security + JJWT 0.12.5 (Resource Server / Stateless Bearer JWT HS256) |
| Base de datos      | PostgreSQL (Supabase) — Spring Data JPA / Hibernate       |
| Base datos pruebas | H2 in-memory (`MODE=PostgreSQL`)                          |
| Validación         | Jakarta Bean Validation                                   |
| Documentación API  | SpringDoc OpenAPI 2.6.0 (Swagger UI en `/swagger-ui.html`)|
| Boilerplate        | Lombok                                                    |
| Build              | Maven (wrapper: `mvnw` / `mvnw.cmd`)                      |
| Calidad & Cobertura| JaCoCo 0.8.11 + SonarCloud                               |
| Contenedores       | Docker multi-stage (eclipse-temurin:21)                   |
| Tests              | JUnit 5 + Mockito + Spring Boot Test                      |

## Estructura del proyecto

```
src/main/java/com/udea/business/
├── BusinessApplication.java             # @SpringBootApplication — punto de entrada
├── HealthController.java                # GET /api/v1/health
│
├── config/
│   └── SwaggerConfig.java              # Configuración OpenAPI con soporte Bearer JWT
│
├── controllers/
│   ├── NegocioController.java           # POST /api/v1/negocios · GET /api/v1/negocios
│   ├── ServicioController.java          # POST /api/v1/servicios · GET /api/v1/servicios/negocio/{id}
│   └── DTOs/
│       ├── NegocioCreateRequest.java    # { nombreNegocio, direccion, telefono, identificacionFiscal, monedaCodigoIso }
│       └── ServicioCreateRequest.java   # { negocioId, modalidadId, nombre, descripcion, duracionMinutos, precio }
│
├── entities/
│   ├── Negocio.java                     # id, propietarioId, nombreNegocio, direccionNegocio,
│   │                                    #   telefono, identificacionFiscal, monedaCodigoIso, estado
│   └── Servicio.java                    # id, negocio(FK), modalidadId, nombre, descripcion,
│                                        #   duracionMinutos, precio, estado ("No Asignado")
│
├── exceptions/
│   └── RecursoDuplicadoException.java   # Lanzada cuando identificacionFiscal o nombre de servicio ya existen
│
├── repositories/
│   ├── NegocioRepository.java           # findByPropietarioId, existsByIdentificacionFiscal
│   └── ServicioRepository.java          # findByNegocio_IdNegocio, existsByNombreAndNegocio
│
├── security/
│   ├── IJwtService.java                 # Contrato: extractUserId, isTokenValid
│   ├── JwtService.java                  # Implementación HS256 · JJWT 0.12.5
│   ├── JwtAuthenticationFilter.java     # OncePerRequestFilter — valida Bearer, inyecta propietarioId como Principal
│   └── SecurityConfig.java             # SecurityFilterChain STATELESS
│
└── services/
    ├── INegocioService.java             # Contrato: crear, listarPorPropietario
    ├── NegocioService.java             # Valida unicidad de identificaciónFiscal, pertenencia al propietario
    ├── IServicioService.java            # Contrato: crear, listarPorNegocio
    └── ServicioService.java            # Valida rol PROPIETARIO, pertenencia del negocio, nombre no duplicado
```

## API Endpoints

| Método | Ruta                              | Seguridad              | Descripción                                         |
|--------|-----------------------------------|------------------------|-----------------------------------------------------|
| `GET`  | `/api/v1/health`                  | Público                | Health check de la API                              |
| `POST` | `/api/v1/servicios/{servicioId}/proveedores` | Bearer JWT (Propietario, dueño del negocio) | Vincula uno o varios proveedores a un servicio |
| `GET`  | `/swagger-ui.html`                | Público                | Documentación interactiva Swagger UI                |
| `GET`  | `/v3/api-docs`                    | Público                | Especificación OpenAPI v3 en JSON                   |

> **Autorización por rol**: la verificación del rol `PROPIETARIO` se realiza en la capa de servicios extrayendo el `propietarioId` del `Principal` (claim `sub` del JWT). Spring Security solo distingue autenticado / no autenticado a nivel de filtro.

### Ejemplo — Registrar negocio

**Request**
```http
POST /api/v1/negocios
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "nombreNegocio": "Peluquería Estilo",
  "direccionNegocio": "Cra 10 # 20-30, Medellín",
  "telefono": "3001234567",
  "identificacionFiscal": "900123456-1",
  "monedaCodigoIso": "COP"
}
```

**Response 201 Created**
```json
{
  "idNegocio": 5,
  "propietarioId": 42,
  "nombreNegocio": "Peluquería Estilo",
  "direccionNegocio": "Cra 10 # 20-30, Medellín",
  "telefono": "3001234567",
  "identificacionFiscal": "900123456-1",
  "monedaCodigoIso": "COP",
  "estado": "Activo"
}
```

### Ejemplo — Crear servicio

**Request**
```http
POST /api/v1/servicios
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "negocioId": 5,
  "modalidadId": 1,
  "nombre": "Corte de cabello clásico",
  "descripcion": "Corte tradicional con tijeras y máquina",
  "duracionMinutos": 30,
  "precio": 25000.00
}
```

**Response 201 Created**
```json
{
  "idServicio": 12,
  "negocioId": 5,
  "modalidadId": 1,
  "nombre": "Corte de cabello clásico",
  "descripcion": "Corte tradicional con tijeras y máquina",
  "duracionMinutos": 30,
  "precio": 25000.00,
  "estado": "No Asignado"
}
```

> Los **recursos físicos** de los negocios (espacios y equipos) se gestionan en **Masters** (`/api/v1/negocios/{negocioId}/recursos`).

> **Rol activo**: si el JWT trae el claim opcional `rolActivo`, el filtro de seguridad solo concede ese rol (y solo si está en `roles`). Así, una cuenta Propietario + Proveedor que opera como Proveedor no puede asignar proveedores. Si el token no trae `rolActivo`, se conservan todos los roles.

### Ejemplo — Asignar proveedores a un servicio

Los ids se obtienen del directorio de proveedores de **Masters** (`GET /api/v1/proveedores/directorio`).

```http
POST /api/v1/servicios/12/proveedores
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{ "proveedorIds": [11, 13] }
```

**Response 201 Created**
```json
{
  "mensaje": "Servicio asignado a 2 proveedor(es)",
  "idServicio": 12,
  "estadoServicio": "Asignado",
  "proveedoresAsignados": [
    { "idUsuario": 11, "nombre": "Carlos", "apellido": "Ruiz", "especialidad": "Estilista", "tipoDocumento": "CC", "numeroDocumento": "123456" },
    { "idUsuario": 13, "nombre": "Diego", "apellido": "Salas", "especialidad": "Barbero", "tipoDocumento": "CC", "numeroDocumento": "777" }
  ],
  "totalProveedores": 2
}
```

La operación es transaccional: si algún proveedor no es válido o ya estaba vinculado, no se guarda ninguno.

| Código | Cuándo | `error` |
|--------|--------|---------|
| `400` | Lista vacía o ausente | `Selecciona al menos un proveedor para asignar.` |
| `400` | Proveedor repetido en la solicitud | `No repitas el mismo proveedor en la solicitud.` |
| `400` | Algún id no es un proveedor activo | `Estos usuarios no son proveedores activos de la plataforma: 12, 15.` |
| `403` | El rol activo no es Propietario | `Solo un Propietario puede asignar proveedores a los servicios.` |
| `403` | El servicio es de un negocio ajeno | `No tienes permiso para asignar proveedores a este servicio.` |
| `404` | El servicio no existe | `Servicio no encontrado.` |
| `409` | Algún proveedor ya está vinculado | `Ya está vinculado a este servicio: Carlos Ruiz (CC 123456).` |

## Modelo de datos

### Negocio

| Campo                | Tipo           | Restricciones           | Descripción                                  |
|----------------------|----------------|-------------------------|----------------------------------------------|
| `id_negocio`         | `INTEGER`      | PK, auto-generado       | Identificador interno del negocio            |
| `propietario_id`     | `INTEGER`      | NOT NULL                | ID del usuario propietario (claim JWT `sub`) |
| `nombre_negocio`     | `VARCHAR(250)` | NOT NULL                | Nombre comercial del negocio                 |
| `direccion_negocio`  | `VARCHAR(250)` | NOT NULL                | Dirección física del negocio                 |
| `telefono`           | `VARCHAR(20)`  | opcional                | Teléfono de contacto                         |
| `identificacion_fiscal` | `VARCHAR(20)` | NOT NULL, UNIQUE     | NIT o identificación tributaria              |
| `moneda_codigo_iso`  | `VARCHAR(3)`   | NOT NULL                | Código ISO 4217 de la moneda de operación    |
| `estado`             | `VARCHAR(20)`  | NOT NULL, default `Activo` | Estado del negocio                        |

### Servicio

| Campo               | Tipo            | Restricciones                   | Descripción                                       |
|---------------------|-----------------|---------------------------------|---------------------------------------------------|
| `id_servicio`       | `INTEGER`       | PK, auto-generado               | Identificador interno del servicio                |
| `negocio_id`        | `INTEGER`       | NOT NULL, FK → negocio          | Negocio al que pertenece el servicio              |
| `modalidad_id`      | `INTEGER`       | NOT NULL                        | ID de modalidad (referencia lógica a Masters)     |
| `nombre`            | `VARCHAR(100)`  | NOT NULL                        | Nombre del servicio (único por negocio)           |
| `descripcion`       | `VARCHAR(500)`  | NOT NULL                        | Descripción detallada del servicio                |
| `duracion_minutos`  | `INTEGER`       | NOT NULL                        | Duración estimada en minutos                      |
| `precio`            | `DECIMAL(12,2)` | NOT NULL                        | Precio del servicio en la moneda del negocio      |
| `estado`            | `VARCHAR(20)`   | NOT NULL, default `No Asignado` | Estado del servicio                               |

### ServicioProveedor (`servicio_proveedor`)

| Campo                  | Tipo          | Restricciones                | Descripción                              |
|------------------------|---------------|------------------------------|------------------------------------------|
| `id_servicio_proveedor`| `INTEGER`     | PK, auto-generado            | Identificador de la vinculación          |
| `servicio_id`          | `INTEGER`     | NOT NULL, FK → servicio      | Servicio vinculado                       |
| `proveedor_id`         | `INTEGER`     | NOT NULL, FK → perfil_proveedor | Proveedor (id de usuario) que presta el servicio |
| `fecha_asignacion`     | `TIMESTAMPTZ` | NOT NULL                     | Momento de la vinculación                |

Restricción única `(servicio_id, proveedor_id)`: un proveedor no puede quedar vinculado dos veces al mismo servicio.

> Business lee `usuario`, `rol`, `asignacion_rol`, `tipo_documento` y `perfil_proveedor` como entidades de **solo lectura** (`@Immutable`), únicamente para validar que los ids asignados sean proveedores activos. Esas tablas las administra Masters, que también ofrece el directorio de búsqueda.

## Configuración y variables de entorno

Copia `.env.example` como `.env` en la raíz del proyecto y completa los valores:

```bash
cp .env.example .env
```

| Variable            | Descripción                              | Valor por defecto / Ejemplo            |
|---------------------|------------------------------------------|----------------------------------------|
| `DB_HOST`           | Host de PostgreSQL (Supabase)            | `aws-0-...pooler.supabase.com`         |
| `DB_PORT`           | Puerto de base de datos                  | `5432`                                 |
| `DB_NAME`           | Nombre de la base de datos               | `postgres`                             |
| `DB_USERNAME`       | Usuario de base de datos                 | `postgres.xxx`                         |
| `DB_PASSWORD`       | Contraseña de base de datos              | `*****`                                |
| `DB_SSLMODE`        | Modo SSL                                 | `require`                              |
| `JPA_DDL_AUTO`      | Estrategia DDL de Hibernate              | `update`                               |
| `JWT_SECRET`        | Clave secreta Base64 (mín. 256 bits)     | `*****`                                |
| `JWT_EXPIRATION_MS` | Tiempo de vida del JWT en ms             | `86400000` (24 horas)                  |
| `PORT`              | Puerto HTTP del servidor                 | `8083`                                 |

> **⚠️ IMPORTANTE**: Jamás commitear secretos en texto plano. En producción (Render, CI) las variables se inyectan en el entorno.  
> La clave `JWT_SECRET` debe ser **idéntica** en todos los servicios del ecosistema para que la validación local del JWT funcione.

## Comandos de desarrollo

```bash
# Compilar sin tests
./mvnw clean compile          # Linux / macOS
mvnw.cmd clean compile        # Windows

# Ejecutar la aplicación (puerto 8083 por defecto)
./mvnw spring-boot:run
mvnw.cmd spring-boot:run      # Windows

# Empaquetar JAR (sin tests)
./mvnw clean package -DskipTests

# Ejecutar todos los tests (con reporte JaCoCo)
./mvnw test
mvnw.cmd test                 # Windows

# Ejecutar un test específico
./mvnw test -Dtest=NegocioServiceTest

# Ejecutar un paquete completo
./mvnw test -Dtest="com.udea.business.services.**"

# Con output detallado en consola
./mvnw test -Dsurefire.useFile=false

# Docker build & run
docker build -t business .
docker run -p 8083:8083 --env-file .env business
```

## Testing

- **JUnit 5** + **Mockito** (`@ExtendWith(MockitoExtension.class)`)
- **H2 en memoria** configurado en `src/test/resources/application.properties` con modo PostgreSQL (`create-drop`), garantizando tests herméticos sin dependencia de la base de datos de desarrollo.
- Clases de test ubicadas en `src/test/java/com/udea/business/` replicando la jerarquía de paquetes de producción.
- Convenciones de nombres: sufijo `Test` (ej. `NegocioServiceTest`, `ServicioControllerTest`); métodos en camelCase descriptivo sin prefijo `test` (ej. `crearNegocioExitoso`, `debeRetornar409CuandoIdentificacionFiscalDuplicada`).
- Smoke test de contexto (`BusinessApplicationTests#contextLoads`) siempre presente.
- Tests unitarios de servicios con Mockito simulando repositorios.
- Tests de controladores con `@WebMvcTest` o pruebas de validación de DTOs.

## Reglas de negocio

| Regla | Descripción |
|-------|-------------|
| Unicidad de `identificacionFiscal` | Un NIT/identificación fiscal no puede estar registrado en más de un negocio. Lanza `RecursoDuplicadoException`. |
| Pertenencia del negocio | Solo el propietario autenticado (claim `sub` del JWT) puede crear servicios en sus propios negocios. |
| Unicidad del nombre de servicio | Dentro de un mismo negocio, el nombre de un servicio debe ser único. Lanza `RecursoDuplicadoException`. |
| Estado del servicio | Derivado: `No Asignado` con 0 proveedores, `Asignado` con 1 o más. |
| Asignación de proveedores | Transaccional (todos o ninguno); sin duplicados por servicio; solo el Propietario dueño del negocio. |
| Estado inicial del servicio | Todo servicio se crea con estado `"No Asignado"` hasta ser asignado a un horario de disponibilidad. |
| Estado inicial del negocio | Todo negocio se crea con estado `"Activo"`. |

## Convenciones de código

- **Paquetes**: camelCase (`config`, `controllers`, `entities`, `exceptions`, `repositories`, `security`, `services`).
- **Clases**: PascalCase. Interfaces de servicio con prefijo `I` (`INegocioService`, `IServicioService`, `IJwtService`).
- **Métodos y variables**: camelCase. Constantes: `UPPER_SNAKE_CASE`.
- **Lombok**: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` en entidades y DTOs.
- **Idioma**: español para nombres de dominio, Javadoc y mensajes de error.
- Endpoints documentados con `@Operation`, `@ApiResponse` y `@Tag` (SpringDoc/OpenAPI).
- Validación de entrada estricta con Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@NotNull`, `@Size`, `@Pattern`) en DTOs bajo `controllers/DTOs`.
