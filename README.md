# Business ÔÇö API REST de Reglas de Negocio y Cat├ílogo

Microservicio independiente extra├¡do del monolito modular `com.udea.Backend` (Spring Modulith).  
Gestiona las **entidades de negocio principales** de la plataforma de reservas: negocios de propietarios y su cat├ílogo de servicios.  
Paquete ra├¡z: `com.udea.business`

## Contexto en la arquitectura

Este servicio corresponde a los m├│dulos `Negocios` y `CatalogoServicios` del monolito original.  
Opera desacoplado de las tablas maestras mediante identificadores l├│gicos, sin FK directas a los esquemas de **Masters** ni **WebAuth**:

- `propietarioId` ÔåÆ ID de usuario proveniente del claim `sub` del JWT
- `monedaCodigoIso` ÔåÆ c├│digo ISO 4217 (ej. `COP`, `USD`) referenciado por valor
- `modalidadId` ÔåÆ ID de modalidad (Presencial, Virtual, H├¡brido) referenciado por valor

```
Cliente ÔåÆ POST /api/v1/auth/login  ÔåÆ WebAuth (emite JWT)
Cliente ÔåÆ POST /api/v1/negocios    ÔåÆ Business (usa propietarioId del claim JWT)
Cliente ÔåÆ POST /api/v1/servicios   ÔåÆ Business (usa modalidadId y monedaCodigoIso como claves l├│gicas)
```

## Tech Stack

| Capa               | Tecnolog├¡a                                                |
|--------------------|-----------------------------------------------------------|
| Lenguaje           | Java 21                                                   |
| Framework          | Spring Boot 4.1.1 (WebMVC)                                |
| Seguridad          | Spring Security + JJWT 0.12.5 (Resource Server / Stateless Bearer JWT HS256) |
| Base de datos      | PostgreSQL (Supabase) ÔÇö Spring Data JPA / Hibernate       |
| Base datos pruebas | H2 in-memory (`MODE=PostgreSQL`)                          |
| Validaci├│n         | Jakarta Bean Validation                                   |
| Documentaci├│n API  | SpringDoc OpenAPI 2.6.0 (Swagger UI en `/swagger-ui.html`)|
| Boilerplate        | Lombok                                                    |
| Build              | Maven (wrapper: `mvnw` / `mvnw.cmd`)                      |
| Calidad & Cobertura| JaCoCo 0.8.11 + SonarCloud                               |
| Contenedores       | Docker multi-stage (eclipse-temurin:21)                   |
| Tests              | JUnit 5 + Mockito + Spring Boot Test                      |

## Estructura del proyecto

