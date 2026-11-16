# SPEC-01: Registro flexible de ganado con acceso familiar y funcionamiento sin conexión

## 1. Identificación

| Campo | Valor |
|---|---|
| Estado | **Pendiente de aprobación — No implementada** |
| Fecha | 15 de noviembre de 2026 |
| Requisitos | RF02 y RF17; RF21 para uso de acceso familiar existente; RF20 solo en su parte individual |
| Historias | HU02, HU18, HU21 (alta individual), HU22 (verificación de acceso, no gestión de miembros) |
| Calidad | AC01, AC02, AC04, AC05, AC06, AC08, AC09 |
| Drivers / ADR | DA01, DA02, DA06, DA07, DA09; ADR-01, ADR-02, ADR-03, ADR-04, ADR-07, ADR-12, ADR-13 |
| Escenarios | EC01, EC02, EC04, EC06, EC08, EC11, EC12 |
| Documentos | [Plan](plan.md), [tareas](tasks.md), [plantilla original](../plantilla-spec.md) |

**Base aprobada:** React PWA/IndexedDB, Spring Boot monolito modular/Clean
Architecture, PostgreSQL principal, identidad interna única, datos opcionales,
acceso familiar autorizado, permisos/versiones/duplicados al sincronizar.
**Decisión confirmada en esta revisión:** sexo obligatorio MACHO o HEMBRA; nombre, arete, raza, edad, peso y fotografía opcionales. **Propuesta para aprobar en esta SPEC:** las demás precisiones de campos,
permisos, UUID, API, estados, plazos y recuperación descritas a continuación.
No se modifican los RF ni ADR aprobados. Ningún criterio se declara cumplido.

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

## 3. Actores y permisos propuestos

Los roles se asignan a una cuenta dentro de cada ganadería, no globalmente.
El propietario se representa como una membresía vigente con rol PROPIETARIO.

| Acción dentro de esta SPEC | Propietario | Editor | Lector |
|---|---|---|---|
| Consultar lista y ficha básica de su ganadería | Sí | Sí | Sí |
| Crear animal online u offline preparado | Sí | Sí | No |
| Reenviar su propia alta pendiente | Sí | Sí | No |
| Consultar copias locales ya descargadas | Sí | Sí | Sí |
| Editar animal ya confirmado, eliminar o trasladar | Fuera de alcance | Fuera de alcance | Fuera de alcance |
| Invitar, revocar, cambiar roles, crear ganadería o publicar anuncios | Fuera de alcance | Fuera de alcance | Fuera de alcance |

Ningún rol obtiene acceso a ganaderías ajenas. El administrador de plataforma
no hereda estos permisos; requiere su propia membresía para estas operaciones.
Un familiar no reenvía la cola privada de otra cuenta. La ocultación de botones
no sustituye la autorización del backend. No se acepta actorId desde el cuerpo.

## 4. Alcance

Incluye alta individual, listado paginado, ficha básica, preparación explícita
del dispositivo, copia de las páginas/fichas descargadas, cola de altas y
sincronización manual o automática mientras la aplicación está abierta.
Se admite el mismo flujo de operación online y offline, con reintentos seguros.

No incluye edición de animales confirmados, importación masiva, gestión del ciclo
de vida de cuentas/membresías, autenticación completa nueva, nacimientos,
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

## 6. Flujo principal propuesto

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
| HTTP 403 por revocación o cambio a lector | RECHAZADO, detener cola de esa ganadería, retirar copias sincronizadas y bloquear pendientes hasta recuperar autorización o descarte explícito. |
| HTTP 404 | Recurso no accesible o inexistente, sin distinguir información ajena; no reintentar automáticamente. |
| HTTP 409 | CONFLICTO; no cambiar IDs ni sobrescribir por iniciativa del sincronizador. |
| HTTP 400/413/422 definitivos, sin alta | RECHAZADO. Permite corregir tras demostrar por respuesta definitiva que esa operación no produjo alta; nueva operationId y mismo animalId, dejando la anterior como sustituida localmente. |
| HTTP 429, 5xx o error de red | ERROR_REINTENTABLE; respetar Retry-After cuando exista. |
| Respuesta mal formada, IDs distintos o versión inválida | No confirmar localmente; error reintentable del mismo sobre y aviso de fallo de protocolo. |

Reintentos propuestos por ejecución: primer intento inmediato y hasta cuatro
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

