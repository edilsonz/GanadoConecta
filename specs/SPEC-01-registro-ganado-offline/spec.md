# SPEC-01: Registro flexible de ganado con acceso familiar y funcionamiento sin conexión

## 1. Identificación

| Campo | Valor |
|---|---|
| Estado | **Aprobada — Implementación en curso** |
| Fecha | Revisión DP01: 16 de noviembre de 2026; borrador inicial: 15 de noviembre de 2026 |
| Requisitos | RF01 (registro/login y sesión mínima), RF02 y RF17; RF21 para pertenencia y permisos; RF20 solo alta individual. Perfil completo e invitaciones fuera de alcance. |
| Historias | HU01 (registro/login), HU02, HU18, HU21 (alta individual), HU22 (propietario inicial, pertenencia y revocación; sin invitaciones) |
| Calidad | AC01, AC02, AC04, AC05, AC06, AC08, AC09 |
| Drivers / ADR | DA01, DA02, DA06, DA07, DA09; ADR-01, ADR-02, ADR-03, ADR-04, ADR-07, ADR-12, ADR-13 |
| Escenarios | EC01, EC02, EC04, EC06, EC08, EC11, EC12 |
| Documentos | [Plan](plan.md), [tareas](tasks.md), [plantilla original](../plantilla-spec.md) |

**Base aprobada:** React PWA/IndexedDB, Spring Boot monolito modular/Clean
Architecture, PostgreSQL principal, identidad interna única, datos opcionales,
acceso familiar autorizado, permisos/versiones/duplicados al sincronizar.
**Corte de evidencia previo al incremento del 20 de noviembre:**
**Decisiones adoptadas por el responsable en esta revisión:** DP02–DP08 según la sección 13, incluidas identidad UUIDv4, sexo obligatorio, alcance individual, conflictos conservadores, máximo offline 168 h sin borrado de pendientes, pruebas y zona America/Lima con instantes UTC. DP01 y SPEC-01 se aprobaron documentalmente el 16 de noviembre de 2026. Desde el 17 de noviembre existe implementación incremental de Identidad y ganaderías; el 18 de noviembre se verificaron T04–T10 para alta, listado y ficha backend y su cliente React online. El 19 de noviembre de 2026 el responsable autorizó y se verificó el cambio controlado T19-ID para identificar opcionalmente mediante lana/cinta en las orejas; también se implementaron el almacenamiento T11, la barrera local inicial T12/T15, el primer envío manual T13/T14 por cuenta y ganadería y un app shell PWA instalable. La instalación y una recarga F5 offline se observaron en Chrome. La sincronización manual revalida sesión, membresía y rol online, conserva IDs y recibos y nunca usa el contexto local como autorización del servidor. La PWA no cachea respuestas privadas ni sincroniza automáticamente; las actualizaciones explícitas respetan formularios y sincronización en curso. Cierre/reapertura completa, actualización A→B, caché privada autorizada, automatización, backoff programado y E2E integral siguen pendientes. No se modifican los RF/ADR aprobados ni se declara completo CA03.

**Estado actual del incremento:** T13/T14 añade opt-in efímero para sincronización
automática en primer plano y el backoff aprobado, reutilizando el mismo motor
seguro. La PWA no cachea respuestas privadas ni envía desde el Service Worker.
Background Sync, caché privada autorizada, conflictos asistidos y E2E integral
continúan pendientes; CA03 no se declara completo.

Fuentes locales: [RF](../../docs/01-analisis-de-sistema/03-requisitos-funcionales.md),
[HU](../../docs/01-analisis-de-sistema/02-historias-de-usuario.md),
[AC](../../docs/01-analisis-de-sistema/04-atributos-de-calidad.md),
[ADR](../../docs/01-analisis-de-sistema/07-decisiones-arquitectonicas.md),
[modelo conceptual](../../docs/05-preparacion-implementacion/02-modelo-datos-y-reglas.md),
[consolidación](../../docs/05-preparacion-implementacion/05-consolidacion-arquitectonica.md).

## 2. Objetivo

Un propietario o editor autorizado registra un vacuno individual, lo ve en
«Mi ganado» y consulta su ficha básica. Si pierde conexión en un dispositivo
preparado, conserva el alta local y la sincroniza posteriormente sin duplicar
el efecto de la operación, manteniendo el aislamiento entre ganaderías.

## 3. Actores y permisos

Los roles se asignan a una cuenta dentro de cada ganadería, no globalmente.
El propietario se representa como una membresía vigente con rol PROPIETARIO.

| Acción dentro de esta SPEC | Propietario | Editor | Lector |
|---|---|---|---|
| Consultar lista y ficha básica de su ganadería | Sí | Sí | Sí |
| Crear animal online u offline preparado | Sí | Sí | No |
| Reenviar su propia alta pendiente | Sí | Sí | No |
| Consultar copias locales ya descargadas | Sí | Sí | Sí |
| Editar animal ya confirmado, eliminar o trasladar | Fuera de alcance | Fuera de alcance | Fuera de alcance |
| Crear ganadería nueva y membresía inicial (AU05) | Sí, como usuario autenticado | Sí, queda propietario de la nueva | Sí, queda propietario de la nueva |
| Revocar membresía editor/lector existente | Sí, únicamente en su ganadería | No | No |
| Invitar/asignar editores o lectores, cambiar roles, transferir propiedad o publicar anuncios | Fuera de alcance | Fuera de alcance | Fuera de alcance |

Ningún rol obtiene acceso a ganaderías ajenas. El administrador de plataforma
no hereda estos permisos; requiere su propia membresía para estas operaciones.
Un familiar no reenvía la cola privada de otra cuenta. La ocultación de botones
no sustituye la autorización del backend. No se acepta actorId desde el cuerpo.

## 4. Alcance

Incluye alta individual, listado paginado, ficha básica, preparación explícita
del dispositivo, copia de las páginas/fichas descargadas, cola de altas y
sincronización manual o automática mientras la aplicación está abierta.
Se admite el mismo flujo de operación online y offline, con reintentos seguros. DP01 añade como prerrequisito verificable registro/login/sesión mínima, creación de ganadería y propietario inicial, consulta de contexto y revocación limitada; se implementan antes de habilitar animales reales.

No incluye edición de animales confirmados, importación masiva, invitaciones,
altas reales de editores/lectores, recuperación de contraseña, verificación de correo,
MFA, transferencia de propietario ni administración completa de cuentas; tampoco nacimientos,
genealogía, tratamientos, campañas, peso, venta, anuncios, IA, notificaciones
ni fotografías. Esas capacidades siguen en el alcance del producto y necesitan
otras SPEC. No se crea un microservicio de sincronización.
La venta atómica aprobada conserva su significado: estado del animal y cierre
de anuncios en PostgreSQL, sin pagos ni transacciones comerciales; no se ejecuta aquí.

## 5. Precondiciones y dependencias

- Cuenta autenticada, ganadería existente y membresía activa, con rol definido.
- El contrato de autenticación y validación/revocación de membresías debe estar
  aprobado e implementado antes de exponer estas rutas a usuarios reales.
  Los fixtures de prueba no sustituyen la autenticación de producción.
