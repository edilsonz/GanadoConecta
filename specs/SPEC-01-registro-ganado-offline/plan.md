# SPEC-01 — Plan técnico propuesto

**Estado:** borrador para aprobación; no implementado. Depende de aprobar
[spec.md](spec.md), DP01–DP08 y el contrato de acceso. Este plan no modifica
los ADR aprobados ni autoriza empezar a programar. [Tareas](tasks.md).

## 1. Arquitectura y límites

React PWA usa un adaptador IndexedDB y un cliente REST. Spring Boot permanece
como monolito modular; PostgreSQL es la única fuente remota principal.
La sincronización entra por los casos de uso normales de alta. No hay servicio
nuevo, escritura directa desde el navegador a PostgreSQL ni dependencia del
núcleo Java respecto a Spring, JPA, Redis o APIs del navegador.

| Área prevista | Responsabilidad / contratos orientativos |
|---|---|
| backend/modulos/identidad | Consultar identidad autenticada y contexto autorizado de ganadería; contrato AutorizarAccesoGanaderia. No implementa invitaciones en esta SPEC. |
| backend/modulos/ganado/dominio | Animal, AnimalId, GanaderiaId, Sexo, Categoria; invariantes y validaciones puras. |
| backend/modulos/ganado/aplicacion | RegistrarAnimal, ConsultarAnimales, ConsultarFichaAnimal; contratos RepositorioAnimales, RegistroOperaciones y UnidadDeTrabajo. |
| backend/modulos/ganado/adaptadores/entrada | Controladores REST, DTO y validación de contrato/errores. |
| backend/modulos/ganado/adaptadores/salida | Persistencia PostgreSQL/JPA, restricciones, orden transaccional y consultas autorizadas. |
| backend/modulos/ganado/configuracion | Cableado Spring y límites de transacción; sin anotaciones de infraestructura en dominio. |
| frontend/funcionalidades/ganado | Formulario, «Mi ganado», ficha, permisos de UI y avisos de conexión/sincronización. |
| frontend/aplicacion/sincronizacion | Construcción de sobres, estados/reintentos y reconciliación por animalId. |
| frontend/adaptadores | IndexedDB, Web Crypto y HTTP inyectables; Service Worker solo para recursos estáticos. |

Rutas orientativas para diseñar después de aprobar, no carpetas creadas.
Solo se introducen contratos donde permiten aislar persistencia, seguridad,
reloj, generador de IDs y red. No se exige una interfaz por clase.


### Dependencia previa para registro real

Antes de habilitar POST, listado, ficha o preparación offline con datos reales,
deben existir autenticación funcional, ganadería activa, membresía vigente y
permisos de PROPIETARIO/EDITOR/LECTOR, comprobados en backend y con pruebas de
integración. Contrato redactado o mock no bastan. Solo propietario/editor crean;
lector consulta. La preparación inicial y renovación offline requieren esa
comprobación online. La expiración no concede nuevos permisos.
Se podrá diseñar y probar dominio con dobles antes, pero no habilitar registro
real. La gestión de invitaciones y el registro masivo necesitan otras SPEC;
esta sigue limitada al alta individual, ficha, listado y funcionamiento offline.
## 2. Entidades y representación propuesta

| Concepto | Datos / invariantes |
|---|---|
| Ganaderia (existente) | id, estado y propietario; la asociación del animal no cambia en este flujo. |
| Membresia (dependencia) | usuarioId + ganaderiaId, rol PROPIETARIO/EDITOR/LECTOR, estado; autorización vigente. |
| Animal | id UUIDv4, ganaderiaId, sexo obligatorio MACHO/HEMBRA (NOT NULL), nombre, areteFrente, areteReverso, raza, fechaNacimiento, nacimientoEstimado, categoria, observaciones; ACTIVO, version=1, creadoPor, creadoEn. |
| OperacionConfirmada | operationId UUIDv4, ganaderiaId, actorId autenticado, animalId, tipo, schemaVersion, baseVersion, huella del cuerpo canónico, versionCreada, confirmadoEn. Solo éxitos confirmados. |
| AnimalLocal | cuentaId, ganaderiaId, animalId, datos, versionRemota nullable, fechaCopia y estado de sincronización; separado de la copia remota. |
| OperacionLocal | cuentaId, ganaderiaId, operationId, sobre inmutable, estado, intentos, proximoIntento, errorSeguro, lease local de envío. |
| ContextoOffline | cuentaId y ganaderiaId, permisos vistos, preparadoEn y ultimaValidacionOnline; metadatos, nunca credenciales. |