| ID | Regla propuesta |
|---|---|
| RN01 | Animal pertenece a una única ganadería existente; ganaderiaId es inmutable en esta SPEC. creadoPor proviene de la identidad autenticada, no del cliente. |
| RN02 | animalId UUIDv4 generado localmente identifica la misma ficha online/offline; nombre y arete no son claves primarias. operationId UUIDv4 identifica una intención de alta, se genera una vez al confirmar y se conserva en reintentos. |
| RN03 | PostgreSQL aplica unicidad global de animalId y operationId. El efecto y su recibo de éxito se confirman en una transacción. Un fallo revierte ambos. |
| RN04 | Antes de consultar un recibo previo se revalidan permisos. Solo misma cuenta, ganadería y contenido canónico obtienen el recibo de la misma operación. Colisiones no revelan datos de otro ámbito. |
| RN05 | Alta usa tipo CREAR_ANIMAL y baseVersion=0 (espera inexistencia); éxito produce version=1 y estado ACTIVO. Una versión base distinta no es una edición: se rechaza como conflicto. |
| RN06 | Repetición idéntica de operationId devuelve el mismo efecto confirmado, sin una nueva alta; mismo ID con distinto contenido produce conflicto. Otro operationId para un animalId existente también produce conflicto, aunque los datos coincidan. |
| RN07 | El sexo es obligatorio: MACHO o HEMBRA, sin valor predeterminado. Nombre, arete, raza, edad, peso y fotografía son opcionales; su ausencia no bloquea el alta. Categoría puede quedar sin clasificar. |
| RN08 | Las seis categorías son declaradas; no se infieren por edad, parto ni rol reproductivo. Toda categoría informada debe ser compatible con el sexo obligatorio. Categoría vacía no crea una séptima categoría de negocio. |
| RN09 | Arete frontal y reverso admiten texto alfanumérico y marcas familiares. Una coincidencia de arete frontal normalizado en la misma ganadería avisa de posible duplicado, no bloquea ni fusiona animales. |
| RN10 | Los reintentos evitan duplicados técnicos, no detectan de forma infalible que dos personas registraron el mismo vacuno físico con IDs diferentes. La interfaz explica esta limitación. |
| RN11 | Pendientes locales se particionan por cuenta y ganadería; solo esa cuenta puede enviarlos con permisos actuales. No se guardan historiales completos ni credenciales en el almacenamiento offline de esta SPEC. |
| RN12 | Redis, IA, notificaciones y fotografías no participan en el alta ni en la transacción; su indisponibilidad no bloquea estos flujos. |

### Preparación y datos locales propuestos

Offline es voluntario y solo para dispositivo de confianza. Se descargan las
páginas/fichas básicas que el usuario haya solicitado, no todo el ganado de forma
implícita. Se indica qué información está disponible sin red y la fecha de copia;
la consulta local no se presenta como lista completa ni como estado actual remoto.
Se separan instantáneas confirmadas y registros pendientes propios por animalId.

La preparación recuerda cuenta/contexto, roles comprobados y última validación
online, sin contraseñas ni tokens en IndexedDB/localStorage. Se distingue:

- **Autorización temporal offline:** ventana propuesta de 7 días (168 horas)
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

## 9. Datos de entrada y salida propuestos

Datos técnicos los genera la aplicación, no el productor. Se rechazan campos
no admitidos (actor, estado, propietario, versión resultante o fotografías).
Cadenas: normalización Unicode NFC y recorte exterior; vacío equivale a null solo en campos opcionales. Sexo vacío/null siempre es inválido.
Límites en caracteres Unicode; cliente y servidor comparten casos de prueba.
Se rechazan controles invisibles, salvo salto de línea en observaciones.