- Para offline: primera carga online por HTTPS, recursos PWA disponibles,
  IndexedDB funcional y preparación voluntaria en dispositivo de confianza.
- El usuario elige la ganadería antes del alta. El contexto visible indica
  ganadería, última comprobación de permisos y fecha de la copia descargada.
- Si falta almacenamiento o generación segura de identificadores, se informa
  el problema; no se afirma que hubo un guardado local.

### 5.1. Contrato mínimo DP01 aprobado documentalmente (prerrequisito)

Este contrato guía la implementación incremental: AU01 y AU02–AU04 cuentan con
backend y cliente React automatizado; AU05, el contexto online AU06 y la
revocación backend AU07 están implementados. Las comprobaciones integrales y la
continuidad offline de AU08 permanecen pendientes.
La ampliación se justifica
por RF01/RF21 y ADR-07/ADR-13: reemplaza la dependencia indefinida de acceso
por operaciones mínimas verificables, sin alterar arquitectura ni crear servicios.

| ID | Operación/regla | Resultado observable requerido |
|---|---|---|
| AU01 | Registro de usuario | Correo válido de hasta 254 caracteres, normalizado trim/minúsculas y único; contraseña de 15–128 caracteres, sin recorte, normalización ni truncamiento. Campos rol, ganadería o estado no se aceptan. Crea cuenta activa sin membresías ni sesión automática. Validación inválida -> 422, duplicado -> 409 genérico, sin mostrar datos de otra cuenta. |
| AU02 | Login | Correo/contraseña correctos y cuenta activa crean sesión de dispositivo; errores -> 401 genérico, sin distinguir cuenta ausente, inactiva o contraseña incorrecta. Diez fallos por correo normalizado u origen en 15 min producen 429 temporal; sin bloqueo permanente. |
| AU03 | Caducidad y renovación | JWT en memoria de 10 min; sesión online absoluta máxima de 24 h. Renovar rota atómicamente token opaco de un solo uso y emite JWT sin alargar las 24 h; anterior queda consumido. Reutilizarlo revoca sesión/familia y requiere login. Verificadores seguros en servidor, ningún token en claro ni en IndexedDB. |
| AU04 | Logout y JWT previos | Con conexión revoca estado de sesión y verificadores de renovación; cada petición posterior comprueba ese estado y rechaza JWT anteriores, incluso sin expirar. No se afirma invalidez automática del JWT. Sin red bloquea localmente y conserva pendientes; procesa cierre remoto antes de reactivar acceso al reconectar. |
| AU05 | Crear ganadería | Usuario autenticado envía ID UUIDv4 y nombre (1–100 caracteres, NFC/trim). En una transacción crea ganadería activa, propietarioUsuarioId y membresía activa PROPIETARIO del actor. No acepta propietario/rol de otro usuario; falla -> rollback de ambos. Repetir mismo ID/actor/nombre devuelve la misma ganadería; otro contenido/actor -> conflicto genérico. |
| AU06 | Contexto y autorización | Una membresía única por (usuarioId, ganaderiaId), rol PROPIETARIO/EDITOR/LECTOR y estado ACTIVA/REVOCADA. Cada petición privada valida sesión/cuenta, ganadería, membresía y permiso en backend; los roles del cliente/JWT no otorgan acceso. Propietario/editor crean y leen animales, lector solo lee. Usuarios de otra ganadería no consultan/modifican ni reciben información de ella. |
| AU07 | Revocar | Solo propietario vigente puede revocar una membresía EDITOR/LECTOR existente de su ganadería, sin borrar la fila. No puede revocarse al propietario por esta operación; no hay transferencia de propiedad. Repetir revocación autorizada es idempotente. Las operaciones posteriores se rechazan; carrera con alta de animal sigue el orden de bloqueos del plan. |
| AU08 | Seguridad y continuidad offline | Contraseñas mediante hash adaptativo Argon2id y salt; renovación solo en cookie segura HttpOnly, nunca en IndexedDB/localStorage. Acceso JWT solo en memoria. HTTPS, mismo origen y protección CSRF en operaciones que cambian estado. Vencer sesión/renovación no borra pendientes ni equivale a vencer el permiso offline; revalidación exige sesión y membresía actuales. |

Los plazos 10 min/24 h son de sesión; las 168 h son autorización local separada.
Tras cerrar sesión o perder el contexto preparado no hay acceso offline hasta
login online; la cola se conserva. Con token expirado pero contexto preparado
vigente puede seguir trabajando sin red, sin enviar. Revocación o desactivación
no puede retirarse instantáneamente de un dispositivo desconectado: al volver
la red se comprueban permisos antes de transmitir o descargar, se retiran copias
sincronizadas y se bloquean pendientes sin borrarlos. El dueño del perfil del
navegador puede inspeccionar copias locales; no se promete cifrado local.

### 5.2. Membresías iniciales y límite de acceso familiar

El mecanismo real mínimo es AU05: toda nueva ganadería obtiene al creador como
único propietario, sin endpoint público de asignación arbitraria de roles.
Su registro/login/ganadería le permiten utilizar el alta individual una vez
implementado este prerrequisito. Una cuenta puede pertenecer a varias ganaderías;
la membresía y el rol se evalúan por ganadería, no globalmente.

No hay alta real de editores/lectores ni invitaciones en este incremento.
Se incorporarán mediante otra SPEC de acceso familiar. Hasta entonces sus
permisos/revocación se verifican con usuarios y membresías sintéticos cargados
por fixtures exclusivos del entorno de pruebas; no se instala endpoint de test,
cuenta compartida, asignador de roles ni script de aprovisionamiento en producción.
Los fixtures no reemplazan autenticación real: las pruebas integrales inician
sesión por el mismo contrato. AU07 cubre miembros existentes y su transición,
pero no los crea. No se declara RF21 completo ni acceso familiar implementado.

## 6. Flujo principal

1. El propietario/editor abre «Mi ganado», selecciona ganadería y «Registrar animal».
2. Selecciona sexo (MACHO o HEMBRA), agrega los datos opcionales conocidos y revisa el resumen antes de confirmar. Sin sexo válido no puede guardar.
3. El cliente valida, crea animalId y operationId distintos y guarda el animal
   provisional y la operación en una misma transacción local.
4. Solo al completar esa transacción muestra «Guardado en este dispositivo ·
   pendiente de sincronización». Evita dos envíos por doble pulsación.
5. Si hay conexión, sesión válida y permisos verificables, envía la operación.
6. El backend valida identidad, estado de cuenta/ganadería, membresía, rol,
   esquema, identificadores, versión y deduplicación; confirma alta y resultado
   de operación juntos en PostgreSQL, o no confirma ninguno.
7. La respuesta válida hace que el cliente marque la operación sincronizada
   y sustituya el provisional por el mismo animalId confirmado, nunca una segunda ficha.
8. El listado/ficha muestran la versión remota y fecha de confirmación. Si se
   vuelve a consultar, se respeta la versión vigente más reciente del servidor.