No se crean tablas genealógicas, sanitarias, de venta o fotografías. Este es
un refinamiento propuesto del modelo conceptual, no un esquema ya desplegado.

## 3. Identidad y huella de operación

Se propone UUIDv4 tanto para animalId como para operationId, generados por
`crypto.randomUUID()` en contexto seguro cuando se confirma el formulario.
Son dos valores diferentes y se verifican contra la copia local antes de guardar.
Si falta ese API no se usa Math.random, timestamp, contador ni nombre/arete:
se informa incompatibilidad y no se guarda. Una excepción deja el formulario
sin confirmar. Los adaptadores de pruebas permiten forzar colisiones.

UUIDv4 reduce mucho la probabilidad de colisión, no la elimina por demostración
matemática. Las restricciones de PostgreSQL detectan las colisiones remotas.
El ID no se cambia en cada intento. Colisión -> conflicto seguro; no se genera
otro animal automáticamente después de una petición enviada.

El servidor construye una representación canónica de todos los campos admitidos:
tipo, schemaVersion, baseVersion, animalId y datos; añade ganaderiaId de ruta
y actorId autenticado como ámbito. NFC/trim, vacíos/ausencias a null para datos
opcionales; enum/UUID normalizados; orden de campos fijo y UTF-8. Los campos
técnicos obligatorios no se rellenan por defecto. No se incluye orden de JSON,
headers, número de intento ni reloj local. Calcula SHA-256; no acepta la huella
del cliente. La misma versión de canonicalización se conserva para recibos v1.
El éxito previo devuelve recibo solo si coinciden ámbito y huella; en otro caso
se devuelve conflicto genérico, sin exponer datos del recibo ajeno.

## 4. API REST propuesta

Prefijo `/api/v1`. JSON sobre HTTPS; autenticación conforme al contrato de acceso.
No se introduce una segunda ruta de alta «offline»: el mismo POST procesa el
sobre guardado en la cola, conectado o al sincronizar posteriormente.

| Método y ruta | Entrada | Éxito |
|---|---|---|
| POST /ganaderias/{ganaderiaId}/animales | Sobre de sección 9 de SPEC con operationId, animalId, tipo, schemaVersion, baseVersion y datos descriptivos. | 201 APLICADO en primera confirmación; 200 REPROCESADO al repetir la misma operación, con recibo original. |
| GET /ganaderias/{ganaderiaId}/animales | limit=20 por defecto, 1–100; cursor opcional. Sin filtros nuevos en este incremento. | 200 items básicos, siguienteCursor nullable y consultadoEn. |
| GET /ganaderias/{ganaderiaId}/animales/{animalId} | Identificadores de ruta. | 200 ficha básica con version y fechas remotas vigentes. |

El POST no acepta ganaderiaId en cuerpo ni actorId, estado o versión resultante.
Body máximo propuesto: 16 KiB; sobre mayor -> 413, sin escritura. JSON inválido,
UUID mal formado o query inválida -> 400. Campos inválidos/no admitidos -> 422.
Esquema no soportado -> 422; baseVersion entero !=0 -> 409. Un 413 definitivo pasa a RECHAZADO (sin alta); un 400 exige corregir el formato, no reintento automático. La SPEC sección 7
rige el resto de códigos. Sin autenticación -> 401; sin permiso sobre ganadería
-> 403 genérico; animal ausente/no perteneciente -> 404 sin diferenciar casos.
Una colisión de operationId/animalId nunca devuelve datos de la fila colisionada.

Respuesta de error: codigo, mensajeSeguro, erroresPorCampo opcionales y
correlacionId; operationId solo si era válido y pertenece a la petición actual.
Recibo: IDs de operación/animal, resultado, versionCreada=1, confirmadoEn y
advertencias. No incluye tokens, historial ni diagnósticos.

Listado ordenado por creadoEn DESC, id DESC; cursor codifica esa última tupla,
se valida y se aplica siempre el filtro ganaderiaId autorizado. No concede acceso.
No devuelve total obligatorio ni promete snapshot transaccional entre páginas.
Con datos sin cambios, recorrer cursores no omite/duplica; ante altas concurrentes,
refrescar reinicia navegación. El cliente deduplica por animalId, conserva pendientes
propios y no presenta las páginas locales como lista completa. Lector puede leer
la copia descargada; no consulta la cola privada de otra cuenta.