```
src/main/java/com/udea/business/
Ôö£ÔöÇÔöÇ BusinessApplication.java             # @SpringBootApplication ÔÇö punto de entrada
Ôö£ÔöÇÔöÇ HealthController.java                # GET /api/v1/health
Ôöé
Ôö£ÔöÇÔöÇ config/
Ôöé   ÔööÔöÇÔöÇ SwaggerConfig.java              # Configuraci├│n OpenAPI con soporte Bearer JWT
Ôöé
Ôö£ÔöÇÔöÇ controllers/
Ôöé   Ôö£ÔöÇÔöÇ NegocioController.java           # POST /api/v1/negocios ┬À GET /api/v1/negocios
Ôöé   Ôö£ÔöÇÔöÇ ServicioController.java          # POST /api/v1/servicios ┬À GET /api/v1/servicios/negocio/{id}
Ôöé   ÔööÔöÇÔöÇ DTOs/
Ôöé       Ôö£ÔöÇÔöÇ NegocioCreateRequest.java    # { nombreNegocio, direccion, telefono, identificacionFiscal, monedaCodigoIso }
Ôöé       ÔööÔöÇÔöÇ ServicioCreateRequest.java   # { negocioId, modalidadId, nombre, descripcion, duracionMinutos, precio }
Ôöé
Ôö£ÔöÇÔöÇ entities/
Ôöé   Ôö£ÔöÇÔöÇ Negocio.java                     # id, propietarioId, nombreNegocio, direccionNegocio,
Ôöé   Ôöé                                    #   telefono, identificacionFiscal, monedaCodigoIso, estado
Ôöé   ÔööÔöÇÔöÇ Servicio.java                    # id, negocio(FK), modalidadId, nombre, descripcion,
Ôöé                                        #   duracionMinutos, precio, estado ("No Asignado")
Ôöé
Ôö£ÔöÇÔöÇ exceptions/
Ôöé   ÔööÔöÇÔöÇ RecursoDuplicadoException.java   # Lanzada cuando identificacionFiscal o nombre de servicio ya existen
Ôöé
Ôö£ÔöÇÔöÇ repositories/
Ôöé   Ôö£ÔöÇÔöÇ NegocioRepository.java           # findByPropietarioId, existsByIdentificacionFiscal
Ôöé   ÔööÔöÇÔöÇ ServicioRepository.java          # findByNegocio_IdNegocio, existsByNombreAndNegocio
Ôöé
Ôö£ÔöÇÔöÇ security/
Ôöé   Ôö£ÔöÇÔöÇ IJwtService.java                 # Contrato: extractUserId, isTokenValid
Ôöé   Ôö£ÔöÇÔöÇ JwtService.java                  # Implementaci├│n HS256 ┬À JJWT 0.12.5
Ôöé   Ôö£ÔöÇÔöÇ JwtAuthenticationFilter.java     # OncePerRequestFilter ÔÇö valida Bearer, inyecta propietarioId como Principal
Ôöé   ÔööÔöÇÔöÇ SecurityConfig.java             # SecurityFilterChain STATELESS
Ôöé
ÔööÔöÇÔöÇ services/
    Ôö£ÔöÇÔöÇ INegocioService.java             # Contrato: crear, listarPorPropietario
    Ôö£ÔöÇÔöÇ NegocioService.java             # Valida unicidad de identificaci├│nFiscal, pertenencia al propietario
    Ôö£ÔöÇÔöÇ IServicioService.java            # Contrato: crear, listarPorNegocio
    ÔööÔöÇÔöÇ ServicioService.java            # Valida rol PROPIETARIO, pertenencia del negocio, nombre no duplicado
```

## API Endpoints

| M├®todo | Ruta                              | Seguridad              | Descripci├│n                                         |
|--------|-----------------------------------|------------------------|-----------------------------------------------------|
| `GET`  | `/api/v1/health`                  | P├║blico                | Health check de la API                              |
| `POST` | `/api/v1/negocios`                | Bearer JWT (Propietario) | Registro de un nuevo negocio                      |
| `GET`  | `/api/v1/negocios`                | Bearer JWT (Propietario) | Listado de negocios del propietario autenticado   |
| `POST` | `/api/v1/servicios`               | Bearer JWT (Propietario) | Creaci├│n de un servicio para un negocio propio    |
| `GET`  | `/api/v1/servicios/negocio/{id}`  | Bearer JWT             | Listado de servicios asociados a un negocio         |
| `GET`  | `/swagger-ui.html`                | P├║blico                | Documentaci├│n interactiva Swagger UI                |
| `GET`  | `/v3/api-docs`                    | P├║blico                | Especificaci├│n OpenAPI v3 en JSON                   |

> **Autorizaci├│n por rol**: la verificaci├│n del rol `PROPIETARIO` se realiza en la capa de servicios extrayendo el `propietarioId` del `Principal` (claim `sub` del JWT). Spring Security solo distingue autenticado / no autenticado a nivel de filtro.

### Ejemplo ÔÇö Registrar negocio