Sin conexión se detiene después del paso 4. Al recuperar red, abrir la aplicación
o pulsar «Sincronizar ahora», se reanuda desde el paso 5. El evento de conexión
es una señal para intentar; una respuesta real determina si el servidor está disponible.
No se depende de ejecución en segundo plano con el navegador cerrado.

## 7. Flujos alternativos, fallos y estados

El estado de negocio ACTIVO es independiente del estado local de sincronización.
Un formulario no confirmado es un borrador en memoria, no un animal guardado.

| Estado local | Significado y transición |
|---|---|
| PENDIENTE | Transacción local completada; espera envío. Pasa a SINCRONIZANDO con sesión/contexto válidos. |
| SINCRONIZANDO | Solicitud en curso. Confirmación -> SINCRONIZADO; fallo temporal -> ERROR_REINTENTABLE; autenticación -> REQUIERE_SESION; rechazo -> RECHAZADO; conflicto -> CONFLICTO. |
| ERROR_REINTENTABLE | Resultado remoto desconocido o servidor temporalmente no disponible; conserva el mismo cuerpo e IDs. Reintento -> SINCRONIZANDO. |
| REQUIERE_SESION | Pausa hasta autenticación online de la misma cuenta y verificación de membresía/rol. No mueve la cola a otra cuenta. |
| RECHAZADO | Respuesta definitiva por validación o permisos; sin reintentos automáticos. Muestra motivo seguro y acciones permitidas. |
| CONFLICTO | Colisión de ID, versión incompatible o operationId con otro contenido; no sobrescribe ni fusiona automáticamente. Requiere revisión. |
| SINCRONIZADO | Servidor confirmó el efecto o recuperó su confirmación previa. No vuelve a enviar automáticamente. |

| Situación | Comportamiento esperado |
|---|---|
| Datos inválidos en cliente | Señalar campos; no crear la operación. Validación equivalente en backend. |
| UUID o IndexedDB no disponibles, cuota o transacción local fallida | Informar «No se pudo guardar» y mantener formulario si es posible; no mostrar pendiente ni enviar. |
| Petición enviada, respuesta perdida o cierre del navegador | Recuperar como ERROR_REINTENTABLE/PENDIENTE y reenviar mismos IDs y contenido; no generar otra alta. |
| HTTP 401 | REQUIERE_SESION. Token vencido no elimina los registros locales ni equivale a permiso remoto. |
| HTTP 403 con código MEMBRESIA_REVOCADA o ROL_INSUFICIENTE | RECHAZADO, detener cola de esa ganadería, retirar copias sincronizadas y bloquear pendientes hasta recuperar autorización o descarte explícito. |
| HTTP 403 CSRF_INVALIDO | No indica revocación de membresía: conservar cola y copias, obtener nuevo CSRF y reenviar una sola vez con mismos IDs/cuerpo. Si persiste, pausar con aviso; nunca borrar datos ni repetir login/registro automáticamente. |
| HTTP 404 | Recurso no accesible o inexistente, sin distinguir información ajena; no reintentar automáticamente. |
| HTTP 409 | CONFLICTO; no cambiar IDs ni sobrescribir por iniciativa del sincronizador. |
| HTTP 400/413/422 definitivos, sin alta | RECHAZADO. Permite corregir tras demostrar por respuesta definitiva que esa operación no produjo alta; nueva operationId y mismo animalId, dejando la anterior como sustituida localmente. |
| HTTP 429, 5xx o error de red | ERROR_REINTENTABLE; respetar Retry-After cuando exista. |
| Respuesta mal formada, IDs distintos o versión inválida | No confirmar localmente; error reintentable del mismo sobre y aviso de fallo de protocolo. |

Reintentos definidos por ejecución: primer intento inmediato y hasta cuatro
más, con esperas de 2, 5, 15 y 30 segundos más variación aleatoria de 0–1 s.
Tras cinco fallos se pausa sin borrar la operación; «Sincronizar ahora» inicia
otra ejecución. No reintentar continuamente sin red ni con sesión inválida.

Una operación confirmada o enviada tiene cuerpo inmutable. No se permite
editar o «cancelar» silenciosamente una solicitud con resultado desconocido.
Para conflictos de identidad se exige reconciliar con el servidor por un usuario
autorizado; nunca regenerar UUID y reenviar automáticamente. Un alta rechazada
por permisos solo puede reintentarse tras comprobar autorización actual y
mediante acción explícita; el cuerpo e IDs permanecen iguales.

## 8. Reglas de negocio y privacidad

| ID | Regla |
|---|---|
| RN01 | Animal pertenece a una única ganadería existente; ganaderiaId es inmutable en esta SPEC. creadoPor proviene de la identidad autenticada, no del cliente. |
| RN02 | animalId UUIDv4 generado localmente identifica la misma ficha online/offline; nombre y arete no son claves primarias. operationId UUIDv4 identifica una intención de alta, se genera una vez al confirmar y se conserva en reintentos. |
| RN03 | PostgreSQL aplica unicidad global de animalId y operationId. El efecto y su recibo de éxito se confirman en una transacción. Un fallo revierte ambos. |
| RN04 | Antes de consultar un recibo previo se revalidan permisos. Solo misma cuenta, ganadería y contenido canónico obtienen el recibo de la misma operación. Colisiones no revelan datos de otro ámbito. |
| RN05 | Alta usa tipo CREAR_ANIMAL y baseVersion=0 (espera inexistencia); éxito produce version=1 y estado ACTIVO. Una versión base distinta no es una edición: se rechaza como conflicto. |
| RN06 | Repetición idéntica de operationId devuelve el mismo efecto confirmado, sin una nueva alta; mismo ID con distinto contenido produce conflicto. Otro operationId para un animalId existente también produce conflicto, aunque los datos coincidan. |
| RN07 | El sexo es obligatorio: MACHO o HEMBRA, sin valor predeterminado. Nombre, arete, identificación textil, raza, edad, peso y fotografía son opcionales; su ausencia no bloquea el alta. Categoría puede quedar sin clasificar. |
| RN08 | Las seis categorías son declaradas; no se infieren por edad, parto ni rol reproductivo. Toda categoría informada debe ser compatible con el sexo obligatorio. Categoría vacía no crea una séptima categoría de negocio. |
| RN09 | Arete frontal y reverso admiten texto alfanumérico y marcas familiares. La identificación textil opcional registra colores de lana/cinta y ubicación IZQUIERDA, DERECHA o AMBAS; ambos datos se informan juntos o ambos quedan nulos. Arete y textil son independientes: se permite cualquiera, ambos o ninguno. Una coincidencia de arete frontal normalizado en la misma ganadería avisa de posible duplicado, no bloquea ni fusiona animales. |
| RN10 | Los reintentos evitan duplicados técnicos, no detectan de forma infalible que dos personas registraron el mismo vacuno físico con IDs diferentes. La interfaz explica esta limitación. |
| RN11 | Pendientes locales se particionan por cuenta y ganadería; solo esa cuenta puede enviarlos con permisos actuales. No se guardan historiales completos ni credenciales en el almacenamiento offline de esta SPEC. |
| RN12 | Redis, IA, notificaciones y fotografías no participan en el alta ni en la transacción; su indisponibilidad no bloquea estos flujos. |

