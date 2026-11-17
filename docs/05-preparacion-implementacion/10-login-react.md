# AU02–AU04 — Cliente React de autenticación

Fecha: 17 de noviembre de 2026, America/Lima. Estado: **cliente automatizado y
flujo principal validado manualmente bajo HTTPS; AU05–AU08 pendientes**.

## Alcance realizado

- Formulario responsivo de inicio de sesión con correo, contraseña, estados de
  carga y errores genéricos de credenciales, límite, seguridad, servicio y red.
- Integración con `POST /api/v1/auth/login`, obteniendo primero CSRF y enviando
  la cabecera `X-CSRF-TOKEN` con solicitudes de mismo origen.
- JWT de acceso conservado únicamente en estado React. No se usa
  `localStorage`, `sessionStorage` ni IndexedDB para contraseñas o tokens.
- Cookie `__Host-GC-Refresh` delegada al navegador: JavaScript no intenta leerla,
  copiarla ni incluirla en JSON.
- Restauración al cargar mediante renovación controlada y posterior
  `GET /api/v1/auth/me`; renovación programada antes del vencimiento del JWT.
- Una sola renovación simultánea dentro de la pestaña y coordinación mediante
  Web Locks cuando el navegador la ofrece. La defensa definitiva contra carreras
  continúa en el backend.
- Una respuesta de renovación incierta no se reintenta automáticamente.
- Cierre local inmediato y `POST /api/v1/auth/logout`. Si falla la confirmación
  remota, se conserva solo la marca no secreta
  `ganadoconecta:salida-remota-pendiente`; se procesa antes de restaurar sesión.
- Vista autenticada mínima con el correo disponible y botón de salida. No crea
  ganadería, membresía, rol ni sesión ficticia.
- Vite puede servir HTTPS con certificado y clave externos mediante
  `DEV_HTTPS_CERT_PATH` y `DEV_HTTPS_KEY_PATH`. Ningún certificado, clave RSA ni
  secreto se incluye en el repositorio.

## Pruebas ejecutadas

| Comando / conjunto | Resultado real |
|---|---|
| Pruebas dirigidas de `sesionApi` y `AccesoCuenta` | 15 pruebas aprobadas: login, ausencia normal de sesión, contrato `/me` sin correo, errores/429, reobtención CSRF, no persistencia, restauración, renovación compartida, logout y fallo remoto. |
| `npm.cmd test -- --reporter=dot` | 4 archivos, 26 pruebas aprobadas, 0 fallos. |
| `RegistroUsuarioCsrfPostgresIntegrationTest` | 1 prueba aprobada con origen HTTPS, CSRF y PostgreSQL real. |
| Dos métodos dirigidos de `AutenticacionHttpPostgresIntegrationTest` | 2 pruebas aprobadas: atributos seguros de renovación, `/me`, logout e invalidación del JWT previo. |
| `npm.cmd run typecheck` | TypeScript correcto. |
| `npm.cmd run build` | Build Vite correcto; 21 módulos transformados. |

El backend no cambió. Por la regla de validación optimizada no se repitieron las
54 pruebas backend/PostgreSQL ya aprobadas en el incremento anterior. Esta
omisión no convierte las pruebas frontend en una integración real con la base.

## Trazabilidad

| Elemento | Evidencia de este incremento |
|---|---|
| AU02 / T02.2 / PA02 | Formulario, login real por contrato, errores seguros y prevención de doble envío cubiertos con API simulada en pruebas de cliente. |
| AU03 / T02.3 / PA03 | JWT en memoria, restauración, renovación previa al vencimiento, single-flight y Web Locks implementados; multipestaña real pendiente. |
| AU04 / T02.3 / PA04 | Logout y bloqueo local implementados; la marca persistida no contiene credenciales. Cola offline aún inexistente. |
| AU08 / PA07 | CSRF enviado, refresh HttpOnly no leído y HTTPS local configurable sin material privado versionado. Navegador HTTPS real pendiente. |
| CA03 / CA12 / CA14 | Cobertura parcial; faltan ganadería, membresías, offline y E2E real. |

## Validación manual HTTPS

Se observaron realmente en Chrome: registro de cuenta correcto, login desde
React correcto, recuperación de la sesión después de F5 y logout seguido de F5
sin restaurar la sesión. Tras restaurar, la interfaz mostró «Cuenta verificada»:
es coherente con el contrato aprobado de `/me`, que solo devuelve id y estado.

No se inspeccionaron manualmente el código HTTP 204 ni los atributos de cookies
en DevTools. Esos aspectos conservan cobertura automatizada específica de backend;
no se presentan como evidencia visual.

## Validaciones no ejecutadas

- No se probó coordinación efectiva entre dos pestañas o ventanas reales.
- No se ejecutó Playwright ni otro E2E de navegador.
- No se probó cola offline porque aún no está implementada.

La revisión manual utilizó HTTPS, claves RSA externas, perfil `postgres` y una
cuenta sintética. No se declara completo T02 ni el criterio CA03.