| Campo | Tipo | Obligatorio | Validación propuesta |
|---|---|---|---|
| ganaderiaId | UUID en ruta/contexto | Sí, técnico | Ganadería accesible y activa; no se repite en cuerpo. |
| animalId | UUIDv4 | Sí, técnico | Formato/versión válidos; no depende del reloj ni del arete. |
| operationId | UUIDv4 | Sí, técnico | Único por intención; nunca se regenera en reintento. |
| schemaVersion | Entero | Sí, técnico | 1; otra versión -> 422 ESQUEMA_NO_SOPORTADO. |
| tipo | Enumeración | Sí, técnico | CREAR_ANIMAL. |
| baseVersion | Entero | Sí, técnico | 0; otro entero -> 409 VERSION_INCOMPATIBLE. |
| sexo | Enumeración | Sí | MACHO o HEMBRA. Ausencia, null, vacío u otro valor se rechazan en cliente y servidor; sin valor predeterminado ni inferencia. |
| nombre | Texto o null | No | Máximo 100 caracteres; admite espacios y acentos. |
| areteFrente | Texto o null | No | Máximo 100; no solo numérico, sin unicidad obligatoria. |
| areteReverso | Texto o null | No | Máximo 100; marca de propiedad distinta del nombre del animal. |
| raza | Texto o null | No | Máximo 100; declarada, sin catálogo de razas nuevo. |
| fechaNacimiento | Fecha ISO o null | No | Fecha real, no futura respecto a fecha del servidor en America/Lima; no se impone edad máxima. |
| nacimientoEstimado | Booleano | Sí si hay fecha | Distingue fecha aproximada; sin fecha se omite o es null. |
| categoria | Enumeración o null | No | TERNERA, VAQUILLA, VACA, TERNERO, TORETE, TORO; compatible con el sexo obligatorio. |
| observaciones | Texto o null | No | Máximo 2000; texto plano, nunca HTML ejecutable. |

Compatibilidad: TERNERA/VAQUILLA/VACA con HEMBRA; TERNERO/TORETE/TORO con MACHO.
Sin sexo válido no se guarda localmente ni se confirma alta remota; la categoría no sustituye la selección de sexo.
Cliente valida fecha con su reloj como ayuda; el servidor decide al sincronizar.
El alta mínima contiene sexo y los datos técnicos generados por la aplicación. La edad es opcional y se deriva solo si existe fecha de nacimiento. Peso y foto no son requisitos; su captura queda fuera de este incremento según sección 4, sin añadir campos productivos ni almacenamiento de imágenes.

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

## 11. Criterios de aceptación propuestos

La regla de sexo obligatorio ya está confirmada; los criterios completos requieren aprobación y ejecución posterior. Dado/Cuando/Entonces define
el resultado verificable, no una prueba ya aprobada. Se conservan CA01–CA08
con precisiones: CA01 exige sexo por la decisión confirmada y CA08 no infiere categorías. Se retira la propuesta anterior de sexo opcional; se concreta esta validación en SPEC sin reescribir RF/ADR aprobados.