### Preparación y datos locales (política DP06 adoptada)

Offline es voluntario y solo para dispositivo de confianza. Se descargan las
páginas/fichas básicas que el usuario haya solicitado, no todo el ganado de forma
implícita. Se indica qué información está disponible sin red y la fecha de copia;
la consulta local no se presenta como lista completa ni como estado actual remoto.
Se separan instantáneas confirmadas y registros pendientes propios por animalId.

La preparación recuerda cuenta/contexto, roles comprobados y última validación
online, sin contraseñas ni tokens en IndexedDB/localStorage. Se distingue:

- **Autorización temporal offline:** máximo adoptado de 7 días (168 horas)
  desde la última autenticación y comprobación online exitosa de cuenta,
  ganadería y rol. Al vencer se bloquean consulta privada local, nuevas altas
  y envío hasta revalidar. Volver a tener red no renueva por sí solo el plazo.
- **Conservación local:** el plazo NO es un TTL de los registros. No se borran
  pendientes, sobres, IDs ni resultados desconocidos por vencimiento, expiración
  del token o pérdida de sesión. Quedan conservados por cuenta/ganadería hasta
  confirmación remota o descarte explícito informado. El vencimiento tampoco
  ejecuta limpieza automática de las copias descargadas.

La autorización local se controla aparte del estado de sincronización: un
PENDIENTE no desaparece ni se marca SINCRONIZADO por expirar su permiso.
La interfaz muestra «Revalidar acceso para continuar», sin revelar datos bloqueados.
Si caduca solo el token mientras no hay red, puede trabajar dentro de la ventana
con contexto preparado, sin enviar. Si pierde el contexto o cerró sesión
explícitamente, no reconstruye el permiso desde los registros: exige login online
de la misma cuenta. En ambos casos conserva la cola.

Al recuperar conexión se autentica/revalida cuenta, ganadería y rol antes de
reanudar. La revalidación exitosa renueva la ventana y recupera pendientes con
los mismos IDs; 401 mantiene la pausa y 403 bloquea contexto sin borrar pendientes.
El reloj local no es una frontera de seguridad fiable: el servidor siempre
revalida permisos. Reabrir offline con contexto vigente no autentica de nuevo.
Quien controle el perfil del navegador podría acceder a copias locales; no se
promete aislamiento frente a otra persona que use ese perfil.

La revocación remota no puede retirar inmediatamente datos de un dispositivo
sin red. Al detectarla, se elimina la copia sincronizada de esa ganadería y
se bloquea la cola pendiente: sin vista ni exportación de su contenido hasta
recuperar autorización; se permite descartarla con confirmación explícita.
Cerrar sesión/cambiar cuenta invalida el contexto y limpia copias sincronizadas
visibles, pero conserva pendientes bloqueados por defecto. Otra cuenta no los
ve ni envía. Solo la cuenta de origen autenticada y con permisos actuales puede
recuperarlos. El usuario puede sincronizar antes de salir o descartarlos mediante
confirmación separada con aviso de pérdida. Cerrar sesión o expirar no implica
consentir su borrado. Si el resultado remoto era desconocido, descartar localmente
no revierte un alta que pudiera haberse confirmado.
Una limpieza fallida de copias sincronizadas impide activar offline para otra
cuenta hasta resolverla; la cola retenida intencionalmente no es un fallo de
limpieza. El bloqueo en la aplicación no equivale a cifrado frente al dueño del
perfil. No hay exportación automática ni respaldo privado en esta entrega.
El borrado del perfil, limpieza del navegador, desinstalación o pérdida del
dispositivo pueden causar pérdida externa que la aplicación no garantiza evitar.

## 9. Datos de entrada y salida

Datos técnicos los genera la aplicación, no el productor. Se rechazan campos
no admitidos en alta de animales (actor, estado, propietario, versión resultante o fotografías). Los cuerpos de autenticación son exclusivos de AU01/AU02 y nunca se guardan en la cola.
Cadenas: normalización Unicode NFC y recorte exterior; vacío equivale a null solo en campos opcionales. Sexo vacío/null siempre es inválido.
Límites en caracteres Unicode; cliente y servidor comparten casos de prueba.
Se rechazan controles invisibles, salvo salto de línea en observaciones.

| Campo | Tipo | Obligatorio | Validación |
|---|---|---|---|
| ganaderiaId | UUID en ruta/contexto | Sí, técnico | Ganadería accesible y activa; no se repite en cuerpo. |
| animalId | UUIDv4 | Sí, técnico | Formato/versión válidos; no depende del reloj ni del arete. |
| operationId | UUIDv4 | Sí, técnico | Único por intención; nunca se regenera en reintento. |
| schemaVersion | Entero | Sí, técnico | 1 conserva el contrato histórico sin textil; 2 incorpora identificación textil. Otra versión -> 422 ESQUEMA_NO_SOPORTADO. |
| tipo | Enumeración | Sí, técnico | CREAR_ANIMAL. |
| baseVersion | Entero | Sí, técnico | 0; otro entero -> 409 VERSION_INCOMPATIBLE. |
| sexo | Enumeración | Sí | MACHO o HEMBRA. Ausencia, null, vacío u otro valor se rechazan en cliente y servidor; sin valor predeterminado ni inferencia. |
| nombre | Texto o null | No | Máximo 100 caracteres; admite espacios y acentos. |
| areteFrente | Texto o null | No | Máximo 100; no solo numérico, sin unicidad obligatoria. |
| areteReverso | Texto o null | No | Máximo 100; marca de propiedad distinta del nombre del animal. |
| coloresIdentificacionTextil | Texto o null | No | Máximo 100 caracteres Unicode, NFC/trim, sin catálogo nuevo. Se informa junto con ubicación o ambos quedan null. |
| ubicacionIdentificacionTextil | Enumeración o null | No | IZQUIERDA, DERECHA o AMBAS; se informa junto con colores o ambos quedan null. |
| raza | Texto o null | No | Máximo 100; declarada, sin catálogo de razas nuevo. |
| fechaNacimiento | Fecha ISO o null | No | Fecha real, no futura respecto a fecha del servidor en America/Lima; no se impone edad máxima. |
| nacimientoEstimado | Booleano | Sí si hay fecha | Distingue fecha aproximada; sin fecha se omite o es null. |
| categoria | Enumeración o null | No | TERNERA, VAQUILLA, VACA, TERNERO, TORETE, TORO; compatible con el sexo obligatorio. |
| observaciones | Texto o null | No | Máximo 2000; texto plano, nunca HTML ejecutable. |

Compatibilidad: TERNERA/VAQUILLA/VACA con HEMBRA; TERNERO/TORETE/TORO con MACHO.
Sin sexo válido no se guarda localmente ni se confirma alta remota; la categoría no sustituye la selección de sexo.
Cliente valida fecha con su reloj como ayuda; el servidor decide al sincronizar usando America/Lima. Los instantes creadoEn, confirmadoEn, expiraciones y auditoría se almacenan en UTC y se devuelven con zona explícita; fechaNacimiento es fecha de negocio sin hora, no un instante.
El alta mínima contiene sexo y los datos técnicos generados por la aplicación. La edad es opcional y se deriva solo si existe fecha de nacimiento. Peso y foto no son requisitos; su captura queda fuera de este incremento según sección 4, sin añadir campos productivos ni almacenamiento de imágenes. El UUID interno continúa siendo la única identidad técnica aunque no exista arete ni identificación textil.

