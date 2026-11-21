# Sincronización sanitaria offline — SPEC-02 S07

El [flujo editable de arquitectura](../02-arquitectura-software/diagramas/ganadoconecta-tecnico.drawio)
y su [vista SVG](../02-arquitectura-software/diagramas/sincronizacion-offline.svg)
resumen la autorización, el reenvío y la confirmación local.

## Resultado

El orquestador existente sincroniza ahora `CREAR_ANIMAL`, vacunaciones y los
eventos `DESPARASITACION`, `TRATAMIENTO_SANITARIO` y
`REVISION_VETERINARIA`. No existe un segundo motor: manual y automática
voluntaria invocan el mismo servicio, con revalidación previa de sesión y
membresía, Web Locks, lease recuperable, timeout y clasificación de errores.
Cada endpoint vuelve a autorizar la escritura dentro de su transacción.

IndexedDB permanece en V3 porque no cambió su esquema de stores o índices. Los
registros sanitarios V3 se normalizan de forma compatible al leerlos; reciben
estado, intentos, lease y comprobante sin borrar o reinicializar la base. La cola
histórica de animales y sus comprobantes no se modificaron.

## Confirmación e idempotencia

- `animalId`, `eventId`, `operationId` y payload se reenvían sin cambios.
- Una caída de red o respuesta perdida conserva la operación como reintentable.
- `APLICADO` y `REPROCESADO` son válidos únicamente si los identificadores y la
  fecha del comprobante coinciden con la solicitud.
- Solo entonces IndexedDB marca `SINCRONIZADO`; un error al guardar el recibo no
  se presenta como éxito local.
- 401 exige recuperar sesión; 403 bloquea el ámbito; 409 y validación requieren
  intervención; red y servidor permanecen recuperables.

React muestra los hechos confirmados por separado de vacunaciones y eventos
locales. El control manual y la activación automática existentes consideran
ambas colas. La selección visual de ganadería nunca sustituye la autorización
del backend.

## Evidencia ejecutada el 20/11/2026

- Pruebas dirigidas: compatibilidad V3, vacunaciones, eventos, payload e IDs,
  recibos, respuesta perdida, revocación, errores, aislamiento, lease vencido y
  coordinación simulada entre ventanas.
- Regresión frontend: 211/211 pruebas, sin fallos; TypeScript y build PWA
  correctos. Tras ajustar la clasificación de un fallo 5xx al obtener CSRF, se
  ejecutaron 8/8 pruebas dirigidas y nuevamente el build PWA/TypeScript.
- PostgreSQL 17.12 saludable: 8/8 pruebas sanitarias con Flyway V1–V8 en
  `gc_sanidad_test_20261120`; cubren autorización transaccional, aislamiento,
  CSRF, idempotencia y `REPROCESADO`.
- No se repitió la regresión backend completa de 146 pruebas porque no cambió
  código backend. No se insertaron datos en la base de desarrollo.

No se ejecutó aún una prueba manual de navegador para el nuevo envío sanitario
ni una prueba real entre dos ventanas; la coordinación multipestaña fue
automatizada con el mismo contrato de Web Locks y leases.