Advertencia ARETE_COINCIDENTE compara areteFrente NFC/trim en mayúsculas entre
animales de la misma ganadería; no evalúa reverso como identificador individual.
Se calcula dentro del alta y queda en el recibo. La comprobación no bloquea ni
garantiza advertir otra alta concurrente aún no confirmada. Localmente se muestran
coincidencias conocidas, aclarando que la copia puede ser parcial o antigua.

## 5. Persistencia PostgreSQL y transacciones

Migraciones propuestas en backend, sin generarlas todavía:

- animal: PK id UUID; FK ganaderiaId; FK creadoPor; version positiva; estado ACTIVO
  para altas; constraints de sexo NOT NULL y enum MACHO/HEMBRA, compatibilidad sexo/categoría; nulls solo en campos opcionales
  y longitudes iguales a SPEC; nacimientoEstimado es obligatorio con fecha y null sin ella. Fecha futura se valida en aplicación con reloj inyectable.
- operacion_confirmada: PK operationId UUID global; FK a animal y ganadería/actor;
  datos de recibo y huella; UNIQUE animalId para una sola operación de creación.
  Sin caducidad/limpieza automática en este incremento.
- Índice de listado `(ganaderiaId, creadoEn DESC, id DESC)` e índice de arete
  normalizado por ganadería, no único. Índice de membresía por usuario/ganadería
  depende del diseño de Identidad; no duplicar su fuente de permisos.
- Autorización de consultas siempre por ganadería, no por creadoPor: familiares
  autorizados consultan los mismos animales confirmados.

Secuencia transaccional propuesta:

1. Validar autenticación y forma de sobre; iniciar unidad de trabajo PostgreSQL.
2. Leer/bloquear estado de cuenta, ganadería y membresía en orden estable.
   Las operaciones de revocación/desactivación deben usar los mismos registros
   y bloqueos incompatibles con la aceptación del alta. Coordinar con Identidad.
3. Revalidar permisos vigentes bajo esos bloqueos, antes de consultar recibos.
4. Reservar operationId con unicidad dentro de la transacción. Dos solicitudes
   idénticas concurrentes esperan por la unicidad; la perdedora relee el recibo
   confirmado y comprueba ámbito/huella. Nunca hacer check-then-insert sin constraint.
5. Para una reserva nueva, exigir inexistencia de animalId y baseVersion=0;
   insertar Animal y completar recibo en la misma transacción.
6. Commit de ambos; entonces responder. Si falla cualquiera, rollback completo,
   incluida la reserva; nunca persiste recibo de éxito sin Animal ni viceversa.

Reserva es interna y no confirmada; no se expone estado remoto pendiente.
Se propone FK de recibo a Animal diferible hasta commit y campos de recibo
obligatorios: la reserva ya contiene IDs, huella y resultado previsto, pero
solo se hace visible si Animal y recibo confirman juntos. Una caída antes del
commit revierte todo; una FK ausente al commit impide confirmar el recibo.
Tratar unicidad mediante operación atómica compatible con transacción/savepoint;
no capturar una violación y continuar usando una transacción ya abortada.

Orden observable de revocación: si revocación confirma primero, el alta se
rechaza; si el alta obtiene el bloqueo y confirma primero, ocurrió antes de la
revocación. Esto debe probarse con dos transacciones reales. Sin esa coordinación,
la precondición de seguridad DP01 no se considera satisfecha.
El replay revalida acceso incluso después de tener un recibo antiguo. No
sobrescribe el Animal actual con el contenido de la confirmación inicial.

No hay Redis en esta transacción ni en las lecturas privadas. No se publican
anuncios, ni se registran pagos, ventas, notificaciones o llamadas de IA.

## 6. IndexedDB, PWA y permisos locales

Propuesta: adaptador con API nativa IndexedDB; sin añadir biblioteca hasta
revisión. DB con object stores `animalesConfirmados`, `animalesPendientes`,
`operaciones` y `contextos`. Claves incluyen cuentaId/ganaderiaId; operación
única por operationId y pendientes únicos por animalId dentro de su contexto.
No guardar secretos ni respuestas privadas en Service Worker/Cache Storage.

Guardar alta = una transacción readwrite sobre pendientes y operaciones.
La UI espera al evento de finalización de la transacción, no solo al éxito
individual de un put. Al recibir recibo, actualizar cola y provisional en otra
transacción. Fallo en esa confirmación local conserva la posibilidad de reenvío.
Se puede usar el DTO del alta local como provisional confirmado; para el estado
remoto vigente se consulta GET. Recibo antiguo nunca degrada una copia version>1.