Salida de alta: operationId, animalId, resultado APLICADO o REPROCESADO,
versionCreada=1, confirmadoEn del servidor y advertencias no bloqueantes.
El recibo conserva la confirmación original; no sustituye la versión vigente de
la ficha si esta fue modificada por otro flujo en el futuro. Listado/ficha devuelven
solo campos básicos autorizados, ganaderiaId, estado, version y fechas del servidor.
Los errores usan código estable y campos inválidos, sin SQL, datos ajenos ni secretos.

## 10. Persistencia e integraciones

PostgreSQL es la fuente central; guarda Animal y recibos de operaciones exitosas
con restricciones y transacción descritas en el plan. No se eliminan esos recibos
en este incremento; cualquier limpieza futura exige conservar la garantía de
idempotencia. Un reintento concurrente espera/reconsulta el resultado confirmado,
no aplica una segunda escritura. Autorización y cambios de membresía deben
ordenarse transaccionalmente para no aceptar una escritura posterior a una
revocación confirmada. El detalle técnico está en el plan.

IndexedDB confirma juntos provisional y cola, recupera envíos interrumpidos y
marca confirmaciones en una transacción local. Un fallo de almacenamiento de
la confirmación provoca reintento seguro, no otra identidad. Service Worker
conserva recursos estáticos; las respuestas privadas no entran en Cache Storage.
No se almacenan imágenes, secretos ni datos de IA en esta copia.

## 11. Criterios de aceptación aprobados documentalmente

Los criterios están aprobados documentalmente y pendientes de implementación y ejecución. Dado/Cuando/Entonces define
el resultado verificable, no una prueba ya aprobada. Se conservan CA01–CA08
con precisiones: CA01 exige sexo por la decisión confirmada y CA08 no infiere categorías. Se retira la propuesta anterior de sexo opcional; se concreta esta validación en SPEC sin reescribir RF/ADR aprobados.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA01 | Propietario/editor autenticado, miembro vigente y ganadería activa | Confirma alta con solo sexo MACHO o HEMBRA y datos técnicos generados | Una ficha ACTIVO, version=1, ganadería correcta y animalId estable; no exige nombre, arete, textil, raza, edad, peso ni foto. Sin sexo válido no hay guardado local ni alta remota. |
| CA02 | Sexo obligatorio y datos opcionales con letras, números, acentos y ausencias | Registra solo arete, solo textil, ambos o ninguno; también envía sexo/textos/ubicación inválidos | Las cuatro combinaciones conservan el mismo UUID interno. Textos normalizados se conservan; textil parcial, ubicación desconocida, límites y fechas inválidos se rechazan con 422 sin escritura. |
| CA03 | Contrato AU01–AU08 en implementación incremental y cuentas de dos ganaderías | Se prueban registro/login, creación inicial y cada rol con sesión real | Registro no concede roles; ganadería/propietario se crean juntos. Propietario/editor crean y leen; lector solo lee; anónimo -> 401, ajeno/rol insuficiente -> 403/404 seguros. Pruebas sintéticas de editor/lector no equivalen a alta familiar real; AU01–AU08 y sus comprobaciones deben pasar antes de habilitar animales reales. |
| CA04 | Dispositivo preparado offline con permiso local vigente | Confirma alta, reinicia aplicación y reconecta | Pendiente persiste; tras autorización actual se confirma una sola ficha con los mismos IDs. |
| CA05 | Alta confirmada cuya respuesta se perdió | Se reenvía concurrentemente el mismo sobre, incluso desde dos pestañas | Un Animal y un recibo de éxito; misma confirmación y ningún efecto adicional. |
| CA06 | Editor con alta local pendiente | Su membresía se revoca o pasa a lector antes del envío | El backend rechaza; no escribe; el cliente bloquea cola/retira copia según política, sin acceso ajeno. |
| CA07 | Guardado local o solicitud con respuesta desconocida | Se muestra el estado | Nunca se afirma «Sincronizado» sin confirmación válida; error de cuota no se muestra como guardado. |
| CA08 | Sexo MACHO/HEMBRA seleccionado y fecha/edad/categoría opcionales | Se valida el alta | No se infiere sexo ni categoría por edad; categoría incompatible se rechaza y categoría null se muestra sin clasificar. La categoría nunca permite omitir sexo. |
| CA09 | Mismo operationId y contenido diferente, o animalId ya existente con otra operación | Se sincroniza | 409 genérico, sin sobrescritura ni datos ajenos; no se regenera identidad automáticamente. |
| CA10 | Alta con baseVersion distinta de 0 o esquema no soportado | Llega a API | 409 de versión o 422 de esquema, respectivamente; sin modificar Animal ni recibo de éxito. |
| CA11 | Fallo de PostgreSQL entre alta y recibo, o fallo local al guardar confirmación | Se reintenta | No queda alta parcial sin recibo; una confirmación remota recuperable no origina duplicado. |
| CA12 | JWT expirado, sesión vencida/revocada o renovación usada/incierta, con pendientes | Se renueva, reautentica o recupera red | Rotación de un solo uso emite nuevo JWT y cookie sin ampliar 24 h; replay de refresh consumido revoca familia y rechaza JWT previos por estado de sesión. Respuesta de refresh incierta exige login sin retry automático. Cola e IDs se conservan; revalidar identidad/ganadería/rol antes de sincronizar. No hay invalidez automática del JWT. |
| CA13 | Mismo arete frontal en una ganadería o misma descripción física/textil con IDs distintos | Se registra otra alta intencionada | Solo el arete frontal conserva la advertencia conocida; textil no es clave ni fusiona/bloquea. No revela coincidencias de otra ganadería ni promete deduplicación física. |
| CA14 | Contexto validado hace 168 h y pendientes/resultados desconocidos | Vence ventana, cierra sesión/cambia cuenta o revalida después | No borra/modifica sobres/IDs por plazo o logout. Bloquea acceso/nuevas altas al vencer; logout limpia contexto/copias sincronizadas y conserva pendientes bloqueados. Cierre offline no promete revocación remota; al volver la red se procesa antes de reactivar acceso. Otra cuenta no ve/envía pendientes; solo origen autenticado y con permisos los recupera o descarta explícitamente. |
| CA15 | Lista paginada online y algunas páginas disponibles offline | Consulta «Mi ganado» y ficha en móvil/escritorio | Respuestas autorizadas, orden estable, sin duplicar provisional/confirmado; copia incompleta etiquetada con fecha y cobertura. |
| CA16 | Navegador sin preparación, sin UUID seguro o IndexedDB fallido | Intenta registrar offline | Informa la limitación sin afirmar guardado ni generar IDs inseguros; una respuesta mal formada tampoco confirma. |
| CA17 | Redis, IA y notificaciones no disponibles | Registra y consulta animales | Flujos privados siguen funcionando sin consultar esas integraciones. |
| CA18 | Entorno y datos definidos en plan | Se ejecuta carga de consultas con 100 concurrentes | p95 ≤ 2 s, errores funcionales 0 en escenario sin fallos; exclusiones de IA/fotos y resultados reales documentados. |

