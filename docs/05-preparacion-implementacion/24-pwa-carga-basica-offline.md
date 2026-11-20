# PWA — Instalación y carga básica sin conexión

**Estado: instalación y carga del app shell verificadas manualmente; ciclo de actualización segura verificado automáticamente y pendiente de prueba manual con dos builds.**

## Alcance implementado

- Vite genera un manifiesto instalable en español, iconos PNG de 192 y 512 px
  (incluido uno `maskable`) y un Service Worker de producción mediante
  `vite-plugin-pwa` 1.3.0.
- El Service Worker precachea únicamente el app shell estático: HTML, JavaScript,
  CSS, manifiesto, iconos y recursos de presentación. No existe estrategia de
  caché en tiempo de ejecución para `/api`, autenticación, ganaderías, animales
  ni respuestas privadas.
- La navegación offline puede recuperar `index.html`, excepto las rutas `/api`.
  Abrir desde cero sin sesión en memoria muestra una pantalla segura y no revela
  ganaderías, animales ni operaciones de IndexedDB.
- Una sesión que ya estaba en memoria puede continuar usando solamente las
  capacidades offline que fueron preparadas y autorizadas previamente. El
  Service Worker no concede permisos ni amplía la ventana local de 168 horas.
- La instalación se ofrece únicamente cuando Chrome emite
  `beforeinstallprompt`. La disponibilidad del app shell y los errores de
  preparación se comunican sin afirmar acceso a datos privados.
- Las actualizaciones se notifican y requieren confirmación. No se recarga la
  página automáticamente. Los formularios con cambios y la sincronización manual
  mantienen un bloqueo compartido; «Actualizar ahora» obtiene uno exclusivo. En
  Chrome/Web Locks esto también impide aplicar la versión mientras otra ventana
  mantiene una de esas actividades.
- Si la actualización se aplica, una marca no sensible y de vida corta en
  `sessionStorage` permite confirmar el resultado después de la recarga. La marca
  no identifica al usuario, no autoriza acceso y se consume al mostrar el aviso.
  Un fallo conserva la versión anterior y permite volver a intentar.
- El Service Worker no abre, migra ni elimina IndexedDB. La cola, sus UUID y los
  recibos permanecen bajo los adaptadores T11–T15 existentes.
- La sincronización continúa siendo exclusivamente manual mediante «Sincronizar
  pendientes». No se añadió Background Sync, envío por evento `online`, backoff
  automático ni login offline.

## Verificación ejecutada

- Pruebas dirigidas: contrato del manifiesto/precaché, registro solo en
  producción, instalación condicionada al evento del navegador, aviso de nueva
  versión sin recarga automática, pantalla segura sin sesión y regresiones de
  cola/sincronización manual.
- TypeScript y build de producción correctos. El build generó
  `manifest.webmanifest`, `sw.js` y el módulo Workbox; el manifiesto contiene los
  campos e iconos esperados y el precaché contiene ocho recursos estáticos
  únicos, sin caché de ejecución para API.
- Los PNG se comprobaron en 192×192, 512×512 y 512×512. El script
  `frontend/scripts/generar-iconos-pwa.ps1` permite regenerarlos sin depender de
  certificados, secretos ni servicios externos.
- El preview de producción respondió 200 para `/`, `/manifest.webmanifest` y
  `/sw.js` en el puerto alternativo 4173. Esta comprobación HTTP de artefactos no
  acredita instalación ni comportamiento HTTPS en Chrome.
- No se ejecutaron pruebas backend porque este incremento no modifica Spring
  Boot, PostgreSQL ni Flyway.

La regresión de este incremento aprobó 134/134 pruebas en 16 archivos, TypeScript
y build. Antes se ejecutaron 83/83 pruebas dirigidas de PWA, autorización local,
IndexedDB, registro y sincronización. El artefacto conserva ocho entradas únicas
de precaché y ninguna estrategia para datos privados. Estas comprobaciones no
sustituyen la actualización A→B ni la coordinación multipestaña en Chrome real.

## Validación manual observada

El responsable confirmó el 19 de noviembre de 2026 que GanadoConecta se instaló
como PWA en Windows y que, después de una primera carga, el app shell volvió a
mostrarse al activar Offline y pulsar F5. Esta observación no acredita un cierre
total con restauración de datos privados: sin identidad en memoria la barrera
segura debe seguir ocultándolos.

La comprobación manual previa también confirmó que `PRUEBA-OFFLINE-02` se envió
manualmente a PostgreSQL y permaneció tras F5 sin duplicados visibles. La PWA no
automatiza ese envío.

## Validación manual pendiente

1. Servir el build bajo HTTPS confiable en `https://127.0.0.1:5173/`, cargar una
   vez online y comprobar Manifest y Service Workers en DevTools.
2. Con la PWA ya instalada, activar Offline, cerrar totalmente y reabrirla. Sin
   sesión en memoria debe aparecer la barrera segura y no información privada.
3. Recuperar red, autenticarse y seleccionar la misma ganadería. Verificar que
   IndexedDB conserva las operaciones/recibos previos y que sincronizar sigue
   exigiendo el botón explícito.
4. Para probar una actualización, mantener abierto el build A, publicar el build
   B en el mismo origen y recargar o esperar la comprobación del navegador. Debe
   mostrarse «Nueva versión disponible» sin recarga automática.
5. Comprobar que el botón queda deshabilitado mientras haya un formulario con
   cambios o una sincronización en curso, incluso si la actividad está en otra
   ventana Chrome. Al quedar libres, pulsar «Actualizar ahora» y verificar el
   aviso de aplicación correcta tras la recarga.
6. Simular un fallo de descarga sin borrar Cache Storage: el build A debe seguir
   disponible y las operaciones IndexedDB deben conservarse.

No usar «Clear site data» ni borrar IndexedDB durante esta validación.

## Pendientes

- Validación manual de cierre/reapertura completa y actualización A→B en Chrome.
- Caché privada selectiva de fichas confirmadas, su retirada al revocar permisos
  y los E2E de navegador correspondientes.
- Backoff/`Retry-After`, sincronización automática y resolución asistida de
  conflictos. No forman parte de este incremento.
- Sin Web Locks solo puede garantizarse el bloqueo de la ventana actual; la
  coordinación multipestaña requiere una prueba real en el navegador objetivo.
