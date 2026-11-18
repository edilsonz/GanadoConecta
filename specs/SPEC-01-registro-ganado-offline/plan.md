# SPEC-01 — Plan técnico propuesto

**Estado: Aprobada — Implementación en curso.** Plan aprobado documentalmente junto con
[spec.md](spec.md) y el contrato DP01, el 16 de noviembre de 2026 por autorización del responsable. La implementación comenzó con la persistencia parcial de T02.1 el 17 de noviembre; este plan no modifica los ADR aprobados ni declara completos los flujos funcionales. [Tareas](tasks.md).

## 1. Arquitectura y límites

React PWA usa un adaptador IndexedDB y un cliente REST. Spring Boot permanece
como monolito modular; PostgreSQL es la única fuente remota principal.
La sincronización entra por los casos de uso normales de alta. No hay servicio
nuevo, escritura directa desde el navegador a PostgreSQL ni dependencia del
núcleo Java respecto a Spring, JPA, Redis o APIs del navegador.

| Área prevista | Responsabilidad / contratos orientativos |
|---|---|
| backend/modulos/identidad | Consultar identidad autenticada y contexto autorizado de ganadería; contrato AutorizarAccesoGanaderia. Implementa el prerrequisito mínimo AU01–AU08; no invitaciones ni altas reales de editores/lectores. |
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
real. AU01–AU08 se ejecutan como prerrequisito T02; no se presupone que ya existan. La gestión de invitaciones, altas reales de editor/lector y el registro masivo necesitan otras SPEC;
esta sigue limitada al alta individual, ficha, listado y funcionamiento offline.
## 1.1. Contrato técnico DP01: acceso mínimo previo

**Aprobado documentalmente por el responsable.** AU01–AU08 se especifican en spec.md.
Son parte de Identidad en el monolito, no un servicio nuevo ni una invitación
completa. Usa Spring Security y JWT conforme a ADR-07; membresías conforme a
ADR-13 y persistencia PostgreSQL conforme a ADR-04. React muestra registro,
login, logout, selección/creación de ganadería y estados de sesión; no asignador
de roles ni pantalla de invitaciones. AU01 ya tiene implementación y pruebas
desde el formulario React hasta PostgreSQL. AU02–AU04 exponen en backend login,
renovación, logout y consulta de sesión con cookie/JWT; React integra esos contratos
con JWT solo en memoria, restauración y cierre. Su E2E en navegador HTTPS,
las operaciones restantes de AU07–AU08 y las demás pantallas de acceso continúan
pendientes. AU05 ya crea atómicamente ganadería y propietario inicial; el backend
lista solo membresías activas y React permite crear, listar y seleccionar en
memoria. AU06 expone el contexto online `/acceso`; su integración en React y la
preparación offline siguen pendientes.

### API de acceso y creación inicial

Todas las rutas bajo `/api/v1`, mismo origen HTTPS con frontend. Cuerpos JSON
máximo 16 KiB; validaciones de AU01/AU05, campos extras rechazados. Registro/
login solo online; contraseñas nunca se encolan. Respuestas privadas y de sesión
usan Cache-Control: no-store. Errores con código/mensaje seguro, sin trazas ni
confirmación pública de datos personales. Rate limit devuelve 429/Retry-After.