## 12. Pruebas necesarias y trazabilidad

Herramientas previstas conforme a DP07 adoptada: JUnit 5 y Spring Boot Test ya
se fijaron y ejecutaron para la base y el esquema de Identidad; esto no ejecuta
las pruebas funcionales enumeradas debajo. Siguen pendientes los dobles para
núcleo, la decisión de Testcontainers o PostgreSQL controlado por entorno para
cada suite, y Playwright/k6. Vitest está fijado para la base del cliente. Las
versiones restantes se fijarán antes de su implementación. Se probarán el
navegador real y su IndexedDB además de dobles.

| ID | Tipo y escenario | Criterios |
|---|---|---|
| PU01 | Validadores: sexo omitido/null/vacío/inválido, MACHO/HEMBRA válidos, opcionales ausentes, arete/textil en cuatro combinaciones, par textil completo, ubicaciones, límites 100/101 y 2000/2001, Unicode, fechas y categorías. | CA01, CA02, CA08 |
| PU02 | Autorización por cuenta/ganadería/rol/estado; caso de uso con repositorios simulados. | CA03, CA06 |
| PU03 | Generación UUID inyectable, sobre inmutable, estados, backoff y parser de confirmaciones. | CA04, CA07, CA10, CA12, CA16 |
| PU04 | Normalización y huella de operación, IDs distintos/contenido cambiado, deduplicación de vistas. | CA05, CA09, CA13, CA15 |
| PI01 | Contrato real AU01–AU08: registro, login, sesión, CSRF, propietario inicial atómico, roles por ganadería, revocación y acceso cruzado; fixtures familiares solo test. | CA03, CA06, CA12, CA14 |
| PI02 | Concurrencia, unicidad, respuesta perdida, rollback entre Animal/recibo, revocación concurrente ordenada. | CA05, CA06, CA09, CA11 |
| PI03 | Sexo obligatorio en API: ausente/null/vacío/inválido -> 422 sin alta; esquemas v1/v2, textil, versiones/campos no admitidos, listado estable, aislamiento, compatibilidad de registros anteriores y advertencia arete. | CA01, CA02, CA08, CA10, CA13, CA15 |
| PI04 | Cliente/IndexedDB real: sexo inválido offline no crea provisional/cola; commit/abort, cuota simulada, recuperación tras recarga y fallo al confirmar localmente. | CA01, CA02, CA04, CA07, CA11, CA16 |
| PI05 | Arranque y consultas sin conexión con Redis/IA/notificaciones; sin dependencias en flujo privado. | CA17 |
| EE01 | Registro/login reales -> crear ganadería/propietario -> alta con MACHO/HEMBRA -> listado/ficha. Editor/lector por fixtures aislados, misma autenticación, sin UI de invitaciones; 360 px y escritorio. | CA01, CA02, CA03, CA08, CA15 |
| EE02 | Preparar, desconectar, guardar, cerrar/reabrir, reconectar; doble clic/pestaña, caída tras commit y reenvío único. | CA04, CA05, CA07, CA11 |
| EE03 | JWT de 10 min, renovación y sesión de 24 h; logout online/offline, CSRF, revocación/cambio a lector, 168 h sin borrar cola y recuperación por misma cuenta. | CA03, CA06, CA12, CA14 |
| EE04 | Cuota, incompatibilidad, colisión inyectada, versiones y errores permanentes/temporales/protocolo. | CA09, CA10, CA13, CA16 |
| PC01 | Carga reproducible de listado/ficha autorizados según plan, sin caché privada. | CA18 |

Las colisiones se inyectan con generador controlado; generar muchos UUID sin
colisión no demuestra unicidad absoluta. Pruebas de carga se ejecutarán al
existir versión ejecutable y ante cambios de rendimiento, no en esta tarea.
Venta, invalidación de anuncios, genealogía, IA y fotografías no se prueban como
implementadas aquí: pertenecen a sus SPEC. Las pruebas Redis de catálogo siguen
obligatorias para el producto; no aplica caché vacía/poblada a consultas privadas.

### Comprobaciones verificables del contrato de acceso

No añaden criterios CA: precisan CA03, CA06, CA12 y CA14; AU01–AU08 son reglas
de contrato, no resultados. PA01 tiene evidencia parcial del contrato global;
PA02/PA03 tienen ejecución backend y cliente automatizado; PA04 y PA07 tienen ejecución parcial online;
PA05 tiene ejecución backend/cliente; PA06/PA08 tienen ejecución parcial y
permanecen no ejecutadas integralmente.