| ID | Dado | Cuando | Entonces |
|---|---|---|---|
| CA01 | Propietario/editor autenticado, miembro vigente y ganadería activa | Confirma alta con solo sexo MACHO o HEMBRA y datos técnicos generados | Una ficha ACTIVO, version=1, ganadería correcta y animalId estable; no exige nombre, arete, raza, edad, peso ni foto. Sin sexo válido no hay guardado local ni alta remota. |
| CA02 | Sexo obligatorio y datos opcionales con letras, números, acentos y ausencias | Guarda/consulta o envía sexo omitido/null/vacío/no admitido | Valores opcionales normalizados se conservan; límites y fechas inválidos se rechazan. Sexo inválido produce error de campo sin escritura; la API devuelve 422. |
| CA03 | Autenticación, pertenencia y roles previamente implementados y verificados | Propietario/editor/lector, administrador sin membresía, usuario ajeno y petición anónima intentan alta/consulta | Propietario/editor crean y leen; lector solo lee; ajeno y administrador sin membresía no acceden; petición anónima recibe 401. No se habilita registro real con autenticación o membresías simuladas. |
| CA04 | Dispositivo preparado offline con permiso local vigente | Confirma alta, reinicia aplicación y reconecta | Pendiente persiste; tras autorización actual se confirma una sola ficha con los mismos IDs. |
| CA05 | Alta confirmada cuya respuesta se perdió | Se reenvía concurrentemente el mismo sobre, incluso desde dos pestañas | Un Animal y un recibo de éxito; misma confirmación y ningún efecto adicional. |
| CA06 | Editor con alta local pendiente | Su membresía se revoca o pasa a lector antes del envío | El backend rechaza; no escribe; el cliente bloquea cola/retira copia según política, sin acceso ajeno. |
| CA07 | Guardado local o solicitud con respuesta desconocida | Se muestra el estado | Nunca se afirma «Sincronizado» sin confirmación válida; error de cuota no se muestra como guardado. |
| CA08 | Sexo MACHO/HEMBRA seleccionado y fecha/edad/categoría opcionales | Se valida el alta | No se infiere sexo ni categoría por edad; categoría incompatible se rechaza y categoría null se muestra sin clasificar. La categoría nunca permite omitir sexo. |
| CA09 | Mismo operationId y contenido diferente, o animalId ya existente con otra operación | Se sincroniza | 409 genérico, sin sobrescritura ni datos ajenos; no se regenera identidad automáticamente. |
| CA10 | Alta con baseVersion distinta de 0 o esquema no soportado | Llega a API | 409 de versión o 422 de esquema, respectivamente; sin modificar Animal ni recibo de éxito. |
| CA11 | Fallo de PostgreSQL entre alta y recibo, o fallo local al guardar confirmación | Se reintenta | No queda alta parcial sin recibo; una confirmación remota recuperable no origina duplicado. |
| CA12 | Token caducado o contexto de sesión perdido, con cola local | Permanece offline o recupera red | Conserva la cola. Token caducado con contexto offline vigente permite trabajo local sin envío; contexto perdido exige login y bloquea consulta/alta. Al volver la red, autenticar la misma cuenta y validar ganadería/rol permite recuperar/enviar con los mismos IDs; 401 pausa y 403 bloquea sin borrar pendientes. |
| CA13 | Mismo arete frontal en una ganadería o misma descripción física con IDs distintos | Se registra otra alta intencionada | Advierte coincidencia conocida sin fusionar/bloquear; no revela coincidencias de otra ganadería ni promete deduplicación física. |
| CA14 | Contexto validado hace 168 horas y cola con pendientes/resultados desconocidos | Vence ventana, cierra sesión/cambia cuenta o revalida después | Vencimiento bloquea acceso/nuevas altas sin eliminar ni modificar sobres/IDs. Cierre invalida contexto y limpia copias sincronizadas, conservando pendientes bloqueados. Otra cuenta no accede. Revalidar la cuenta original permite recuperarlos; solo descarte separado y explícito permite borrarlos. |
| CA15 | Lista paginada online y algunas páginas disponibles offline | Consulta «Mi ganado» y ficha en móvil/escritorio | Respuestas autorizadas, orden estable, sin duplicar provisional/confirmado; copia incompleta etiquetada con fecha y cobertura. |
| CA16 | Navegador sin preparación, sin UUID seguro o IndexedDB fallido | Intenta registrar offline | Informa la limitación sin afirmar guardado ni generar IDs inseguros; una respuesta mal formada tampoco confirma. |
| CA17 | Redis, IA y notificaciones no disponibles | Registra y consulta animales | Flujos privados siguen funcionando sin consultar esas integraciones. |
| CA18 | Entorno y datos definidos en plan | Se ejecuta carga de consultas con 100 concurrentes | p95 ≤ 2 s, errores funcionales 0 en escenario sin fallos; exclusiones de IA/fotos y resultados reales documentados. |

## 12. Pruebas necesarias y trazabilidad

Herramientas propuestas, aún no instaladas/ejecutadas: JUnit 5 y dobles para
núcleo, Spring Boot Test y Testcontainers/PostgreSQL para integración, Vitest
para cliente, Playwright para E2E y k6 para carga. Versiones se fijarán antes
de implementar. Se probarán el navegador real y su IndexedDB además de dobles.

| ID | Tipo y escenario | Criterios |
|---|---|---|
| PU01 | Validadores: sexo omitido/null/vacío/inválido, MACHO/HEMBRA válidos, opcionales ausentes, límites 100/101 y 2000/2001, Unicode, fechas y categorías. | CA01, CA02, CA08 |
| PU02 | Autorización por cuenta/ganadería/rol/estado; caso de uso con repositorios simulados. | CA03, CA06 |
| PU03 | Generación UUID inyectable, sobre inmutable, estados, backoff y parser de confirmaciones. | CA04, CA07, CA10, CA12, CA16 |
| PU04 | Normalización y huella de operación, IDs distintos/contenido cambiado, deduplicación de vistas. | CA05, CA09, CA13, CA15 |
| PI01 | API real y PostgreSQL: matriz de permisos, IDOR, membresía revocada y token inválido. | CA03, CA06, CA12 |
| PI02 | Concurrencia, unicidad, respuesta perdida, rollback entre Animal/recibo, revocación concurrente ordenada. | CA05, CA06, CA09, CA11 |
| PI03 | Sexo obligatorio en API: ausente/null/vacío/inválido -> 422 sin alta; esquema/versiones/campos no admitidos, listado estable, aislamiento y advertencia arete. | CA01, CA02, CA08, CA10, CA13, CA15 |
| PI04 | Cliente/IndexedDB real: sexo inválido offline no crea provisional/cola; commit/abort, cuota simulada, recuperación tras recarga y fallo al confirmar localmente. | CA01, CA02, CA04, CA07, CA11, CA16 |
| PI05 | Arranque y consultas sin conexión con Redis/IA/notificaciones; sin dependencias en flujo privado. | CA17 |
| EE01 | Propietario/editor registran con MACHO/HEMBRA y lector consulta; controles UI y API, rechazo de sexo ausente, 360 px y escritorio. | CA01, CA02, CA03, CA08, CA15 |
| EE02 | Preparar, desconectar, guardar, cerrar/reabrir, reconectar; doble clic/pestaña, caída tras commit y reenvío único. | CA04, CA05, CA07, CA11 |
| EE03 | Token expirado y contexto perdido por separado; antes/a las/después de 168 horas, revocación/cambio a lector, conservación exacta de cola al salir y recuperación al reautenticar la misma cuenta. | CA06, CA12, CA14 |
| EE04 | Cuota, incompatibilidad, colisión inyectada, versiones y errores permanentes/temporales/protocolo. | CA09, CA10, CA13, CA16 |
| PC01 | Carga reproducible de listado/ficha autorizados según plan, sin caché privada. | CA18 |

