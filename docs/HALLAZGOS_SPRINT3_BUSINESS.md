# Hallazgos de calidad — Sprint 3 — Business

## 1. Resumen de la verificación

| Dato | Valor |
|---|---|
| Microservicio | Business |
| Sprint | 3 |
| Tipo de verificación | Verificación final del Sprint 3 (pruebas unitarias de QA) |
| Fecha de verificación | 2026-11-20 |
| Rama y commit verificados | qa/sprint3 @ 67fdd6b |
| Línea base | main @ 4ab0009 (`src/main` idéntico al de qa/sprint3) |
| Tests ejecutados | 95 → 331 |
| Fallos | 0 |
| Cobertura de líneas (JaCoCo) | 24,5 % (173/707) → 99,9 % (706/707) |
| Cobertura de ramas (JaCoCo) | 10,8 % (34/314) → 98,1 % (308/314) |
| Cobertura estilo Sonar (líneas + ramas, exclusiones del workflow) | ≈ 20,3 % → ≈ 99,3 % ((706 + 308) / (707 + 314)) |
| Cobertura en SonarCloud | 18,8 % en main @ 4ab0009; qa/sprint3 no disponible (sin análisis) |
| Complejidad ciclomática del proyecto (SonarCloud) | 247 en main @ 4ab0009 |
| Complejidad ciclomática del proyecto (JaCoCo) | 372 sin exclusiones, 288 con las exclusiones de Sonar |
| Complejidad máxima por clase | 54 en SonarCloud, 56 en JaCoCo (ReservaValidator) |

Las cifras de JaCoCo salen de `mvnw.cmd -B clean verify` sobre 67fdd6b. Exclusiones del workflow para la cobertura: `**/config/**`, `**/entities/**` y `**/*Application.java`.

## 2. Resumen por severidad

| Severidad | Vigentes | Corregidos | No vigentes | Cerrados por decisión | Total |
|---|---|---|---|---|---|
| Alta | 0 | 0 | 0 | 0 | 0 |
| Media | 1 | 0 | 0 | 0 | 1 |
| Baja | 2 | 0 | 0 | 0 | 2 |
| Por confirmar | 0 | 0 | 0 | 0 | 0 |
| Total | 3 | 0 | 0 | 0 | 3 |

Incluye el hallazgo nuevo H-01 y los heredados del Sprint 2 que siguen vigentes: H-07 y H-08.

## 3. Hallazgos

| ID | ID PR | Clase y método | Descripción | Impacto | Severidad | HU / CA | Estado | Recomendación |
|---|---|---|---|---|---|---|---|---|
| H-01 | Nuevo | ReservaService.crearReserva (L54-L126; el inicio se arma en L77), ReservaValidator.validarParametrosBasicos (L63-L81), ReservaValidator.obtenerYValidarFranja (L84-L116) | Al crear una reserva no se compara la fecha y hora de la franja con el momento actual, y tampoco se aplica la ventana de 30 días. Ningún método del flujo recibe `Clock` para validar eso: ReservaValidator no tiene `Clock` y ReservaService solo lo usa para `fechaCreacion` (L99). Reproducción con ReservaValidator real y `Clock.fixed` en el jueves 05/11/2026 10:00 (America/Bogota): una solicitud equivalente a `POST /api/v1/reservas` con `{"servicioId":1,"proveedorId":12,"fecha":"2026-11-02","horaInicio":"09:00"}`, sobre una franja definida de los lunes a las 09:00, devuelve estado "Confirmada". Con `"fecha":"2026-12-07"` (fuera de la ventana, cuyo límite es el 05/12/2026) también se confirma. | La reserva pasada aparece de inmediato como "Realizada" en el historial (HU-0019/CA-02, ClasificadorReservas L25) y no se puede cancelar porque su inicio ya pasó (HU-0021, CancelacionReservaValidator L65). Además permite ocupar la agenda del proveedor fuera de la ventana que la consulta de disponibilidad sí aplica (DisponibilidadService L66-L68). | Media | HU-0018 / funcionalidad 2 (la franja debe seguir disponible al confirmar); relacionado con HU-0016/CA-07 y CA-09 | Vigente | Validar en el flujo de creación, con el `Clock` inyectado, que el inicio sea posterior al momento actual y que la fecha esté dentro de la ventana de 30 días, con el mismo criterio de DisponibilidadService (L66-L68 y L104). |
| H-07 | Sprint 2 | ProveedoresExceptionHandler.errorInesperado (L58-L63; en el informe del Sprint 2, L55-L60) | `@ExceptionHandler(Exception.class)` sigue capturando las excepciones estándar de Spring MVC. El advice ahora también aplica a ReservaController, MisReservasController, AgendaProveedorController y ServicioRecursoController. Nuevas manifestaciones en el Sprint 3, reproducidas con MockMvc standalone y el handler registrado: (1) `GET /api/v1/servicios/{id}/franjas-disponibles` sin el parámetro `fecha` responde 500 "Ha ocurrido un error inesperado en el servidor." (MissingServletRequestParameterException), aunque ReservaController documenta 400 "Parámetros inválidos o fecha faltante." (L80); (2) `POST /api/v1/reservas` con `Content-Type: text/plain` responde 500 en lugar de 415. | Los errores del cliente se informan como 500 y se registran en nivel error; el frontend no puede distinguir una solicitud incompleta de una falla del servidor. | Baja | HU-0016 / — (fecha obligatoria); HU-0018 / —; en el Sprint 2, HU-0008 / CP-23 a CP-28 | Vigente | Conservar el status de las excepciones estándar de Spring MVC (por ejemplo, extendiendo `ResponseEntityExceptionHandler`) y reservar el 500 para el resto. |