**Request**
```http
POST /api/v1/negocios
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "nombreNegocio": "Peluquer├¡a Estilo",
  "direccionNegocio": "Cra 10 # 20-30, Medell├¡n",
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
  "nombreNegocio": "Peluquer├¡a Estilo",
  "direccionNegocio": "Cra 10 # 20-30, Medell├¡n",
  "telefono": "3001234567",
  "identificacionFiscal": "900123456-1",
  "monedaCodigoIso": "COP",
  "estado": "Activo"
}
```

### Ejemplo ÔÇö Crear servicio

**Request**
```http
POST /api/v1/servicios
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "negocioId": 5,
  "modalidadId": 1,
  "nombre": "Corte de cabello cl├ísico",
  "descripcion": "Corte tradicional con tijeras y m├íquina",
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
  "nombre": "Corte de cabello cl├ísico",
  "descripcion": "Corte tradicional con tijeras y m├íquina",
  "duracionMinutos": 30,
  "precio": 25000.00,
  "estado": "No Asignado"
}
```

## Modelo de datos

### Negocio

| Campo                | Tipo        | Restricciones           | Descripci├│n                                  |
|----------------------|-------------|-------------------------|----------------------------------------------|
| `id_negocio`         | `INTEGER`   | PK, auto-generado       | Identificador interno del negocio            |
| `propietario_id`     | `INTEGER`   | NOT NULL                | ID del usuario propietario (claim JWT `sub`) |
| `nombre_negocio`     | `VARCHAR(250)` | NOT NULL             | Nombre comercial del negocio                 |
| `direccion_negocio`  | `VARCHAR(250)` | NOT NULL             | Direcci├│n f├¡sica del negocio                 |
| `telefono`           | `VARCHAR(20)`  | opcional             | Tel├®fono de contacto                         |
| `identificacion_fiscal` | `VARCHAR(20)` | NOT NULL, UNIQUE    | NIT o identificaci├│n tributaria              |
| `moneda_codigo_iso`  | `VARCHAR(3)`  | NOT NULL             | C├│digo ISO 4217 de la moneda de operaci├│n    |
| `estado`             | `VARCHAR(20)` | NOT NULL, default `Activo` | Estado del negocio                      |

### Servicio

| Campo               | Tipo            | Restricciones                | Descripci├│n                                       |
|---------------------|-----------------|------------------------------|---------------------------------------------------|
| `id_servicio`       | `INTEGER`       | PK, auto-generado            | Identificador interno del servicio                |
| `negocio_id`        | `INTEGER`       | NOT NULL, FK ÔåÆ negocio       | Negocio al que pertenece el servicio              |
| `modalidad_id`      | `INTEGER`       | NOT NULL                     | ID de modalidad (referencia l├│gica a Masters)     |
| `nombre`            | `VARCHAR(100)`  | NOT NULL                     | Nombre del servicio (├║nico por negocio)           |
| `descripcion`       | `VARCHAR(500)`  | NOT NULL                     | Descripci├│n detallada del servicio                |
| `duracion_minutos`  | `INTEGER`       | NOT NULL                     | Duraci├│n estimada en minutos                      |
| `precio`            | `DECIMAL(12,2)` | NOT NULL                     | Precio del servicio en la moneda del negocio      |
| `estado`            | `VARCHAR(20)`   | NOT NULL, default `No Asignado` | Estado del servicio                           |

## Configuraci├│n y variables de entorno

Copia `.env.example` como `.env` en la ra├¡z del proyecto y completa los valores:

```bash
cp .env.example .env
```

| Variable            | Descripci├│n                              | Valor por defecto / Ejemplo                             |
|---------------------|------------------------------------------|---------------------------------------------------------|
| `DB_HOST`           | Host de PostgreSQL (Supabase)            | `aws-0-...pooler.supabase.com`                          |
| `DB_PORT`           | Puerto de base de datos                  | `5432`                                                  |
| `DB_NAME`           | Nombre de la base de datos               | `postgres`                                              |
| `DB_USERNAME`       | Usuario de base de datos                 | `postgres.xxx`                                          |
| `DB_PASSWORD`       | Contrase├▒a de base de datos              | `*****`                                                 |
| `DB_SSLMODE`        | Modo SSL                                 | `require`                                               |
| `JPA_DDL_AUTO`      | Estrategia DDL de Hibernate              | `update`                                                |
| `JWT_SECRET`        | Clave secreta Base64 (m├¡n. 256 bits)     | `*****`                                                 |
| `JWT_EXPIRATION_MS` | Tiempo de vida del JWT en ms             | `86400000` (24 horas)                                   |
| `PORT`              | Puerto HTTP del servidor                 | `8083`                                                  |