| Prueba | Escenario y resultado requerido | Criterios |
|---|---|---|
| PA01 | **EJECUTADA para AU01:** React obtiene CSRF y registra sin persistir credenciales; registro válido crea cuenta sin permisos; correo normalizado duplicado no crea segunda cuenta; inválidos y campos rol/ganadería/estado son rechazados; hash nunca se devuelve. | CA03; [evidencia](../../docs/05-preparacion-implementacion/08-registro-usuarios.md) |
| PA02 | **EJECUTADA EN BACKEND Y CLIENTE AUTOMATIZADO:** contraseña válida/inválida, cuenta inexistente/inactiva y límite persistente; HTTP 401/429, JWT alterado/expirado/emisor-audiencia incorrectos y sesión revocada se rechazan mediante filtro. React cubre éxito, error genérico, límite/red y evita doble envío, sin E2E HTTPS real. | CA03, CA12; [backend](../../docs/05-preparacion-implementacion/09-nucleo-autenticacion-sesiones.md), [React](../../docs/05-preparacion-implementacion/10-login-react.md) |
| PA03 | **EJECUTADA EN BACKEND Y CLIENTE AUTOMATIZADO:** 10 min/24 h; cookie, rotación, dos refresh concurrentes, reutilización y respuesta perdida segura verificados. React restaura, renueva en memoria y serializa en pestaña/Web Locks; falta validar varias pestañas en navegador HTTPS real. | CA12; [backend](../../docs/05-preparacion-implementacion/09-nucleo-autenticacion-sesiones.md), [React](../../docs/05-preparacion-implementacion/10-login-react.md) |
| PA04 | **EJECUCIÓN PARCIAL ONLINE:** logout idempotente revoca sesión/verificadores y rechaza el JWT anterior. AU07 revoca membresía y el JWT del miembro pierde acceso en la siguiente operación. React bloquea localmente y conserva solo una marca no secreta cuando falla el cierre remoto. Faltan cola offline y validación real entre pestañas/navegador. | CA06, CA12, CA14; [backend](../../docs/05-preparacion-implementacion/09-nucleo-autenticacion-sesiones.md), [React](../../docs/05-preparacion-implementacion/10-login-react.md), [AU07](../../docs/05-preparacion-implementacion/14-revocacion-membresias.md) |
| PA05 | **EJECUTADA EN BACKEND Y CLIENTE AUTOMATIZADO:** crear ganadería confirma propietario y membresía juntos; fallo de membresía revierte la ganadería; repetición mismo ID/actor/nombre no duplica y campos de propietario/rol se rechazan. React genera UUIDv4, usa JWT/CSRF y no persiste la selección. | CA03; [backend](../../docs/05-preparacion-implementacion/11-creacion-ganaderia.md), [React/listado](../../docs/05-preparacion-implementacion/12-ganaderias-react-listado.md) |
| PA06 | **EJECUCIÓN PARCIAL:** listado de ganaderías, `/acceso` y T09 consultan PostgreSQL; T08 revalida sesión, ganadería y membresía dentro de la transacción. Propietario/editor registran y los tres roles leen por HTTP; ajeno/revocado es rechazado sin filtrar datos y ambos órdenes de carrera con AU07 siguen cubiertos en el núcleo. Falta el flujo offline. | CA03, CA06; [AU06](../../docs/05-preparacion-implementacion/13-acceso-ganaderia.md), [AU07](../../docs/05-preparacion-implementacion/14-revocacion-membresias.md), [T04–T07](../../docs/05-preparacion-implementacion/15-nucleo-registro-animales.md), [T08](../../docs/05-preparacion-implementacion/16-endpoint-registro-animales.md), [T09](../../docs/05-preparacion-implementacion/17-consulta-animales.md) |
| PA07 | **EJECUCIÓN BACKEND Y CLIENTE AUTOMATIZADO:** cookie HttpOnly/Secure/SameSite, `no-store`, CSRF inválido y origen ajeno probados en backend; AU07 exige CSRF/origen; React obtiene/envía CSRF sin leer refresh ni persistir JWT y permite configurar HTTPS con certificados externos. Falta navegador HTTPS real para AU07. | CA03, CA12, CA14; [backend](../../docs/05-preparacion-implementacion/09-nucleo-autenticacion-sesiones.md), [React](../../docs/05-preparacion-implementacion/10-login-react.md), [AU07](../../docs/05-preparacion-implementacion/14-revocacion-membresias.md) |
| PA08 | **EJECUCIÓN PARCIAL:** núcleo de Identidad cubre contraseñas, expiración, permisos, creación y revocación; integración de animales usa propietario real y roles editor/lector solo como fixtures, sin vía productiva de alta familiar. React T10 consume `/acceso` y presenta permisos online. Faltan E2E real y offline. | CA03, CA06, CA12, CA14; [AU07](../../docs/05-preparacion-implementacion/14-revocacion-membresias.md), [T09](../../docs/05-preparacion-implementacion/17-consulta-animales.md), [T10](../../docs/05-preparacion-implementacion/18-animales-react.md) |

### Relación con los 16 criterios de evaluación

| Criterio | Cobertura de SPEC-01 y evidencia futura |
|---|---|
| 1–2 | Objetivo, actores, RF/HU, validaciones, flujos y modelo propuesto. |
| 3 | Módulos/contratos/transacciones del plan, compatibles con C4 vigente; PNG continúan pendientes. |
| 4 | spec/plan/tasks revisados antes del código; no equivale a implementación. |
| 5–6 | PU/PI/EE con resultados por CA. |
| 7 | «Mi ganado», estados claros, teclado y responsividad. |
| 8–9 | Paginación, restricciones e índices; PC01 y planes de consultas, sin proclamar escalabilidad demostrada. |
| 10 | HTTPS es precondición; dominio/DNS/CDN fuera de este incremento. |
| 11 | Redis del catálogo fuera de alcance; sin caché privada nueva. |
| 12 | Ejecución futura de comprobaciones en CI, sin pipeline creado aquí. |
| 13 | PC01 con k6 propuesto, entorno y resultados. |
| 14 | Migraciones y comandos reproducibles futuros, sin secretos. |
| 15–16 | Evidencias y guion de demostración futuros, no informe/defensa terminados. |

Referencia: [matriz global](../../docs/06-evaluacion-sdd/matriz-trazabilidad.md).

## 13. Estado de las decisiones y aprobación formal

| ID | Estado / decisión | Condición restante |
|---|---|---|
| DP01 | Adoptada y aprobada documentalmente: AU01–AU08, registro/login, JWT 10 min, sesión máxima 24 h, rotación de renovación, verificadores seguros, cookies/CSRF y membresía validada por petición. | Implementar y verificar T02.1–T02.8/PA01–PA08 antes de animales reales. Sin invitaciones reales en este incremento. |
| DP02 | Adoptada: UUIDv4 local, IDs estables, idempotencia y restricciones únicas; recibos sin limpieza automática en este incremento. | Verificar implementación y pruebas antes de afirmar cumplimiento. |
| DP03 | Adoptada: sexo obligatorio MACHO/HEMBRA; datos descriptivos restantes opcionales y límites documentados. | Peso/foto no se capturan en este incremento. |
| DP04 | Adoptada: alta individual, listado, ficha y offline; DP01 se ejecuta como prerrequisito. | Masivo, invitaciones y altas reales de editor/lector mediante otras SPEC. |
| DP05 | Adoptada: conflictos conservadores sin sobrescritura ni descarte silencioso; revisar/reintentar solo según reglas documentadas. | Probar respuesta perdida, concurrencia y conflictos; no prometer fusión automática. |
| DP06 | Adoptada: máximo 168 h en dispositivo confiable, sin eliminación automática de pendientes. | Documentadas limitaciones de revocación sin red y acceso al perfil; no equiparar autorización con TTL de datos. |
| DP07 | Adoptada: unitarias, integración, E2E, offline y carga con herramientas del plan. | Fijar versiones/compatibilidad cuando se prepare el proyecto ejecutable (T03), no cambiar herramientas silenciosamente. |
| DP08 | Adoptada: zona de negocio America/Lima, instantes UTC y fecha/paginación documentadas. | Validar límites de fecha y cursor en pruebas, sin convertir fechaNacimiento en instante. |

DP01–DP08 quedan adoptadas y SPEC-01 aprobada documentalmente por autorización expresa del responsable el 16 de noviembre de 2026. Las precisiones de rotación sustituyen la propuesta anterior de refresh estable; no se reabren las demás decisiones. Plan y tareas forman parte de esta aprobación. Identidad, ganaderías y T04–T10 existen incrementalmente; T11 inició la persistencia local, pero su integración, autorización offline y los criterios conjuntos continúan pendientes.

## 14. Evidencia de validación

