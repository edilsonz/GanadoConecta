# PWA — Instalación y carga básica sin conexión

**Estado: implementada y verificada automáticamente; instalación y apertura offline en Chrome pendientes de validación manual.**

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
  página automáticamente: se pide terminar formularios o sincronizaciones antes
  de aplicar la nueva versión.
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

La regresión frontend final aprobó 122/122 pruebas en 15 archivos. Tras depurar
duplicados del precaché cambió solo el contrato PWA: sus 2/2 pruebas dirigidas y
el build volvieron a aprobarse. Estas comprobaciones automatizadas no sustituyen
la instalación ni la prueba offline real en Chrome.

## Validación manual pendiente

1. Servir el build bajo HTTPS confiable en `https://127.0.0.1:5173/`, cargar una
   vez online y comprobar Manifest y Service Workers en DevTools.
2. Instalar desde el aviso de GanadoConecta o el control de instalación de
   Chrome; confirmar que abre en modo independiente.
3. Sin borrar datos del sitio, activar Offline, cerrar y reabrir la PWA. Sin
   sesión en memoria debe aparecer la barrera segura y no información privada.
4. Recuperar red, autenticarse y seleccionar la misma ganadería. Verificar que
   IndexedDB conserva las operaciones/recibos previos y que sincronizar sigue
   exigiendo el botón explícito.
5. Para probar una actualización futura, publicar otro build en el mismo origen:
   debe mostrarse el aviso y no recargarse hasta pulsar «Actualizar ahora».

No usar «Clear site data» ni borrar IndexedDB durante esta validación.

## Pendientes

- Validación manual de instalación, reapertura offline y actualización en Chrome.
- Caché privada selectiva de fichas confirmadas, su retirada al revocar permisos
  y los E2E de navegador correspondientes.
- Backoff/`Retry-After`, sincronización automática y resolución asistida de
  conflictos. No forman parte de este incremento.
