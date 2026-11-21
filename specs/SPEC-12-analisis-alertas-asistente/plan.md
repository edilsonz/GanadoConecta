# Plan · SPEC-12

La sección privada «Análisis y alertas» conserva el selector de ganadería,
la sesión actual y la navegación adaptable. Consume dos proyecciones REST
independientes de solo lectura con `Cache-Control: no-store`.

| Ruta bajo `/api/v1/ganaderias/{ganaderiaId}` | Entrada y respuesta |
|---|---|
| `GET /analisis` | `desde`, `hasta` en ISO y `animalId` opcional. Período, conteos confirmados de sanidad y reproducción y dos pesajes recientes del animal con sus UUID, fecha, kilos y variación. Se rechazan parámetros extra y repetidos. |
| `GET /alertas` | Sin parámetros. Día de corte en Lima y lista de avisos de agenda con UUID, tipo, descripción, estado y fecha prevista. |

Spring Boot reutiliza los casos de uso autorizados de reportes y agenda.
PostgreSQL es la fuente de verdad. Los conteos reproductivos informan de
eventos, por lo que una evaluación registrada no prueba el estado actual de
gestación. El peso se compara entre dos registros, sin extrapolación. Las
alertas derivan del UUID de la actividad y no necesitan tablas ni migraciones.

## Frontera de inferencia prevista

El contrato conversacional queda como diseño pendiente de revisión al elegir
modelo y fuentes. Spring conservará la sesión, resolverá por pertenencia los
filtros y construirá una proyección acotada de hechos y referencias. FastAPI
solo recibirá la pregunta y esa proyección o los extractos del corpus necesarios.
No recibirá JWT de usuario, credenciales de PostgreSQL, historiales completos,
correos, observaciones libres ni herramientas para ejecutar SQL o escrituras.
No decidirá la ganadería ni podrá pedir una ampliación de permisos. El backend
validará que las referencias devueltas estén dentro del conjunto enviado y
revalidará el acceso antes de entregar la respuesta. La inferencia deberá
abstenerse ante insuficiencia o petición clínica. Se definirán autenticación
interna, tiempos límite, validación de afirmaciones, retención y evaluación
del corpus antes de habilitar el transporte. Caídas del modelo se informarán
en el asistente sin impedir consultar estadísticas, alertas o registrar ganado.
En este incremento no hay modelo aprobado ni ruta pública conversacional.

Verificación con JUnit, Spring MockMvc y PostgreSQL aislado para filtros,
reglas de fecha, aislamiento y revocación. Vitest y Testing Library para
estados y permisos de la UI. TypeScript, build PWA y Chrome para integrar y
verificar los anchos de 360, 768 y 1440 px cuando el entorno lo permita.
