# Hallazgos de calidad — Sprint 2 — Business

## 1. Resumen de la verificación

| Dato | Valor |
|---|---|
| Microservicio | Business |
| Sprint | 2 |
| Tipo de verificación | Verificación final del Sprint 2 |
| Fecha de verificación | 2026-10-27 |
| Rama y commit verificados | main @ c9e1de5 |
| Línea base | e15fda6 (merge del PR #6) |
| Tests ejecutados | 95 |
| Fallos | 0 |
| Cobertura de líneas | 98,05 % (201/205, JaCoCo) |
| Cobertura de ramas | 93,18 % (41/44, JaCoCo) |
| Cobertura en SonarCloud | 98,9 % |
| Complejidad ciclomática del proyecto (SonarCloud) | 48 |
| Complejidad ciclomática del proyecto (JaCoCo) | 83 sin exclusiones, 63 con las exclusiones de Sonar |
| Complejidad máxima por clase | 11 en SonarCloud, 17 en JaCoCo (AsignacionProveedorService) |

## 2. Resumen por severidad

| Severidad | Vigentes | Corregidos | No vigentes | Cerrados por decisión | Total |
|---|---|---|---|---|---|
| Alta | 0 | 0 | 0 | 0 | 0 |
| Media | 0 | 2 | 0 | 0 | 2 |
| Baja | 2 | 4 | 0 | 0 | 6 |
| Por confirmar | 0 | 0 | 0 | 0 | 0 |
| Total | 2 | 6 | 0 | 0 | 8 |

## 3. Hallazgos

| ID | ID PR | Clase y método | Descripción | Impacto | Severidad | HU / CP | Estado | Recomendación |
|---|---|---|---|---|---|---|---|---|
| H-01 | S1 | JwtService.isTokenValid (L49-L51) | Un token sin claim `exp` se rechaza solo porque `getExpiration()` devuelve null y la NullPointerException cae en `catch (Exception)`. | El rechazo depende de un efecto colateral; si se cambia el catch, se aceptarían tokens sin expiración. | Media | Transversal (seguridad JWT) / — | Corregido en 41d7c8e: valida `expiration != null` antes de comparar (L49-L50). | Exigir `exp` de forma explícita (`requireExpiration` o validación de null) antes de comparar la fecha. |
| H-02 | S4 | ProveedoresExceptionHandler (L21) | No existe handler genérico; las excepciones no controladas salen sin traducir y las responde el manejo por defecto de Spring Boot con un 500. | Formato de error inconsistente con el resto de la API y posible exposición de detalles internos según la configuración. | Media | HU-0008 / CP-23 a CP-28 | Corregido en 41d7c8e: `errorInesperado` responde 500 con mensaje genérico y registra el error (L55-L60). Ver H-07. | Agregar un `@ExceptionHandler(Exception.class)` que responda 500 con mensaje genérico y registre el error. |
| H-03 | S2 | UsuarioActual.getId (L20), UsuarioActual.esPropietario (L31-L32) | No se valida `isAuthenticated()`; un token no autenticado con nombre numérico devolvería ese id. | Bajo mientras solo JwtAuthenticationFilter llene el contexto con tokens autenticados. | Baja | HU-0008 / CP-28 | Corregido en 41d7c8e: `autenticacionValida` rechaza autenticaciones no autenticadas o anónimas; la usan `getId`, `esPropietario` y `esProveedor` (L44-L51). | Rechazar autenticaciones con `isAuthenticated() == false` o de tipo anónimo. |
| H-04 | S3 | JwtAuthenticationFilter.doFilterInternal (L71) | `catch (Exception ignored)` vacío, sin registro en logs. Sonar lo marca como code smell. | Fallos de configuración del secreto o de la librería JWT pasan desapercibidos. | Baja | Transversal (seguridad JWT) / — | Corregido en 41d7c8e: registra el mensaje de la excepción en nivel warn sin el token (L77-L78). | Registrar la excepción en nivel debug o warn sin exponer el token. |
| H-05 | S5 | JwtAuthenticationFilter.doFilterInternal (L57-L68), SecurityConfig.securityFilterChain (L27) | Un token con `rolActivo` ajeno a sus roles o sin roles queda autenticado sin authorities y supera `anyRequest().authenticated()`. | Sin impacto actual porque `asignar` valida el rol en el service; un endpoint futuro que solo exija autenticación lo aceptaría. | Baja | Rol activo / CP-38, CP-47 | Corregido en 41d7c8e: solo autentica cuando la lista de authorities no queda vacía (L63-L74). | No autenticar cuando la lista de authorities queda vacía, o exigir roles en SecurityConfig. |
| H-06 | Nuevo | ProveedoresExceptionHandler.errorDeNegocio (L37) | `ESTADOS.get(e.getClass())` busca por clase exacta; una subclase de las excepciones de negocio obtiene un status null y el handler falla con 500. | Latente: hoy no existen subclases, pero las excepciones no son `final`. Introducido por el refactor. | Baja | HU-0008 / CP-25, CP-27, CP-28 | Corregido en 41d7c8e: las cuatro excepciones de negocio son `final` y el handler usa `getOrDefault` (L39). | Declarar las excepciones `final` o resolver el estado recorriendo la jerarquía de clases. |
| H-07 | Nuevo | ProveedoresExceptionHandler.errorInesperado (L55-L60) | `@ExceptionHandler(Exception.class)` también captura las excepciones estándar de Spring MVC que ocurren con el controlador ya resuelto. Un POST a `/api/v1/servicios/{id}/proveedores` con `Content-Type: text/plain` responde 500; en e15fda6 respondía 415. | Errores del cliente se informan como 500 y se registran en nivel error en AsignacionProveedorController y MisServiciosController. Introducido por 41d7c8e. | Baja | HU-0008 / CP-23 a CP-28 | Vigente | Conservar el status de las excepciones estándar de Spring MVC (por ejemplo, extendiendo `ResponseEntityExceptionHandler`) y reservar el 500 para el resto. |
| H-08 | Nuevo | SecurityConfig.allowedOrigins (L30), SecurityConfig.corsConfigurationSource (L60-L64) | El valor por defecto de los orígenes incluye los patrones `https://*.vercel.app` y `http://localhost:*` junto con `allowCredentials(true)`. El mismo valor está en application.properties y .env.example. | Cualquier despliegue de terceros en vercel.app o cualquier puerto local queda como origen confiable con credenciales si no se define `CORS_ALLOWED_ORIGINS`. Latente mientras la autenticación use el header Authorization y no cookies. `config/` está fuera del análisis de Sonar, así que no se revisa como hotspot. | Baja | Transversal (CORS) / — | Vigente | Dejar por defecto solo el dominio del frontend, definir los demás orígenes por entorno y desactivar `allowCredentials` si no se usan cookies. |

## 4. Pendientes de definir con el PO

No aplica.

## 5. Complejidad ciclomática

| Métrica | Antes (e15fda6) | Después (HEAD, c9e1de5) |
|---|---|---|
| Complejidad del proyecto, SonarCloud | 48 | 48 |
| Complejidad del proyecto, JaCoCo sin exclusiones | 54 | 83 |
| Complejidad del proyecto, JaCoCo con exclusiones de Sonar | 44 | 63 |
| Complejidad máxima por clase, SonarCloud | No disponible | 11 |
| Complejidad máxima por clase, JaCoCo | 11 | 17 |

| Clase | JaCoCo | SonarCloud |
|---|---|---|
| services.AsignacionProveedorService | 17 | 11 |
| security.UsuarioActual | 13 | 9 |
| security.JwtService | 9 | 8 |
| security.JwtAuthenticationFilter | 9 | 8 |
| config.SecurityConfig | 7 | Excluida |

En SonarCloud la quinta clase es controllers.ProveedoresExceptionHandler (4; 6 en JaCoCo).

Nota: el pipeline evalúa `complexity < 50` con el valor de SonarCloud.

- Por proyecto: se cumple (48).
- Por clase: se cumple (máximo 11).

Fuentes y alcance:

- SonarCloud (`api/measures/component`, rama main) responde sin autenticación: complexity 48 y coverage 98,9 para el análisis de c9e1de5. El valor de Antes corresponde al análisis de 3a9247a, que tiene el mismo `src/main` que e15fda6 y no tenía exclusiones; e15fda6 no tiene análisis propio porque el historial se reescribió.
- `sonar.exclusions` se agregó en el pom.xml en 3755641 con `**/config/**,**/entities/**,**/*Application.java`; c9e1de5 le añadió `**/DTOs/**`. Esta propiedad retira esos archivos de todo el análisis de Sonar (complejidad, issues, hotspots y duplicación), no solo de la cobertura. El workflow y sonar-project.properties solo definen `sonar.coverage.exclusions`, sin `**/DTOs/**`.
- Evolución en SonarCloud: 76 (a5ef4d3), 74 (2639e1c), 60 (3755641, SecurityConfig movido a `config/` y exclusiones), 52 (0ba3e23, refactor), 48 (c9e1de5, exclusión de DTOs). De los 28 puntos de reducción, 18 provienen de exclusiones y 10 de refactor.
- JaCoCo suma COMPLEXITY_MISSED + COMPLEXITY_COVERED sobre el bytecode, por lo que cuenta lambdas y métodos sintéticos; por eso su valor es mayor que el de SonarCloud.

## 6. Notas

- Los hallazgos no se corrigen desde pruebas unitarias porque el alcance excluye el código de producción. Los de severidad Alta y Media se registran en Mantis.
- Tests agregados o ajustados en esta verificación: ninguno. La cobertura de líneas (98,05 %) supera el 85 %.
- Tests del equipo desde e15fda6: pasan de 69 a 95, sin tests eliminados y sin `@Disabled`. Cambiaron aserciones por los cambios de comportamiento de las correcciones, sin debilitarlas:
  - JwtAuthenticationFilterTest: dos tests ahora verifican que no se autentica (H-05).
  - UsuarioActualTest: el usuario anónimo ahora lanza AccesoDenegadoException en lugar de devolver false (H-03).
  - AsignacionProveedorControllerTest: la excepción no controlada ahora responde 500 en lugar de propagarse (H-02).
- Código nuevo sin pruebas: todas sus líneas están cubiertas. Quedan sin cubrir la rama de UsuarioActual L48 (principal `anonymousUser` en una autenticación no anónima) y la rama falsa de `after` en JwtService L50, inalcanzable porque jjwt rechaza el token expirado al parsearlo. Las consultas JPQL nuevas de ServicioProveedorRepository solo se validan al cargar el contexto en BusinessApplicationTests; no hay pruebas de sus resultados.
- Observaciones menores:
  - Import `java.util.Arrays` sin usar en SecurityConfig (L20). Sonar no lo reporta porque `config/` está excluido.
  - Nombres totalmente calificados en código nuevo: SecurityConfig L53 (`java.util.ArrayList`), AsignacionProveedorController L56 (`java.util.List` y `com.udea.business.controllers.DTOs.ProveedorAsignadoResponse`) y AsignacionProveedorServiceTest L412 y L429.
  - `consultarProveedoresAsignados` reutiliza MSG_SOLO_PROPIETARIO y MSG_SIN_PERMISO, cuyos textos hablan de asignar proveedores; el 403 de la consulta devuelve un mensaje de asignación.
  - `AsignacionProveedorService.validar` recibe la excepción ya construida, así que crea la excepción y su mensaje aunque la condición sea falsa.
  - El nombre de JwtAuthenticationFilterTest.doFilterAutenticaSinAuthoritiesCuandoElTokenNoTraeRoles ya no corresponde a lo que verifica.
  - ProveedoresExceptionHandlerTest.errorDeNegocioManejaExcepcionDesconocidaConFallback400 cubre una rama a la que no se llega por HTTP.
  - .gitignore: tras la reescritura de main quedó con 13 líneas (40 en 3a9247a). Sigue ignorando `target/`, `.env*` (excepto `.env.example`), `.idea`, `*.iml` y `.vscode/`. Se perdieron `*.class`, `*.log`, `*.jar`, `*.war`, `*.zip` y demás empaquetados, `hs_err_pid*` y `replay_pid*`.
  - README: menciona Supabase (L29 y L249, con el ejemplo `aws-0-...pooler.supabase.com`), ubica SecurityConfig en `security/` y no documenta los endpoints de HU-01 y HU-02 ni `CORS_ALLOWED_ORIGINS`. .env.example no menciona Supabase.
- Migración de la base de datos a PostgreSQL en Google Cloud Compute Engine: los tests usan H2 en memoria (`src/test/resources/application.properties`) y no dependen de la base real. application.properties exige `sslmode=require` por defecto y .env.example usa `DB_SSLMODE=disable` para desarrollo local. El uso de SSL en el PostgreSQL de GCE queda como verificación del equipo.