Las colisiones se inyectan con generador controlado; generar muchos UUID sin
colisión no demuestra unicidad absoluta. Pruebas de carga se ejecutarán al
existir versión ejecutable y ante cambios de rendimiento, no en esta tarea.
Venta, invalidación de anuncios, genealogía, IA y fotografías no se prueban como
implementadas aquí: pertenecen a sus SPEC. Las pruebas Redis de catálogo siguen
obligatorias para el producto; no aplica caché vacía/poblada a consultas privadas.

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

## 13. Decisiones pendientes de aprobación

| ID | Propuesta concreta | Impacto / condición antes de implementar |
|---|---|---|
| DP01 | Roles y alcance de sección 3; ganadería/membresía existentes como dependencia. | Aprobar matriz y contrato de autenticación/revocación; esta SPEC no implementa invitaciones. |
| DP02 | UUIDv4 local, operación inmutable, recibos sin caducidad en este incremento. | Aprobar estrategia técnica y política futura de conservación antes de limpiar recibos. |
| DP03 | Sexo obligatorio MACHO/HEMBRA confirmado por el responsable; resto de campos/límites y categorías propuestos. | Se retira sexo opcional. Nombre, arete, raza, edad, peso y foto no son requisitos; peso/foto siguen sin captura en este incremento. |
| DP04 | Alta individual con sexo obligatorio según DP03, listado/ficha y offline; no edición remota ni masivo. | Autenticación y acceso familiar previos; registro masivo mediante otra SPEC. No exigir datos opcionales para crear. |
| DP05 | Conflictos sin fusión, reintentos, corrección tras 422 y API del plan. | Revisar experiencia de recuperación antes del código; no prometer reconciliación automática. |
| DP06 | Ventana de autorización offline propuesta de 7 días, separada de conservación local. | Vencimiento/pérdida de sesión no borran pendientes. Revalidar recupera acceso; revocar bloquea; descarte solo explícito. Riesgo del perfil/dispositivo por aceptar. |
| DP07 | Herramientas y protocolo PC01, más verificación de política offline DP06. | Probar vencimiento de 168 h sin eliminar cola, pérdida de sesión, reconexión, revalidación y aislamiento; herramientas/versiones siguen por aprobar. |
| DP08 | Reloj America/Lima para fecha futura y paginación propuesta. | Confirmar zona de negocio y parámetros API. |

Se prepara para aprobación; no está aprobada todavía. Si cambia una propuesta,
se actualizan plan, tareas y CA afectados. No se renumeran los RF/ADR existentes.

## 14. Evidencia de validación

| Validación | Estado | Evidencia |
|---|---|---|
| Coherencia documental, enlaces y códigos | Comprobada en esta edición | 18 CA con pruebas asociadas; 23 tareas; 52 enlaces locales válidos; códigos definidos; tablas y git diff --check sin errores. No equivale a ejecución funcional. |
| PU01–PU04, PI01–PI05, EE01–EE04, PC01 | No ejecutadas | Sin código de aplicación ni resultados |
| Aprobación de SPEC/plan/tareas | Pendiente | Revisión del responsable del proyecto |

## 15. Condición de finalización

Primero aprobar DP01–DP08 y el contrato de acceso dependiente; después ejecutar
las tareas incrementales. La funcionalidad solo se completará con implementación,
pruebas aplicables aprobadas, resultados reales vinculados a CA, documentación
actualizada y revisión final. La redacción de estos documentos no cumple esa condición.
