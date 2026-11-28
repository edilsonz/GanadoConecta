# SPEC-08 — Anuncios y catálogo público

ACT01/ACT02 → HU12/HU13/HU14 → RF12/RF13/RF14/RF18 → AC01/AC04/AC05, ADR-08/ADR-11/ADR-14/ADR-15.

El propietario vigente prepara anuncios de animales de su ganadería. Indica título, características, ubicación general, precio solicitado en PEN y, opcionalmente, un contacto con autorización explícita. La categoría, raza y sexo provienen de campos de ficha permitidos. La genealogía es optativa por anuncio y se limita a relación, categoría y raza registradas de madre y padre, sin nombres, identificadores ni historial. El propietario puede publicar desde borrador, pausar, publicar nuevamente tras pausa y retirar. El retiro es final.

El visitante consulta anuncios publicados, filtra por categoría, raza, sexo, ubicación general y rango de precio, y navega por páginas. La proyección pública excluye IDs internos de animales y ganaderías, anotaciones, aretes, ubicación precisa y cualquier historial. El formulario solicita expresamente que la ubicación y características ingresadas sean públicas y generales.

| Criterio | Verificación |
|---|---|
| CA08-01 | Titular crea y edita, y controla visibilidad; revocados, roles insuficientes y ajenos se rechazan. |
| CA08-02 | Catálogo anónimo con filtros, orden estable y paginación; pausa y retiro dejan de aparecer inmediatamente. |
| CA08-03 | Contacto y genealogía se proyectan solo tras autorización explícita. |
| CA08-04 | Fotos JPEG/PNG de hasta 5 MiB y 12 megapíxeles se decodifican y reencodifican; se guardan hasta cinco por anuncio. Solo un anuncio público habilita su consulta pública. |

El almacenamiento local usa un adaptador de desarrollo configurable. El proveedor definitivo queda pendiente. El flujo inicial excluye pagos, negociación, transporte y transferencias.

La lectura pública puede usar Redis detrás del contrato de caché del catálogo.
Debe devolver la misma proyección y paginación que PostgreSQL. Los cambios
confirmados que alteran visibilidad o contenido invalidan la revisión anterior.
Ante ausencia, caducidad o caída de Redis, la consulta usa PostgreSQL.
[Diseño y criterios técnicos de caché](plan.md#caché-del-catálogo-público--adr-11)
