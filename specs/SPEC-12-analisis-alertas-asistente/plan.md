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

## Contrato RF19 para la prueba local

Ollama local y `qwen3:4b-instruct-2507-q4_K_M` están autorizados para esta fase,
sujetos a una prueba de viabilidad. Alcance: datos propios confirmados, sin
corpus de orientación general y sin historial persistente. El módulo tendrá
activación explícita y quedará independiente del análisis y de las alertas.

React envía `POST /api/v1/ganaderias/{ganaderiaId}/asistente` con bearer y CSRF.
El cuerpo contiene `pregunta`, `tema`, `desde`, `hasta` y `animalId` opcional.
Los temas son peso, sanidad, reproducción y actividades pendientes. En peso,
sanidad y reproducción se requiere seleccionar un animal. El intervalo es
inclusivo y el selector de tema acota la información que se entrega al modelo.
Cada pregunta tiene un máximo de 400 caracteres y hasta ocho hechos de contexto.
Peso compara los dos pesajes más recientes dentro del intervalo seleccionado.
La API devuelve estado `RESPUESTA` o `INSUFICIENTE`, texto en español y
referencias de origen. 400 indica entrada inválida, 401 sesión inválida,
403 acceso denegado, 429 límite de concurrencia y 503 servicio indisponible.

Spring valida sesión, pertenencia y permisos antes de consultar. Usa servicios
de lectura existentes, prepara hechos tipados y cortos y asigna referencias
temporales. Antes de responder vuelve a validar sesión y membresía, también si
la inferencia falló. FastAPI recibe la pregunta y solamente esa proyección en
`POST /interno/v1/inferir` por loopback, autenticado con un secreto de servicio
externo a Git. Quedan fuera del transporte JWT, IDs persistentes, notas y
contenidos clínicos libres, credenciales de PostgreSQL y herramientas de SQL o
escritura. Ambos adaptadores acotan tamaño, concurrencia y tiempo de espera.

Ollama elige mediante JSON un hecho por su referencia temporal, un listado
acotado del tema o la abstención. FastAPI convierte esa elección en una
selección validada de referencias. Spring la comprueba contra la evidencia
enviada y autorizada. El último pesaje, la pareja de pesajes y la variación
son hechos distintos. La pareja y la variación citan ambos registros. Las
frases de salida conservan las cifras, las fechas y las unidades confirmadas.
Para otros temas se entregan hasta ocho registros del período.
Las referencias se presentan con el tipo y fecha del registro. Una referencia
desconocida o una salida incompatible descarta la respuesta. El modelo no
define el ámbito ni ejecuta acciones, incluso si la pregunta se lo solicita.

Verificación con JUnit, Spring MockMvc y PostgreSQL aislado para filtros,
reglas de fecha, aislamiento y revocación. Vitest y Testing Library para
estados y permisos de la UI. TypeScript, build PWA y Chrome para integrar y
verificar los anchos de 360, 768 y 1440 px cuando el entorno lo permita.