> **ÔÜá´©Å IMPORTANTE**: Jam├ís commitear secretos en texto plano. En producci├│n (Render, CI) las variables se inyectan en el entorno.  
> La clave `JWT_SECRET` debe ser **id├®ntica** en todos los servicios del ecosistema para que la validaci├│n local del JWT funcione.

## Comandos de desarrollo

```bash
# Compilar sin tests
./mvnw clean compile          # Linux / macOS
mvnw.cmd clean compile        # Windows

# Ejecutar la aplicaci├│n (puerto 8083 por defecto)
./mvnw spring-boot:run
mvnw.cmd spring-boot:run      # Windows

# Empaquetar JAR (sin tests)
./mvnw clean package -DskipTests

# Ejecutar todos los tests (con reporte JaCoCo)
./mvnw test
mvnw.cmd test                 # Windows

# Ejecutar un test espec├¡fico
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
- **H2 en memoria** configurado en `src/test/resources/application.properties` con modo PostgreSQL (`create-drop`), garantizando tests herm├®ticos sin dependencia de la base de datos de desarrollo.
- Clases de test ubicadas en `src/test/java/com/udea/business/` replicando la jerarqu├¡a de paquetes de producci├│n.
- Convenciones de nombres: sufijo `Test` (ej. `NegocioServiceTest`, `ServicioControllerTest`); m├®todos en camelCase descriptivo sin prefijo `test` (ej. `crearNegocioExitoso`, `debeRetornar409CuandoIdentificacionFiscalDuplicada`).
- Smoke test de contexto (`BusinessApplicationTests#contextLoads`) siempre presente.
- Tests unitarios de servicios con Mockito simulando repositorios.
- Tests de controladores con `@WebMvcTest` o pruebas de validaci├│n de DTOs.

## Reglas de negocio

| Regla | Descripci├│n |
|-------|-------------|
| Unicidad de `identificacionFiscal` | Un NIT/identificaci├│n fiscal no puede estar registrado en m├ís de un negocio. Lanza `RecursoDuplicadoException`. |
| Pertenencia del negocio | Solo el propietario autenticado (claim `sub` del JWT) puede crear servicios en sus propios negocios. |
| Unicidad del nombre de servicio | Dentro de un mismo negocio, el nombre de un servicio debe ser ├║nico. Lanza `RecursoDuplicadoException`. |
| Estado inicial del servicio | Todo servicio se crea con estado `"No Asignado"` hasta ser asignado a un horario de disponibilidad. |
| Estado inicial del negocio | Todo negocio se crea con estado `"Activo"`. |

## Convenciones de c├│digo

- **Paquetes**: camelCase (`config`, `controllers`, `entities`, `exceptions`, `repositories`, `security`, `services`).
- **Clases**: PascalCase. Interfaces de servicio con prefijo `I` (`INegocioService`, `IServicioService`, `IJwtService`).
- **M├®todos y variables**: camelCase. Constantes: `UPPER_SNAKE_CASE`.
- **Lombok**: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` en entidades y DTOs.
- **Idioma**: espa├▒ol para nombres de dominio, Javadoc y mensajes de error.
- Endpoints documentados con `@Operation`, `@ApiResponse` y `@Tag` (SpringDoc/OpenAPI).
- Validaci├│n de entrada estricta con Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@NotNull`, `@Size`, `@Pattern`) en DTOs bajo `controllers/DTOs`.