H-08 (Sprint 2, CORS): vigente, fuera del alcance de las HU del Sprint 3, no reverificado.

## 4. Pendientes de definir con el PO

- **HU-0016 / CA-01 y CA-02**, y los CA cuya redacción parte del horario del proveedor (CA-03, CA-06, CA-07 y CA-08). El Gherkin de HU-0016 describe franjas calculadas dividiendo los bloques del horario semanal en tramos de la duración del servicio. DisponibilidadService no lee el horario: lista las franjas definidas (`FranjaServicio`, HU-0046) del servicio para ese día de la semana. HU-0018 y HU-0046 establecen que las franjas definidas son la única oferta que se puede reservar, así que el código sigue esa regla.
  - Consecuencia: la regla de CA-02 ("no se ofrece la franja de las 11:45 que no cabe") depende de que Masters no permita definir esa franja (HU-0046/CA-03). DisponibilidadService no la filtra.
  - En las pruebas, estos CA se verificaron con franjas definidas equivalentes a los bloques del escenario.
  - Propuesta: actualizar el Gherkin de HU-0016 para que hable de franjas definidas.

## 5. Escenarios no verificables con pruebas unitarias

| HU / CA | Motivo | Qué sí se cubrió |
|---|---|---|
| Todos los 401 sin JWT, incluidos HU-0042/CA-05 y HU-0043/CA-05 | Los resuelve la cadena de seguridad (SecurityConfig y JwtAuthenticationFilter), que no participa en MockMvc standalone. Se necesita una prueba de integración con el contexto de seguridad. | JwtAuthenticationFilterTest y JwtServiceTest del Sprint 2; los 403 por rol se verifican en los servicios y controladores. |
| HU-0018 / CA-03 | La carrera real entre dos transacciones no se puede reproducir en una prueba unitaria. | El bloqueo pesimista del proveedor (`findByIdConBloqueo`) se verifica antes de validar cruces y guardar (InOrder), y una `DataIntegrityViolationException` simulada en `saveAndFlush` responde 409 con "Esta franja acaba de ser reservada. Elige otra.". |
| HU-0022 / CA-01 | El orden por hora de inicio lo hace la consulta JPQL (`ORDER BY r.inicio ASC` en `findAgendaProveedor`). | Que AgendaProveedorService conserva el orden del repositorio y reporta el total de reservas. |

## 6. Complejidad ciclomática

| Métrica | Antes (main @ 4ab0009) | Después (qa/sprint3 @ 67fdd6b) |
|---|---|---|
| Complejidad del proyecto, SonarCloud | 247 | No disponible |
| Complejidad del proyecto, JaCoCo sin exclusiones | 372 | 372 |
| Complejidad del proyecto, JaCoCo con exclusiones de Sonar | 288 | 288 |
| Complejidad máxima por clase, SonarCloud | 54 | No disponible |
| Complejidad máxima por clase, JaCoCo | 56 | 56 |

Antes y Después coinciden en JaCoCo porque el Sprint 3 de QA no modifica `src/main`.

| Clase | JaCoCo | SonarCloud (main @ 4ab0009) |
|---|---|---|
| services.ReservaValidator | 56 | 54 |
| services.ReservaService | 40 | 39 |
| services.CancelacionReservaValidator | 26 | 26 |
| services.DisponibilidadService | 24 | 21 |
| services.ServicioRecursoService | 21 | 12 |