Para dos pestañas: reclamar operationId en transacción IndexedDB con lease de
60 s, dueño efímero de pestaña y timeout HTTP propuesto de 20 s. Al expirar lease
o detectar envío interrumpido se recupera la operación. No se mantiene una
transacción IndexedDB abierta durante HTTP. La unicidad remota es la defensa
final ante duplicación, suspensión de pestañas o reloj local alterado.

Mismo formulario confirmado conserva su operationId; doble clic se deshabilita
mientras se confirma el guardado. Formularios independientes pueden crear IDs
diferentes: el aviso de arete ayuda, no garantiza deduplicar animales físicos.
No editar un sobre en vuelo ni asumir que cerrar la pestaña cancela el alta.

Preparación: validar contexto online, obtener opt-in, cachear recursos estáticos,
probar apertura/escritura de IndexedDB y descargar páginas/fichas pedidas. Solo
mostrar «disponible sin conexión» al completar todo. Sin disponibilidad no se
promete registro offline. Precaché/versiones PWA no deben borrar la cola;
migraciones locales probarán copia existente y permitirán pausar actualización
si no pueden preservar operaciones. El protocolo schemaVersion=1 seguirá
aceptándose mientras existan operaciones soportadas pendientes, o dará rechazo
explícito sin eliminarlas.

La autorización offline tiene ventana propuesta de 168 horas, separada de la
conservación de registros. ContextoOffline incorpora autorizadoHasta y estado
VIGENTE/REQUIERE_REVALIDACION/BLOQUEADO; son metadatos de acceso, no TTL de cola.
Al alcanzar autorizadoHasta se bloquean consultas privadas, altas y envíos;
operaciones, sobres e IDs no se borran ni modifican por ese motivo. No se
configuran expiración, job de limpieza o migración que eliminen pendientes por edad.

Tokens solo en memoria; la renovación online depende del contrato de autenticación.
Token caducado sin red y contexto vigente permite trabajo local, nunca envío.
Contexto perdido o cierre explícito exige login online de la misma cuenta para
recuperar acceso, sin eliminar la cola. Volver la red dispara revalidación de
identidad, pertenencia y rol; solo el éxito renueva autorizadoHasta y permite
reanudar con los mismos operationId/animalId. Un 401 pausa y un 403 bloquea;
la revocación retira copias sincronizadas pero conserva pendientes bloqueados.
Cerrar sesión/cambiar cuenta limpia contexto/copias sincronizadas, conservando
por defecto los pendientes de la cuenta original sin vista ni envío para otra
cuenta. El descarte necesita confirmación separada e informada, no un timeout.

Esta protección es de acceso en la aplicación: no se promete cifrado ni borrado
remoto instantáneo, y el dueño del perfil podría inspeccionar IndexedDB. El reloj
local no sustituye permisos vigentes en servidor. Ver sección 8 de SPEC y DP06/DP07.

## 7. Sincronización, fallos y conflictos

Orquestador solo procesa colas de la cuenta/contexto activo y autorizado.
Una operación por solicitud, secuencial por contexto; no hay endpoint de lote.
Priorizar recuperación de operaciones con resultado desconocido, conservando IDs.
Red/5xx/429 y protocolo inválido usan el backoff de SPEC; 401 pausa; 403 bloquea
contexto; 409 requiere revisión; 400/413/422 permiten corregir únicamente después del
rechazo definitivo, creando nuevo operationId para el mismo animalId.

baseVersion=0 comprueba inexistencia en esta SPEC de creación. version=1 es la
primera versión remota; no hay endpoint PUT/PATCH ni edición offline de datos
confirmados. Ediciones futuras deberán usar su versión base y otra SPEC. No se
introduce un conflicto ficticio por cambiar cualquier dato de la ganadería.

Conflicto por ID ya existente no permite fusionar por parecido de nombre/arete.
La UI ofrece consultar el estado accesible y detener la operación para revisión;
si no puede resolverlo, conserva pendiente bloqueado sin automatizar otra alta.
Borrado local explícito no ejecuta DELETE remoto. Pérdida de datos del navegador
antes del envío no es recuperable por este flujo; se advierte esa limitación.

## 8. Seguridad y observabilidad

- HTTPS; Spring Security/JWT según ADR-07 y contrato de Identidad. Backend no
  confía en roles locales, IDs de actor ni permisos declarados por cliente.
- Lectura y escritura filtran siempre por ganadería autorizada. Revalidación
  precede replay para evitar filtrar recibos de un permiso revocado.
- Normalizar/validar en ambos extremos; salida como texto escapado. Sin HTML
  ejecutable ni consulta SQL construida con entradas.