| Método/ruta | Entrada y autorización | Resultado verificable |
|---|---|---|
| GET /auth/csrf | Navegador del mismo origen; no exige login. | Token CSRF no autenticador en JSON para memoria y cookie de comprobación; no concede sesión ni membresías. |
| POST /auth/registro | Correo y contraseña; CSRF/origen válidos; sin rol ni ganadería. | 201 cuenta activa sin sesión/membresías; 422 inválido; 409 genérico si duplicado. |
| POST /auth/login | Correo/contraseña; CSRF/origen válidos. | 200 usuario mínimo, accessToken JWT y expiresAt UTC; credencial de renovación solo Set-Cookie HttpOnly. 401 genérico/429 por límite. |
| POST /auth/renovar | Cookie de renovación + CSRF/origen; no exige JWT vigente. | 200 nuevo accessToken/expiresAt y nueva cookie refresh tras rotación atómica si sesión/cuenta/token vigentes; anterior consumido. Reutilización revoca familia y devuelve 401. No renueva membresías offline por sí solo. |
| POST /auth/logout | Cookie de renovación + CSRF/origen; funciona aunque JWT esté vencido. | 204 idempotente, revoca sesión reconocida por refresh activo o histórico y todos sus verificadores; elimina cookie con mismos atributos; sesión ya expirada también 204. Sin CSRF válido -> 403 CSRF_INVALIDO, sin efectos. |
| GET /auth/me | JWT válido y sesión/cuenta vigentes. | 200 id/estado de cuenta, sin hash/token de renovación; 401 en caso contrario. |
| POST /ganaderias | JWT válido, CSRF; id UUIDv4 y nombre. | 201 ganadería + membresía PROPIETARIO del actor en una transacción; replay mismo id/actor/nombre -> 200; otro caso -> 409 seguro. |
| GET /ganaderias | JWT válido. | 200 solo ganaderías con membresía activa de esa cuenta y rol vigente. No devuelve miembros/datos de ganaderías ajenas. |
| GET /ganaderias/{id}/acceso | JWT válido y membresía activa. | 200 contexto propio (rol, estado, validadoEn UTC); 403 genérico si no autorizado. Solo éxito permite preparar/renovar ventana offline de esa ganadería. |
| DELETE /ganaderias/{id}/membresias/{usuarioId} | Propietario vigente de esa ganadería, CSRF. | 204 revocación de editor/lector existente, incluso si ya revocado; 404 genérico para objetivo inexistente/ajeno; 409 si objetivo es propietario; 403 si actor no tiene permiso. |

Validar UUID/ruta/JSON antes de acceder a datos; respuestas de autorización no
revelan datos de otro ámbito. Las rutas de animales mantienen los contratos
anteriores. Todo endpoint protegido requiere sesión/cuenta vigentes además de
la firma JWT. Cada operación que menciona una ganadería evalúa membresía/rol
actuales, incluidos GET /acceso, consulta de ficha y replay de operationId.

### Cuenta, credenciales y sesión

Contrato DP01 aprobado con rotación obligatoria de renovación:

- Correo normalizado trim/minúsculas único; formato válido y máximo 254.
  Es identificador declarado, no prueba de correo verificado; no se usa para
  conceder acceso a una ganadería existente. Verificación y recuperación de
  contraseña requieren otra SPEC/proveedor y no se simulan como disponibles.
- Contraseña de 15–128 caracteres Unicode, preservada exactamente sin trim,
  normalización ni truncamiento; sin reglas de composición inventadas. Hash
  Argon2id con salt aleatorio por contraseña mediante PasswordEncoder del
  adaptador Spring. Para AU01 se fijaron salt 16 bytes, hash 32 bytes,
  paralelismo 1, memoria 65 536 KiB y 3 iteraciones; la medición real queda en
  la evidencia y deberá repetirse en el entorno de despliegue.
  No SHA-256 simple para contraseñas. Nunca devolver ni registrar contraseña/hash.
- Registro no acepta roles, estado ni propietario en cuerpo. Ningún usuario
  puede elegir ADMINISTRADOR o pertenecer a una ganadería por conocer su ID.
- Login con errores indistinguibles de cuenta inexistente/inactiva/contraseña;
  verificar hash de coste equivalente cuando no exista usuario. Límite de diez
  fallos en 15 min por correo normalizado o por origen, independiente de que
  exista la cuenta; sin bloqueo permanente. Contadores con persistencia
  controlada de Identidad/PostgreSQL, no Redis de catálogo. No loggear cuerpos.
- Sesión de dispositivo: id interno aleatorio, usuarioId, creadoEn UTC,
  venceEn=creadoEn+24 h y revocadoEn nullable; una familia de verificadores de
  credenciales de renovación aleatorias de 256 bits. No guardar credenciales en claro en servidor.
  SHA-256 es adecuado como huella del valor aleatorio, distinto del hash de contraseña.
- JWT RS256 de duración 10 min, con sub, sid, jti, iat, exp, iss y aud. Validar
  algoritmo permitido, firma, emisor, audiencia y tiempos; claves de firma en
  configuración segura fuera del repo. No aceptar el algoritmo del solicitante
  ni tratar roles de JWT como fuente de autorización. Expiración se evalúa con
  reloj de servidor; a exp no se permite acceso.