| Validación | Estado | Evidencia |
|---|---|---|
| Coherencia documental, enlaces y códigos | Comprobada el 16 de noviembre de 2026 | 18 CA con pruebas; 23 tareas superiores y ocho subtareas de acceso; AU01–AU08 y PA01–PA08 definidos; 14 enlaces locales de los tres documentos válidos, tablas y códigos coherentes, git diff --check sin errores. No equivale a pruebas funcionales. |
| PA01 / AU01 | Ejecutada el 17 de noviembre de 2026 | Formulario React, CSRF/cookie segura, revisión manual en Chrome, HTTP real, dominio, caso de uso, Argon2id y PostgreSQL; CA03 no está completo porque AU02–AU08 y el E2E automatizado de navegador real siguen pendientes. |
| PA02–PA03 / AU02–AU03 | Backend y cliente automatizado ejecutados el 17 de noviembre de 2026 | Backend: credenciales, límite, sesiones, JWT RS256, cookie, filtro, rotación, concurrencia y respuesta perdida. React: login, JWT en memoria, restauración, renovación controlada y errores; falta navegador HTTPS real. [Backend](../../docs/05-preparacion-implementacion/09-nucleo-autenticacion-sesiones.md), [React](../../docs/05-preparacion-implementacion/10-login-react.md). |
| PA04/PA07 / AU04/AU08 | Parcial backend/cliente el 17 de noviembre de 2026 | Logout/JWT previo, atributos de cookie, CSRF, origen y no-store probados; cliente bloquea localmente ante cierre remoto fallido. Faltan cola offline, membresías y navegador HTTPS real. |
| PA05 / AU05 y PA06 / AU06 parcial | Backend/cliente automatizado el 17 de noviembre de 2026 | Creación/listado/selección verificados; `/acceso` revalida ganadería y membresía activas en PostgreSQL, devuelve rol vigente y rechaza sin distinguir ajena, revocada, inactiva o inexistente. Regresión backend: 73 pruebas. [Listado/React](../../docs/05-preparacion-implementacion/12-ganaderias-react-listado.md), [contexto](../../docs/05-preparacion-implementacion/13-acceso-ganaderia.md). |
| AU07 / PA06–PA08 parcial | Backend/PostgreSQL el 17 de noviembre de 2026 | Propietario revoca editor/lector sin borrar la fila; propietario protegido, idempotencia, aislamiento, CSRF/origen y pérdida inmediata de acceso verificados. Regresión backend: 82 pruebas. [Evidencia](../../docs/05-preparacion-implementacion/14-revocacion-membresias.md). |
| T04–T07 / PU01–PU04 y PI02 parciales | Backend/PostgreSQL el 18 de noviembre de 2026 | V4, dominio, canonicalización, caso de uso y persistencia transaccional verificados. Regresión backend: 104 pruebas; 0 fallos/errores/omitidas. No existe T08 HTTP. [Evidencia](../../docs/05-preparacion-implementacion/15-nucleo-registro-animales.md). |
| T08 / PI01 y PI03 parciales | Backend/PostgreSQL el 18 de noviembre de 2026 | POST autenticado verificado con recibos 201/200, UUIDv4, sexo/campos, roles, revocación, aislamiento, idempotencia, conflictos, CSRF, origen y límite de 16 KiB. Regresión backend: 113 pruebas; 0 fallos/errores/omitidas. [Evidencia](../../docs/05-preparacion-implementacion/16-endpoint-registro-animales.md). |
| T09 / PI03 y CA15 parciales | Backend/PostgreSQL el 18 de noviembre de 2026 | Listado/ficha autenticados, tres roles activos, aislamiento, 404 seguro, vacío, cursor y orden estable verificados; V5 corrige el índice sin editar V1–V4. Regresión backend: 124 pruebas; 0 fallos/errores/omitidas. [Evidencia](../../docs/05-preparacion-implementacion/17-consulta-animales.md). |
| T10 / EE01 y CA01–CA03/CA08/CA15 parciales | Frontend automatizado el 18 de noviembre de 2026 | Formulario, listado, ficha, cursor, roles y replay en memoria verificados: regresión frontend 51 pruebas; TypeScript/build correctos. Sin navegador real ni offline. [Evidencia](../../docs/05-preparacion-implementacion/18-animales-react.md). |
| T11 / PU03 y PI04 parciales | Frontend automatizado el 19 de noviembre de 2026 | Adaptador IndexedDB nativo con guardado, reapertura simulada, aislamiento por cuenta/ganadería, estados, conservación e inyección de fallos: 7 pruebas dirigidas; regresión frontend 60 pruebas, TypeScript/build correctos. No está conectado a la UI y falta navegador real. [Evidencia](../../docs/05-preparacion-implementacion/20-indexeddb-operaciones-pendientes.md). |
| T12/T15 / PU02, PI04 y EE03 parciales | Frontend automatizado el 19 de noviembre de 2026 | Contexto local no secreto preparado desde `/acceso`, roles, 168 h, aislamiento, bloqueo por sesión/rechazo y conservación de cola verificados con dobles: 21 pruebas dirigidas; regresión frontend 74 pruebas, TypeScript/build correctos. No es credencial criptográfica ni está conectado a la UI. [Evidencia](../../docs/05-preparacion-implementacion/21-autorizacion-offline-local.md). |
| T10–T12/T15 / PI04, EE01 y EE03 parciales | Frontend automatizado el 19 de noviembre de 2026 | Opt-in en dispositivo confiable, alta online sin duplicar cola, guardado local solo ante fallo de red, roles/vencimiento/aislamiento, IDs estables, fallo IndexedDB, doble envío y separación visual verificados. Regresión frontend 87 pruebas; TypeScript/build correctos. Sin navegador real, Service Worker ni sincronización. [Evidencia](../../docs/05-preparacion-implementacion/22-registro-provisional-offline-react.md). |
| Corrección UX T10–T12 | Frontend automatizado el 19 de noviembre de 2026 | Confirmación persistente del dispositivo preparado, estado de red solo informativo y caída real de `fetch` clasificada como conexión. Regresión frontend 89 pruebas; TypeScript/build correctos. La prueba manual anterior confirmó un alta online, no un pendiente local. [Evidencia](../../docs/05-preparacion-implementacion/22-registro-provisional-offline-react.md). |
| PWA / PI04 y EE03 parciales | Frontend automatizado y observación manual el 19 de noviembre de 2026 | Manifest, iconos, precaché estático sin API, barrera sin sesión, instalación y F5 offline observados. Detección, confirmación, fallo, formularios/sincronización y coordinación Web Locks: regresión 134/134, TypeScript/build correctos. Cierre total y actualización A→B en Chrome siguen pendientes. [Evidencia](../../docs/05-preparacion-implementacion/24-pwa-carga-basica-offline.md). |
| PI03–PI05, EE01–EE04, PC01, PA06 y PA08 | No ejecutadas integralmente | El formulario integra cola y envío manual, y el app shell PWA está generado, pero faltan navegador real, caché privada autorizada, automatización, conflictos y E2E integral; no se declara SPEC-01 completa. |
| Aprobación de SPEC/plan/tareas | Aprobada documentalmente el 16 de noviembre de 2026 | Autorización expresa del responsable con precisiones obligatorias de sesión y rotación; sin implementación. |

## 15. Condición de finalización

Con SPEC-01 y DP01–DP08 aprobadas documentalmente, ejecutar
las tareas incrementales. La funcionalidad solo se completará con implementación,
pruebas aplicables aprobadas, resultados reales vinculados a CA, documentación
actualizada y revisión final. La redacción de estos documentos no cumple esa condición.
