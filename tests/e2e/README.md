# Recorridos HTTPS en Chrome

El arnés usa Node.js 22 y Chrome headless mediante DevTools. El navegador crea
la cuenta, ganadería y animales sintéticos. La prueba de paginación usa la
API privada para añadir 20 anuncios sintéticos al anuncio creado en pantalla.
Para probar el reintento de campaña se reenvían su ruta y cuerpo originales con
un CSRF vigente. Cada corrida usa un perfil temporal de Chrome.

## Ejecutar

Usa PostgreSQL Docker en estado saludable y las rutas externas de RSA y HTTPS
descritas en el [README principal](../../README.md). Prepara un directorio
privado vacío para las fotografías sintéticas. Los puertos 18082 y 15173
están reservados para este ejemplo de ejecución aislada.

En PowerShell, desde la raíz, crea la base dedicada si todavía no existe.

```powershell
$rolPostgres = (Get-Content .env | Where-Object { $_ -cmatch '^POSTGRES_USER=' } |
  Select-Object -First 1) -replace '^POSTGRES_USER=', ''
docker compose exec -T postgres createdb -U $rolPostgres ganadoconecta_e2e_local
```

Arranca Spring Boot en una terminal desde la raíz. Configura las rutas privadas
de esta prueba en el entorno de ese proceso.

```powershell
$env:POSTGRES_DB = 'ganadoconecta_e2e_local'
$env:SERVER_ADDRESS = '127.0.0.1'
$env:SERVER_PORT = '18082'
$env:GANADOCONECTA_ORIGEN_PERMITIDO = 'https://127.0.0.1:15173'
$env:GANADOCONECTA_JWT_PRIVATE_KEY_PATH = 'C:/ruta-temporal/jwt-privada.pem'
$env:GANADOCONECTA_JWT_PUBLIC_KEY_PATH = 'C:/ruta-temporal/jwt-publica.pem'
$env:GANADOCONECTA_FOTOS_DIRECTORIO = 'C:/ruta-temporal/fotos-e2e'
$env:GANADOCONECTA_REPORTES_PRUEBA_AUTENTICADA = 'false'
Set-Location backend
mvn.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

Antes de lanzar Chrome, confirma en el arranque de Flyway que la base conectada
es `ganadoconecta_e2e_local`. El valor de `GC_E2E_DB` documenta el destino de
la prueba. Esta comprobación del servidor verifica la conexión efectiva.

Sirve la PWA compilada desde otra terminal abierta en la raíz.

```powershell
$env:DEV_HTTPS_CERT_PATH = 'C:/ruta-temporal/tls.pem'
$env:DEV_HTTPS_KEY_PATH = 'C:/ruta-temporal/tls.key'
$env:API_PROXY_TARGET = 'http://127.0.0.1:18082'
Set-Location frontend
npm.cmd run build
npm.cmd exec -- vite preview --host 127.0.0.1 --port 15173 --strictPort
```

Ejecuta los recorridos desde la raíz en una tercera terminal. Indica la ruta
del certificado público presentado por el puerto HTTPS. Chrome fija su clave
pública en el perfil temporal de pruebas.

```powershell
$env:GC_E2E_DB = 'ganadoconecta_e2e_local'
$env:GC_E2E_CERT = 'C:/ruta-temporal/certificado-publico.pem'
node tests/e2e/recorridos.mjs
```

Opcionalmente agrega el argumento `07`, `08`, `09` o `ux` para un solo
recorrido, o configura `GC_E2E_CHROME` si Chrome tiene otra ruta. El origen
aislado se puede cambiar con `GC_E2E_URL`. Las capturas y `resultado.json`
se guardan en `.cache/e2e/<id>/`, ruta ignorada por Git.

Para la auditoría de los flujos privados, con el mismo entorno aislado:

```powershell
node tests/e2e/auditoria-productor.mjs
```

El arnés crea cuentas de prueba y una ganadería por ejecución. Comprueba acceso,
invitaciones, roles, alta individual y por lote, campaña de desparasitación,
reproducción, pesajes, avisos de agenda y la cola nativa de IndexedDB. Simula la
desconexión mediante Chrome DevTools, reenvía el mismo registro a la API real y
vuelve a consultar tras la recarga. Usa `--familia` o `--alertas` para repetir el
caso indicado y su preparación. Guarda resultados y capturas en
`.cache/e2e-auditoria/<id>/`. Si ejecutas varios recorridos, usa el mismo directorio
privado previamente creado para las fotografías de prueba.
[Informe de auditoría](../../docs/AUDITORIA-FUNCIONAL.md) ·
[Resultados E2E](evidencia/auditoria-2026-11-21/resultado.json).

## Evidencia del 21 de noviembre de 2026

**4/4 recorridos aprobados, 0 fallidos.** Chrome headless 158 y Node.js 22.19.0
ejecutaron la PWA compilada en `https://127.0.0.1:15173` contra Spring Boot
3.5.16 y una base nueva PostgreSQL 17.12 con Flyway V1–V16.

La selección versionada contiene solo pantallas con datos sintéticos y un
resumen del resultado sin identificadores locales del entorno.
[Resultado](evidencia/resultado.json) · [Campaña](evidencia/campana-parcial.png) ·
[Catálogo](evidencia/catalogo-pagina-2.png) ·
[Anuncios cerrados](evidencia/anuncios-cerrados.png) ·
[Ficha móvil](evidencia/ficha-movil.png).

| Recorrido | Resultado observado |
|---|---|
| SPEC-07 | Cierre parcial de campaña, persistencia tras recarga y repetición HTTP 200. PostgreSQL confirmó un evento vinculado a PRUEBA-LOTE-01 y cero a PRUEBA-LOTE-02. |
| SPEC-08 | Publicación con foto sintética, catálogo anónimo, filtros de ubicación y sexo, navegación entre dos páginas y salida tras pausar. La proyección omitió datos privados y el historial anónimo devolvió HTTP 401. |
| SPEC-09 | Venta sintética, dos anuncios cerrados y ausentes del catálogo, con historial privado conservado. Recepción de reportes desactivada y cero cuentas con permiso de moderación activo en la base. |
| UX/UI | Escritorio de 1440 px, tablet de 768 px y móvil de 360 px. Se recorrieron el menú, los campos principales y las seis pestañas. Se verificaron ancho, clics, foco por teclado y asociación ARIA de las pestañas. |

El certificado del puerto fue presentado por AVG en este entorno y su clave
pública se fijó en el perfil aislado. Las primeras corridas detectaron la
necesidad de renovar el CSRF en el arnés y de medir el ancho útil con la barra
vertical. Quedan pendientes moderación con cuenta autorizada, accesibilidad
con lector de pantalla, dispositivos físicos y E2E offline.