- Cada petición con JWT verifica sesión no revocada/no caducada y cuenta activa
  en PostgreSQL. Por eso se rechaza un JWT previo al logout aunque aún esté dentro de sus 10 min: no se modifica ni invalida mágicamente su firma.
  No se promete JWT completamente stateless ni revocación solo por vencimiento.
- Cada renovación válida rota la credencial opaca de un solo uso y emite nuevo
  JWT (misma sid, nuevo jti). En una transacción, bloquear sesión y verificador,
  exigir token ACTIVO, marcarlo CONSUMIDO e insertar el hash del sucesor aleatorio
  de 256 bits. Solo tras commit se envían JWT y Set-Cookie con el sucesor.
  Si falla antes del commit, rollback completo; no emitir credencial de un cambio
  no confirmado. exp del JWT no supera diez minutos ni el fin absoluto de las
  24 h. Rotar nunca prolonga esa sesión.
- No guardar refresh en claro, tampoco el sucesor en el historial del servidor.
  Mantener verificadores únicos (SHA-256), sid/familia, estado ACTIVO/CONSUMIDO/
  REVOCADO, consumidoEn y vínculo de sucesión. Los consumidos no se aceptan de
  nuevo y se conservan al menos hasta vencer la sesión para detectar reutilización.
  Solo un token ACTIVO por sesión; emisión/consumo/revocación se serializan bajo
  bloqueo de sesión. Logout revoca sesión y todos sus verificadores.
- Reutilizar un token CONSUMIDO reconocido revoca toda su sesión/familia en una
  transacción y devuelve 401: todos los JWT con esa sid serán rechazados por la
  validación de estado, aunque la firma/exp sigan siendo válidas. Un token aleatorio
  desconocido solo produce 401; no revoca sesiones de terceros. Una revocación
  de membresía impide el acceso a esa ganadería incluso con JWT anterior, sin
  conceder derechos por sus claims ni necesariamente cerrar otras ganaderías.
- React serializa renovar entre pestañas mediante coordinación de mismo origen
  (bloqueo o lease compartido de metadatos, nunca tokens en IndexedDB). Dos
  peticiones que usen simultáneamente el mismo refresh no obtienen dos sucesores
  válidos: como máximo una rota, la repetida revoca la familia y exige login.
  La coordinación reduce falsos positivos; el servidor sigue siendo la defensa.
- Una respuesta de renovación perdida después del commit puede dejar la cookie
  vieja o nueva en el navegador. No se reenvía automáticamente la petición de
  refresh con resultado incierto ni se concede periodo de gracia al token usado:
  pausar y pedir login, que revoca la sesión previa reconocida por cookie incluso
  si esta contiene un token consumido. Conservar toda la cola y sus IDs. No
  confundir esa renovación no idempotente con el alta idempotente de animales.
- Cookie de renovación `__Host-GC-Refresh`: Secure, HttpOnly, SameSite=Strict,
  Path=/, sin Domain, Max-Age no superior al vencimiento de sesión. React no la
  lee ni la copia; el navegador la envía solo al mismo origen. JWT de acceso solo
  en memoria; recarga solicita renovación y comprueba contexto, o pide login.
  No guardar contraseñas, JWT o refresh en IndexedDB, localStorage o Cache Storage.
- Cada login exitoso revoca la sesión previa asociada a la cookie de ese
  navegador antes de emitir otra. Otros dispositivos mantienen sus sesiones;
  logout actual no es «cerrar todas las sesiones». Cuenta desactivada se rechaza
  en todas las sesiones por verificación de cuenta.

### CSRF, React y cierre sin conexión

Protección CSRF de Spring para todos los métodos que cambian estado,
incluidos registro, login, renovar y logout. GET /auth/csrf proporciona un token
en JSON no-store para memoria y cookie de comprobación `__Host-GC-CSRF`
(Secure, HttpOnly, SameSite=Strict, Path=/, sin Domain); React envía el token en
X-CSRF-TOKEN. Tras login/logout se vuelve a obtener token CSRF si se invalida.
El token CSRF no autentica y no se persiste en IndexedDB/localStorage.