Nota: el pipeline evalúa `complexity < 50` con el valor de SonarCloud y no se cumple, ni por proyecto (247) ni por clase (ReservaValidator, 54). La complejidad es responsabilidad de Arquitectura, por eso se registra solo como métrica y no como hallazgo.

Fuentes y alcance:

- SonarCloud (`api/measures/component` y `api/measures/component_tree`, sin autenticación) tiene un análisis de main del 2026-11-12 sobre 4ab0009. No hay análisis de qa/sprint3 ni de un PR desde esa rama: el último PR analizado es el #15 (8967d8d). Por eso Después figura como "No disponible".
- El Quality Gate de SonarCloud para main está en ERROR por `new_coverage` 13,1 % < 80 %. Las demás condiciones (fiabilidad, seguridad, mantenibilidad, duplicación y hotspots) están en OK, con 0 bugs y 0 vulnerabilidades.
- Las exclusiones de Sonar para la complejidad son las de `sonar.exclusions` en el pom.xml: `**/config/**`, `**/entities/**`, `**/DTOs/**` y `**/*Application.java`. La cobertura del workflow no excluye `**/DTOs/**`, por eso la cobertura en SonarCloud (18,8 %) no coincide con el cálculo estilo Sonar de la línea base (≈ 20,3 %).
- JaCoCo suma COMPLEXITY_MISSED + COMPLEXITY_COVERED sobre el bytecode, por lo que cuenta lambdas y métodos sintéticos. Por eso su valor es mayor que el de SonarCloud.

## 7. Cobertura por clase del alcance

| Clase | HU | Líneas antes | Ramas antes | Líneas después | Ramas después |
|---|---|---|---|---|---|
| services.ReservaValidator | HU-0018 | 0/92 | 0/92 | 92/92 | 92/92 |
| services.ReservaService | HU-0018, HU-0021 | 3/159 | 0/54 | 159/159 | 51/54 |
| services.DisponibilidadService | HU-0016 | 2/65 | 0/42 | 65/65 | 42/42 |
| services.CancelacionReservaValidator | HU-0021 | 0/33 | 0/36 | 33/33 | 35/36 |
| services.ServicioRecursoService | HU-0045 | 0/52 | 0/24 | 52/52 | 24/24 |
| services.ReservaConsultaMapper | HU-0019, HU-0022 | 3/54 | — | 54/54 | — |
| services.MisReservasService | HU-0019 | 0/21 | 0/6 | 21/21 | 6/6 |
| services.AgendaProveedorService | HU-0022 | 0/15 | 0/6 | 15/15 | 6/6 |
| services.ClasificadorReservas | HU-0019 | 2/12 | 0/4 | 12/12 | 4/4 |
| services.CodigoReservaGenerator | HU-0018 | 1/10 | 0/4 | 10/10 | 4/4 |
| services.acceso.* (AccesoReservaPolicy y tres reglas) | HU-0019 | 3/13 | 0/10 | 13/13 | 10/10 |
| controllers.ReservaController | HU-0016, HU-0018, HU-0021 | 0/10 | — | 10/10 | — |
| controllers.ServicioRecursoController | HU-0045 | 0/6 | — | 6/6 | — |
| controllers.MisReservasController | HU-0019 | 0/2 | — | 2/2 | — |
| controllers.AgendaProveedorController | HU-0022 | 0/1 | — | 1/1 | — |
| controllers.ProveedoresExceptionHandler | Transversal | 12/12 | — | 12/12 | — |
| services.AsignacionProveedorService | HU-0042, HU-0043 | 53/53 | 14/14 | 53/53 | 14/14 |
| controllers.AsignacionProveedorController | HU-0042 | 4/4 | — | 4/4 | — |
| controllers.MisServiciosController | HU-0043 | 1/1 | — | 1/1 | — |
| security.UsuarioActual | Transversal | 15/16 | 5/6 | 15/16 | 5/6 |

Notas:

- Se agregaron 236 tests: 233 en 15 clases de prueba nuevas y 3 métodos nuevos en clases del Sprint 2, AsignacionProveedorControllerTest (HU-0042/CA-02 y CA-06) y MisServiciosControllerTest (HU-0043/CA-04). No se modificó ni eliminó ningún test existente y no hay `@Disabled`.
- Las 6 ramas sin cubrir no corresponden a escenarios de las HU y no se forzaron: 3 en ReservaService, 1 en CancelacionReservaValidator, 1 en UsuarioActual y 1 en JwtService. La de JwtService es la rama inalcanzable ya descrita en el Sprint 2. UsuarioActual no se modificó en el Sprint 3.
- Los escenarios de cada HU están referenciados en los tests con comentarios `// HU-00XX/CA-0Y`.
