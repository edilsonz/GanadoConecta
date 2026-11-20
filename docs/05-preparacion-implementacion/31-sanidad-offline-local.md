# Sanidad offline local — SPEC-02 S07

> Evidencia del incremento de preparación local. El bloqueo de transmisión aquí
> documentado se resolvió posteriormente mediante el motor único descrito en
> [32-sincronizacion-sanitaria-offline.md](32-sincronizacion-sanitaria-offline.md).

## Alcance implementado

La base `ganadoconecta-offline` aumenta aditivamente de la versión 2 a la 3 y
crea el almacén `eventosSanitariosPendientes`. Conserva sin modificar los
almacenes, operaciones y comprobantes usados por el registro de animales.

Los eventos de desparasitación, tratamiento sanitario y revisión veterinaria
pueden conservarse localmente cuando existe una autorización offline vigente de
escritura para la misma cuenta y ganadería. Cada operación mantiene sin cambios:

- `usuarioId`, `ganaderiaId` y `animalId`;
- UUIDv4 de `eventId` y `operationId`;
- el cuerpo validado y la marca local de creación;
- el estado explícito `PENDIENTE`.

La ficha muestra estos datos separados del historial confirmado. La interfaz no
afirma que PostgreSQL los haya recibido. Un error HTTP no crea un pendiente:
solo el modo offline del navegador o un fallo real de transporte habilitan el
guardado local. El vencimiento de las 168 horas bloquea nuevas escrituras pero
no elimina los datos existentes. IndexedDB no recibe JWT, cookies, contraseñas
ni tokens de renovación.

## Límite seguro

No se implementó el envío de estos eventos. El orquestador actual, sus leases,
Web Locks, respuestas y comprobantes persistidos están tipados para
`CREAR_ANIMAL`. Adaptar solo una llamada HTTP produciría un segundo flujo o
dejaría incompletas las garantías ante respuesta perdida y doble pestaña.

El siguiente incremento deberá generalizar un único protocolo de cola para
varios tipos de operación, conservar los comprobantes sanitarios y revalidar
sesión, membresía y rol antes de transmitir. Hasta entonces no existe botón de
sincronización sanitaria manual ni automática.

## Verificación

- Pruebas dirigidas de reapertura, aislamiento, idempotencia, conflicto,
  coexistencia con la cola de animales y fallo de escritura.
- Pruebas de la barrera de autorización para propietario/editor, lector, sesión
  vencida y acceso bloqueado.
- Pruebas React de presentación separada y conservación del formulario si falla
  IndexedDB.
- Pruebas del cliente para distinguir caída de red de HTTP 500.
- Regresión final: 191/191 pruebas frontend y 146/146 pruebas backend, sin
  fallos, errores ni omisiones; TypeScript y build PWA correctos. PostgreSQL
  17.12 estaba saludable y Flyway validó V1–V8 en la base aislada.

La regresión permitió corregir dos expectativas antiguas de pruebas: la versión
Flyway ya no era V6 sino V8, y la comprobación de conectividad ahora acepta el
nombre de la base aislada configurada en vez de exigir la base de desarrollo.

No se ejecutó una prueba manual en Chrome ni se sincronizó ningún dato sanitario
contra PostgreSQL durante este incremento.