Validar Origin contra el origen configurado, sin comodines; si falta Origin
exigir Referer del mismo origen en métodos de cambio, o rechazar. Producción
frontend/API bajo un mismo origen; desarrollo mediante proxy del frontend,
sin habilitar CORS con credenciales para orígenes arbitrarios. Cookie Strict
no sustituye CSRF. La configuración concreta debe probarse en navegador real.

React distingue sesión online de permiso offline. El logout local limpia JWT,
contexto y copias sincronizadas; conserva operaciones pendientes bloqueadas.
Si no obtiene confirmación remota, guarda solo una marca no secreta
salidaRemotaPendiente, sin tokens. Al volver la red obtiene CSRF y procesa logout
antes de renovar/abrir datos de esa sesión. Un nuevo login revoca la cookie/sesión
previa antes de emitir otra; entonces cancela la marca de cierre anterior.
Nunca ejecutar después un cierre pendiente antiguo contra una sesión nueva.
Si falla red/CSRF se mantiene bloqueado y se informa «Cierre remoto pendiente».
No se afirma que otros dispositivos hayan sido desconectados ni se borra su cola.

### Modelo e invariantes transaccionales de Identidad

El modelo siguiente fue conceptual al aprobarse la SPEC. Su traducción física,
limitada a Identidad, se implementó con Flyway V1/V2 y se amplió con V3 para el
límite persistente de fallos de login el 17 de noviembre de 2026. El backend de
AU02–AU04, incluidos adaptadores HTTP, cookie, filtro JWT y logout, está probado:

| Entidad | Restricciones/operación |
|---|---|
| Usuario | id UUID generado por servidor, emailNormalizado UNIQUE, hashPassword, estado; datos privados, nunca hash en DTO. |
| SesionDispositivo | id aleatorio (sid/familia), FK usuario, creadoEn/venceEn UTC, revocadoEn nullable; validar para cada JWT y renovación. |
| VerificadorRenovacion | huella UNIQUE, FK sesión/familia, estado ACTIVO/CONSUMIDO/REVOCADO, creadoEn/consumidoEn UTC y sucesor; un ACTIVO por sesión. Sin token en claro. |
| Ganaderia | id UUIDv4 de petición, nombre, estado, propietarioUsuarioId FK; instantes UTC. |
| Membresia | UNIQUE usuarioId+ganaderiaId, FKs, rol cerrado PROPIETARIO/EDITOR/LECTOR, estado ACTIVA/REVOCADA. PropietarioUsuarioId debe tener membresía PROPIETARIO activa al confirmar creación. |

La migración V1 crea el esquema PostgreSQL `identidad` y las cinco tablas
`usuario`, `ganaderia`, `membresia`, `sesion_dispositivo` y
`verificador_renovacion`. Usa UUID como claves estables, FKs restrictivas,
unicidad de correo normalizado y membresía, un propietario activo por ganadería,
un verificador activo por sesión y una huella SHA-256 de 32 bytes en lugar del
token de renovación. Un trigger de restricción diferible permite crear ganadería
y membresía propietaria en una misma transacción, pero impide confirmarla huérfana.
La V2 fija en la base el máximo de 24 horas de AU03 para la sesión online. V3
agrega `intento_login`, que conserva solo huellas SHA-256 por correo/origen,
ventana, contador y bloqueo temporal; no guarda esos identificadores en claro. Flyway
valida las migraciones al iniciar el perfil `postgres`; `clean` está deshabilitado.

Esta persistencia es una parte de T02.1, no su cierre: ya existen el modelo,
puerto, caso de uso, adaptador JDBC y endpoint de AU01 con Argon2id, además de
sesión/rotación, endpoints backend AU02–AU04 y cliente React automatizado. Falta
validar coordinación multipestaña en navegador real. AU05 ya usa los modelos
funcionales de ganadería/membresía para crear al propietario inicial; listado y
contexto online ya revalidan membresía activa. Faltan autorización de futuros
recursos y revocación de AU07–AU08. La base nunca
almacena contraseñas en claro ni tokens de renovación. No se agregaron datos
semilla ni membresías reales de editor/lector.

Alta de cuenta y contraseña codificada confirma una sola cuenta. AU05 crea
Ganaderia+Membresia juntas con unicidad de ID y recibo lógico por consulta de
mismo ID/actor/nombre; no confirmar ganadería huérfana. Índice parcial único
sobre propietario activo por ganadería y validación de pertenencia al propietario
mantienen un solo propietario; no se permite su revocación/cambio en este contrato.

