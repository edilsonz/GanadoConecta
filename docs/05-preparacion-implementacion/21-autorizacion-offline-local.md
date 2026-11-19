# T12/T15 — Autorización local inicial por cuenta y ganadería

Fecha: 19 de noviembre de 2026, America/Lima. Estado: **infraestructura
implementada y verificada con dobles; integración React y navegador real
pendientes**.

## Alcance implementado

- La base `ganadoconecta-offline` sube de versión 1 a 2 de forma aditiva y crea
  `contextosOffline` sin eliminar el store de operaciones pendientes.
- `ServicioAutorizacionOffline.revalidarOnline` consume el contrato existente
  `GET /api/v1/ganaderias/{id}/acceso`. Asocia su rol, estado y `validadoEn` con
  el `usuarioId` de la sesión en memoria y la ganadería exacta.
- Solo PROPIETARIO y EDITOR obtienen resultado local de escritura. LECTOR queda
  en solo lectura. Un ámbito distinto devuelve AUSENTE.
- El contexto persiste datos mínimos no secretos: cuenta, ganadería, rol,
  estado/motivo y anclas temporales. No recibe ni guarda contraseña, JWT,
  refresh token, cookie o token CSRF.
- La vigencia termina al primero de los límites de 168 horas calculados desde
  la validación servidor y su observación cliente. Un retroceso del reloj o
  fechas corruptas exige revalidación; no intenta convertir el reloj local en
  una garantía criptográfica.
- 401 pasa a REQUIERE_SESION; 403 seguro bloquea el contexto sin revelar si la
  causa exacta fue revocación, rol o recurso. Red y fallo temporal de servicio
  se distinguen, no renuevan la ventana y no borran datos.
- El servicio permite bloquear todos los contextos de una cuenta al cerrar
  sesión o cambiar de usuario. La cola permanece aislada e intacta, incluidos
  `animalId` y `operationId`.

El contexto IndexedDB puede ser manipulado por quien controla el perfil del
navegador. Es una barrera local de la aplicación, no una autorización firmada.
Antes de sincronizar será obligatorio autenticar y consultar nuevamente sesión,
ganadería, membresía y rol en el backend; PostgreSQL sigue siendo la autoridad.

## Verificación real

| Verificación | Resultado |
|---|---|
| Pruebas dirigidas T11/T12/T15 | 21 aprobadas, 0 fallos. |
| Regresión frontend completa | 74 aprobadas en 11 archivos, 0 fallos. |
| TypeScript `tsc -b` | Correcto. |
| Build Vite | Correcto; 25 módulos transformados. |

Las pruebas cubren validación online, propietario/editor/lector, ausencia,
vencimiento, reloj hacia atrás, cuenta y ganadería distintas, reapertura,
bloqueo de cuenta, 401, rechazo 403, red frente a servidor, fallo IndexedDB,
ausencia de credenciales y conservación de la operación pendiente con sus IDs.

Se usó el doble IndexedDB inyectado existente; no se ejecutaron Chrome,
Playwright, varias pestañas reales ni desconexión física. Tampoco se ejecutó
backend porque no se modificó y `/acceso` conserva su contrato probado.

## Pendientes antes de habilitar registro offline

- Conectar preparación/bloqueo a login, logout, cambio de cuenta y selección de
  ganadería, mostrando los fallos de almacenamiento sin afirmar preparación.
- Integrar el formulario con provisional+operación en una transacción local.
- Implementar PWA/opt-in, orquestador, lease multipestaña, reintentos y recibos.
- Revalidar backend antes de cada envío y resolver 401/403/409/422/5xx sin
  eliminar pendientes ni marcar sincronizado sin confirmación PostgreSQL.
- Ejecutar PI04/EE03 en navegador real, incluyendo manipulación de reloj,
  recarga, revocación y cambio de cuenta.
