# Hallazgos de calidad — Sprint 2 — Business

## 1. Resumen de la verificación

| Dato | Valor |
|---|---|
| Microservicio | Business |
| Sprint | 2 |
| Fecha de verificación | 2026-10-22 |
| Rama y commit verificados | main @ 3a9247a |
| Tests ejecutados | 69 |
| Fallos | 0 |
| Cobertura de líneas | 97,52 % (157/161, JaCoCo) |
| Cobertura de ramas | 96,67 % (29/30, JaCoCo) |
| Complejidad ciclomática del proyecto | 54 |
| Complejidad máxima por clase | 11 (AsignacionProveedorService) |

## 2. Resumen por severidad

| Severidad | Vigentes | Corregidos | No vigentes | Total |
|---|---|---|---|---|
| Alta | 0 | 0 | 0 | 0 |
| Media | 2 | 0 | 0 | 2 |
| Baja | 4 | 0 | 0 | 4 |
| Por confirmar | 0 | 0 | 0 | 0 |
| Total | 6 | 0 | 0 | 6 |

## 3. Hallazgos

| ID | ID PR | Clase y método | Descripción | Impacto | Severidad | HU / CP | Estado | Recomendación |
|---|---|---|---|---|---|---|---|---|
| H-01 | S1 | JwtService.isTokenValid (L49-L51) | Un token sin claim `exp` se rechaza solo porque `getExpiration()` devuelve null y la NullPointerException cae en `catch (Exception)`. | El rechazo depende de un efecto colateral; si se cambia el catch, se aceptarían tokens sin expiración. | Media | Transversal (seguridad JWT) / — | Vigente | Exigir `exp` de forma explícita (`requireExpiration` o validación de null) antes de comparar la fecha. |
| H-02 | S4 | ProveedoresExceptionHandler (L21) | No existe handler genérico; las excepciones no controladas salen sin traducir y las responde el manejo por defecto de Spring Boot con un 500. | Formato de error inconsistente con el resto de la API y posible exposición de detalles internos según la configuración. | Media | HU-0008 / CP-23 a CP-28 | Vigente | Agregar un `@ExceptionHandler(Exception.class)` que responda 500 con mensaje genérico y registre el error. |
| H-03 | S2 | UsuarioActual.getId (L20), UsuarioActual.esPropietario (L31-L32) | No se valida `isAuthenticated()`; un token no autenticado con nombre numérico devolvería ese id. | Bajo mientras solo JwtAuthenticationFilter llene el contexto con tokens autenticados. | Baja | HU-0008 / CP-28 | Vigente | Rechazar autenticaciones con `isAuthenticated() == false` o de tipo anónimo. |
| H-04 | S3 | JwtAuthenticationFilter.doFilterInternal (L71) | `catch (Exception ignored)` vacío, sin registro en logs. Sonar lo marca como code smell. | Fallos de configuración del secreto o de la librería JWT pasan desapercibidos. | Baja | Transversal (seguridad JWT) / — | Vigente | Registrar la excepción en nivel debug o warn sin exponer el token. |
| H-05 | S5 | JwtAuthenticationFilter.doFilterInternal (L57-L68), SecurityConfig.securityFilterChain (L27) | Un token con `rolActivo` ajeno a sus roles o sin roles queda autenticado sin authorities y supera `anyRequest().authenticated()`. | Sin impacto actual porque `asignar` valida el rol en el service; un endpoint futuro que solo exija autenticación lo aceptaría. | Baja | Rol activo / CP-38, CP-47 | Vigente | No autenticar cuando la lista de authorities queda vacía, o exigir roles en SecurityConfig. |
| H-06 | Nuevo | ProveedoresExceptionHandler.errorDeNegocio (L37) | `ESTADOS.get(e.getClass())` busca por clase exacta; una subclase de las excepciones de negocio obtiene un status null y el handler falla con 500. | Latente: hoy no existen subclases, pero las excepciones no son `final`. Introducido por el refactor. | Baja | HU-0008 / CP-25, CP-27, CP-28 | Vigente | Declarar las excepciones `final` o resolver el estado recorriendo la jerarquía de clases. |

## 4. Pendientes de definir con el PO

No aplica.

## 5. Complejidad ciclomática

| Métrica | Antes | Después |
|---|---|---|
| Complejidad del proyecto | 82 | 54 |
| Complejidad máxima por clase | 19 | 11 |

| Clase | Complejidad |
|---|---|
| services.AsignacionProveedorService | 11 |
| security.JwtService | 7 |
| security.JwtAuthenticationFilter | 7 |
| controllers.ProveedoresExceptionHandler | 5 |
| security.UsuarioActual | 4 |

Nota: el pipeline evalúa `complexity < 50`. Falta confirmar con el profesor si el límite aplica al proyecto o a la clase.

- Por proyecto: no se cumple (54).
- Por clase: se cumple (máximo 11).

Los valores provienen de JaCoCo. Sonar calcula la complejidad sobre el código fuente de todo `src/main/java`, sin aplicar `sonar.coverage.exclusions`, por lo que su valor puede diferir levemente. Sin config/, entities/ ni *Application la suma es 45.

## 6. Notas

- Los hallazgos no se corrigen desde pruebas unitarias porque el alcance excluye el código de producción. Los de severidad Alta y Media se registran en Mantis.
- Tests ajustados por el refactor: ninguno. Los 69 tests pasaron sin cambios.
- El refactor eliminó la rama inalcanzable de `JwtService.isTokenValid` (verificación de expiración posterior al parseo).