Autorización en cada petición, también lecturas, no solo al seleccionar ganadería.
Los casos de uso reciben identidad confiable del adaptador de seguridad y usan
contratos de sesión/membresía, sin exponer JWT/JPA al núcleo. Revocar no elimina
historial/animal/recibo ni transfiere propiedad. Para alta de animal y revocación,
se usa el orden estable de bloqueos de sección 5; una revocación confirmada antes
del alta impide esa escritura. Logout y alta protegida coordinan igualmente
estado de sesión para no aceptar escrituras posteriores al cierre confirmado.

### Aprovisionamiento mínimo, sin invitaciones

El único aprovisionamiento real de membresía en esta entrega es AU05, creador
como propietario. No hay POST de asignación de EDITOR/LECTOR, ni asignación
por correo no verificado, ni endpoint/seed/CLI de test en producción.

Los fixtures de integración crean usuarios sintéticos y membresías de editor,
lector, propietario y revocadas en dos ganaderías controladas; las pruebas
entran por login real. Se cargan únicamente en perfil/entorno test, sin contraseñas
reales ni secretos versionados. Su ausencia debe verificarse en artefacto/config
productivo. AU07 permite revocar miembros existentes, pero no incorporarlos.
El alta real de familiares y su invitación aceptada quedan para otra SPEC y
no se declaran implementadas ni completadas como parte de RF21.

### Cierre de DP01 y verificaciones obligatorias

DP01 y SPEC-01 quedan aprobadas documentalmente por autorización expresa del responsable el 16 de noviembre de 2026, con rotación obligatoria, verificadores seguros, comprobación de sesión/membresía y protección CSRF. No queda una credencial estable de renovación ni un contrato indefinido. La aprobación no demuestra ejecución ni habilita datos reales antes de cumplir los prerrequisitos.
T02.1–T02.8 y PA01–PA08 son prerrequisitos de habilitación de animales reales.
Las 168 h offline son decisión DP06 ya adoptada; no son duración de JWT o cookie.
El algoritmo de firma, hash adaptativo y claves son infraestructura, compatibles
con Clean Architecture y ADR-07; no se cambia tecnología ni requisito aprobado.

## 2. Entidades y representación propuesta

| Concepto | Datos / invariantes |
|---|---|
| Ganaderia (creada por AU05) | id, estado y propietario; la asociación del animal no cambia en este flujo. |
| Membresia (prerrequisito de Identidad) | usuarioId + ganaderiaId, rol PROPIETARIO/EDITOR/LECTOR, estado; autorización vigente. |
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
-> 403 con código seguro MEMBRESIA_REVOCADA/ROL_INSUFICIENTE según contexto, sin detalles de terceros; animal ausente/no perteneciente -> 404 sin diferenciar casos.
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

Migraciones de animales propuestas en backend, sin generarlas todavía:

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
2. Leer/bloquear estado de sesión, cuenta, ganadería y membresía en orden estable.
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

La autorización offline tiene máximo adoptado de 168 horas, separada de la
conservación de registros. ContextoOffline incorpora autorizadoHasta y estado
VIGENTE/REQUIERE_REVALIDACION/BLOQUEADO; son metadatos de acceso, no TTL de cola.
Al alcanzar autorizadoHasta se bloquean consultas privadas, altas y envíos;
operaciones, sobres e IDs no se borran ni modifican por ese motivo. No se
configuran expiración, job de limpieza o migración que eliminen pendientes por edad.

JWT de acceso solo en memoria; credencial de renovación solo en cookie segura HttpOnly según DP01, nunca en almacenamiento offline.
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
Red/5xx/429 y protocolo inválido usan el backoff de SPEC; 401 pausa; solo 403 por membresía/rol bloquea
contexto. Un 403 CSRF_INVALIDO conserva copias/cola: recuperar CSRF y reintentar una sola vez el sobre de animal con mismos IDs, luego pausar si persiste. 409 requiere revisión; 400/413/422 permiten corregir únicamente después del
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
- El límite de 16 KiB se aplica a AU01. Como protección inicial se adoptó
  provisionalmente un máximo configurable de 10 intentos de registro por origen
  en 15 minutos, local a cada instancia y sin confiar en cabeceras de proxy.
  Requiere aprobación/estrategia distribuida antes de producción; no cambia el
  límite persistente de fallos de login aún pendiente.
