# T04–T07 — Núcleo de registro individual de animales

Fecha: 18 de noviembre de 2026, America/Lima. Estado: **núcleo backend
implementado y verificado; endpoint HTTP, React y offline pendientes**.

## Alcance implementado

- Flyway V4 crea `ganado.animal` y `ganado.operacion_confirmada` sin modificar
  V1–V3. Incluye FKs a Identidad, sexo `MACHO`/`HEMBRA` obligatorio, categorías
  compatibles, versión inicial, índices y recibo con FK diferible.
- El dominio exige UUIDv4 distintos para animal y operación, sexo obligatorio,
  normaliza opcionales NFC/espacios y valida límites, categoría y fecha de negocio
  `America/Lima`.
- `RegistrarAnimal` usa puertos de autorización, persistencia, reloj, huella y
  unidad de trabajo. La canonicalización v1 incluye actor, ganadería, animal,
  metadatos versionados y datos normalizados; la huella SHA-256 se calcula en servidor.
- La autorización transaccional bloquea sesión/cuenta, ganadería y membresía en
  orden estable. Solo `PROPIETARIO` y `EDITOR` escriben; `LECTOR`, ajeno o
  revocado son rechazados antes de consultar recibos previos.
- Operación, animal y recibo se confirman en una transacción. Un reintento
  idéntico devuelve el recibo original; cambios de contenido o colisiones de ID
  producen un conflicto genérico. La coincidencia normalizada de `areteFrente`
  genera una advertencia no bloqueante conservada en el recibo; `areteReverso`
  no se usa como identificador individual.

No se añadió controlador, DTO ni ruta de animales. Tampoco se implementaron
listado/ficha, formulario React, IndexedDB, sincronización o datos familiares reales.

## Verificación real

PostgreSQL 17.12 en Docker se comprobó `healthy` y publicado solo en
`127.0.0.1:5433`. Las variables privadas se cargaron al proceso sin imprimirlas.

| Verificación | Resultado |
|---|---|
| Pruebas unitarias dirigidas | 17 aprobadas: dominio, UUIDv4, contrato versionado, canonicalización, caso de uso y matriz de permisos. |
| Integración dirigida PostgreSQL | 8 aprobadas: persistencia, restricciones, idempotencia secuencial/concurrente, rollback, roles, aislamiento, revocación y ambos órdenes de carrera. |
| Regresión backend final | 104 aprobadas, 0 fallos, 0 errores y 0 omitidas. |
| Flyway | 4 migraciones validadas; esquema en V4 sin ejecutar `clean` ni recrear volúmenes. |

La primera regresión señaló únicamente una expectativa de prueba fijada en V3;
se actualizó para validar V4 y las tablas nuevas, se ejecutó el caso específico y
la regresión final indicada arriba.

## Trazabilidad y límites

| Elemento | Estado de este incremento |
|---|---|
| T04 | Completada y verificada con PostgreSQL. |
| T05 | Completada en dominio con pruebas unitarias. |
| T06 | Completada para el alta individual: puertos y huella canónica v1. |
| T07 | Completada en núcleo y persistencia transaccional. |
| CA01/CA02/CA03/CA05/CA06/CA08/CA09/CA10/CA11 | Cobertura parcial backend; falta completar criterios por HTTP, cliente y offline. |
| T08–T18 | Pendientes según dependencias; no se presenta el alta como disponible al usuario. |

V4 ya fue aplicada en la base controlada. Cualquier evolución posterior debe usar
una nueva migración; no se debe editar V1–V4 después de registrar este incremento.
