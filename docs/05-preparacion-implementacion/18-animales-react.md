# T10 — Registro, listado y ficha de animales en React

Fecha: 18 de noviembre de 2026, America/Lima. Estado: **cliente online
implementado y verificado automática y manualmente para el flujo existente**.

## Alcance implementado

- La ganadería seleccionada muestra la sección responsive «Mis animales» y
  revalida `GET /api/v1/ganaderias/{id}/acceso` antes de habilitar el contexto.
  La selección y el rol del cliente no sustituyen la autorización del backend.
- PROPIETARIO y EDITOR pueden usar el formulario individual; LECTOR consulta
  listado y ficha sin recibir controles de escritura.
- El formulario exige sexo MACHO o HEMBRA. Nombre, arete frontal/reverso, raza,
  fecha de nacimiento, marca de fecha estimada, categoría compatible y
  observaciones son opcionales con los límites de SPEC-01. No se añadieron
  peso, fotografía, edad capturada ni campos de actor/ganadería en el cuerpo.
- El navegador genera `animalId` y `operationId` con `crypto.randomUUID()`.
  Ante respuesta incierta mantiene el sobre en memoria, bloquea su edición y
  reintenta los mismos identificadores. Esto no reemplaza la cola persistente
  de T11/T13: una recarga aún no conserva una operación incierta.
- El POST reutiliza CSRF y JWT solo en memoria. El listado consume el cursor
  opaco de T09, deduplica por `animalId` y permite abrir la ficha mediante el
  GET individual. No usa localStorage, sessionStorage ni IndexedDB para animales
  o credenciales.
- Cambiar de ganadería desmonta el contexto anterior y reinicia formulario,
  lista, cursor y ficha. Un 401 invalida la sesión local; un 403 bloquea el
  contexto y exige seleccionar/revalidar nuevamente.

No se implementaron IndexedDB, PWA/offline, sincronización, edición, vacunas,
genealogía, ventas, invitaciones, catálogo ni IA.

## Verificación real

| Verificación | Resultado |
|---|---|
| Pruebas dirigidas finales de `AnimalesPanel` | 9 aprobadas, 0 fallos. |
| Pruebas dirigidas del cliente REST de animales | 5 aprobadas, 0 fallos. |
| Regresión frontend completa | 51 aprobadas en 8 archivos, 0 fallos. |
| TypeScript `tsc -b` | Correcto. |
| Build Vite de producción | Correcto; 25 módulos transformados. |
| Comprobación manual HTTPS comunicada por el responsable | FIONA permaneció registrada después de F5 y su ficha mostró correctamente los datos existentes. |

La regresión completa se repitió al finalizar después del último ajuste
productivo. TypeScript forma parte también del build final y terminó sin errores.

Las pruebas automatizadas cubren alta válida e inválida, sexo obligatorio,
listado vacío/con datos, ficha, paginación y deduplicación, propietario/editor/
lector, sesión vencida, membresía rechazada, cambio de ganadería y replay del
mismo sobre ante respuesta incierta. También verifican el envío de JWT/CSRF sin
persistencia y el rechazo de respuestas que mezclen otra ganadería.

No se ejecutó Playwright ni se documentó una revisión visual sistemática en
320/360/390/768/1440 px en este incremento. La comprobación manual de FIONA no
incluyó los campos textiles añadidos posteriormente por T19-ID. Tampoco se
repitieron aquí las 124 pruebas backend porque T10 no modificó backend. Estas
son limitaciones, no resultados aprobados.

## Trazabilidad y pendientes

| Elemento | Estado de este incremento |
|---|---|
| T10 | Implementada para el flujo online React y cubierta por pruebas de componentes/cliente. |
| CA01 / CA02 / CA08 | Parciales: validación y formulario online cubiertos; falta offline y E2E real. |
| CA03 | Parcial: permisos visibles usan `/acceso`, y backend sigue siendo autoridad; editor/lector continúan siendo fixtures sin alta familiar productiva. |
| CA05 / CA09 | Parciales: mismo sobre en reintento incierto dentro de la vida de la página; falta persistencia y coordinación multipestaña. |
| CA15 / EE01 | Parciales: lista/ficha online automatizadas y persistencia de FIONA tras F5 observada manualmente; faltan revisión responsive sistemática y recorrido E2E completo. |
| T11–T14 / EE02–EE04 | Pendientes; no hay funcionamiento offline ni sincronización ficticia. |