- Datos locales no se consideran cifrados/aislados frente al dueño del perfil
  del navegador. Solo opt-in en dispositivo de confianza; no ofrecerlo como
  protección para equipos compartidos. DP06 requiere aceptación explícita.

## 9. Herramientas, pruebas y carga propuestas

Herramientas previstas conforme a DP07, documentadas antes del código: JUnit 5, Spring Boot Test,
Testcontainers con PostgreSQL real controlado, Vitest, Playwright y k6. No se
instalan en esta tarea; versiones exactas/compatibilidades se fijarán, conforme a DP07 adoptada, al preparar
los proyectos. Sin secretos, con datos sintéticos y sin servicios de producción.
PU01–PU04 usan dobles de contratos; PI01–PI03 usan HTTP/API/casos de uso/BD real;
PI04 y EE prueban IndexedDB/Service Worker en navegador real, con inyección de
fallos además de desconexión de red. PI05 verifica ausencia de dependencia de
Redis/IA/notificaciones sin desplegar funcionalidades de esos módulos.

PC01 documentado conforme a DP07 adoptada; versiones y entorno por fijar en T03:

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

Los usuarios virtuales deben autenticarse y renovar por DP01; preparar sesiones
antes de medir consultas y mantenerlas vigentes durante PC01. Medir aparte
login/renovación; la latencia de cada consulta incluye la comprobación real de
sesión y membresía, no se elude seguridad para lograr el objetivo.

Registrar entorno y dataset antes de afirmar resultado; ninguna cifra describe
capacidad demostrada. No ejecutar carga mientras no exista el flujo funcional.

## 10. Dependencias, decisiones y aprobación

DP01–DP08 y SPEC-01 aprobadas documentalmente. Ejecutar sus tareas y verificar especialmente contrato de acceso,
sexo obligatorio confirmado, límites/zonas horarias, UUID/recibos, recuperación y retención local.
El despliegue y las versiones de herramientas se concretarán antes del código
que las requiera. Sin implementación de membresías y su revocación transaccional,
solo podrán ejecutarse pruebas con datos controlados, nunca habilitar acceso real.
La UI de registro no amplía permisos para administrar ganaderías.

Los instantes de servidor y sesión se almacenan como instantes UTC (PostgreSQL timestamptz, serialización ISO 8601 con Z); conexiones configuradas UTC. FechaNacimiento permanece tipo date; la validación de fecha futura usa America/Lima. No confundir conversión de presentación con cambio de la fecha de negocio.

## 11. Referencias técnicas consultadas

- [RFC 9562, sección 5.4](https://www.rfc-editor.org/rfc/rfc9562.html#section-5.4): UUIDv4; el generador propuesto se apoya en aleatoriedad, no en datos del animal.
- [Web Cryptography API, randomUUID](https://w3c.github.io/webcrypto/#Crypto-method-randomUUID): mecanismo propuesto de generación en el cliente. Documento técnico en desarrollo; verificar soporte en navegadores objetivo antes de fijar versiones.
- [Indexed Database API](https://www.w3.org/TR/IndexedDB/): transacciones para guardar conjuntamente registros locales. Versión consultada en estado Working Draft; verificar comportamiento en navegador real.

Consultadas el 15 de noviembre de 2026 para fundamentar propuestas, no para
certificar compatibilidad ni cumplimiento del software. No se agregan bibliotecas
ni se afirman capacidades ya verificadas en GanadoConecta.

Referencias adicionales para el contrato DP01, consultadas el 16 de noviembre de 2026:

- [Spring Security: Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html): contratos PasswordEncoder y hash adaptativo; no se adopta una versión del framework por la documentación consultada.
- [Spring Security: CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html): protección de peticiones de cambio y prueba de integración del cliente.
- [OWASP: Session Management](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html): cookies seguras, vencimiento y cierre de sesión.

Las duraciones y mecanismos concretos son decisiones de diseño de GanadoConecta aprobadas documentalmente,
no una afirmación de que estas fuentes exijan exactamente esos valores.