- Sin payloads privados completos, JWT o credenciales en logs. Registrar código
  seguro, correlación, resultado técnico, latencia e intentos. Auditoría de alta
  conserva creadoPor/creadoEn en PostgreSQL; no es una garantía de entrega de red.
- Límites 16 KiB, paginación y backoff son propuestas revisables; limitación de
  solicitudes a nivel plataforma se definirá con autenticación, no se inventa proveedor.
- Datos locales no se consideran cifrados/aislados frente al dueño del perfil
  del navegador. Solo opt-in en dispositivo de confianza; no ofrecerlo como
  protección para equipos compartidos. DP06 requiere aceptación explícita.

## 9. Herramientas, pruebas y carga propuestas

Herramientas candidatas documentadas antes del código: JUnit 5, Spring Boot Test,
Testcontainers con PostgreSQL real controlado, Vitest, Playwright y k6. No se
instalan en esta tarea; versiones exactas/compatibilidades se fijarán al preparar
los proyectos. Sin secretos, con datos sintéticos y sin servicios de producción.
PU01–PU04 usan dobles de contratos; PI01–PI03 usan HTTP/API/casos de uso/BD real;
PI04 y EE prueban IndexedDB/Service Worker en navegador real, con inyección de
fallos además de desconexión de red. PI05 verifica ausencia de dependencia de
Redis/IA/notificaciones sin desplegar funcionalidades de esos módulos.

PC01 propuesto, por aprobar con DP07:

| Parámetro | Protocolo |
|---|---|
| Datos | 10 ganaderías sintéticas, 10 000 animales por ganadería; cuentas propietario/editor/lector y un usuario ajeno de control. |
| Carga | Rampa 0–100 usuarios virtuales en 2 min, 100 usuarios durante 10 min, descenso 1 min; carga cerrada, sin equiparar VU a solicitudes por segundo. |
| Mezcla | 80 % GET listado (20 elementos), 20 % GET ficha; pausa 1 s por iteración; usuarios autorizados y datos conocidos. |
| Medición | Latencia HTTP completa vista por cliente de prueba; reportar p50/p95/p99, throughput, errores y volumen. |
| Aceptación | AC01 p95 ≤ 2 s en agregado y por tipo de consulta; propuesta de 0 errores funcionales en escenario nominal. |
| Entorno | Registrar commit, versiones, CPU/RAM, disco, red, PostgreSQL/configuración/pool y herramientas antes de ejecutar; aún no definido ni medido. |
| Caché | Sin Redis privado. Comparar primeras consultas tras reinicio controlado y repetidas, identificando cachés de BD/OS; no etiquetarlas como caché Redis vacía/poblada. |
| Fallos | Ejecución de continuidad con Redis/IA/notificaciones inaccesibles; datos privados siguen consultándose. Fallos PostgreSQL se evalúan en integración, sin exigirle continuidad imposible sin fuente principal. |
| Exclusiones | IA, fotos y catálogo público. Las pruebas Redis de catálogo se realizarán con su SPEC. |

Registrar entorno y dataset antes de afirmar resultado; ninguna cifra describe
capacidad demostrada. No ejecutar carga mientras no exista el flujo funcional.

## 10. Dependencias, decisiones y aprobación

Aprobar DP01–DP08 de la SPEC y este diseño; especialmente contrato de acceso,
sexo obligatorio confirmado, límites/zonas horarias, UUID/recibos, recuperación y retención local.
El despliegue y las versiones de herramientas se concretarán antes del código
que las requiera. Sin implementación de membresías y su revocación transaccional,
solo podrán ejecutarse pruebas con datos controlados, nunca habilitar acceso real.
La UI de registro no amplía permisos para administrar ganaderías.

## 11. Referencias técnicas consultadas

- [RFC 9562, sección 5.4](https://www.rfc-editor.org/rfc/rfc9562.html#section-5.4): UUIDv4; el generador propuesto se apoya en aleatoriedad, no en datos del animal.
- [Web Cryptography API, randomUUID](https://w3c.github.io/webcrypto/#Crypto-method-randomUUID): mecanismo propuesto de generación en el cliente. Documento técnico en desarrollo; verificar soporte en navegadores objetivo antes de fijar versiones.
- [Indexed Database API](https://www.w3.org/TR/IndexedDB/): transacciones para guardar conjuntamente registros locales. Versión consultada en estado Working Draft; verificar comportamiento en navegador real.

Consultadas el 15 de noviembre de 2026 para fundamentar propuestas, no para
certificar compatibilidad ni cumplimiento del software. No se agregan bibliotecas
ni se afirman capacidades ya verificadas en GanadoConecta.
