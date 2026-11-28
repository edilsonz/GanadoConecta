# Sustentación del Sprint 2 · GanadoConecta

**Duración estimada de 20 minutos.** Abre con la aplicación, explica una decisión
arquitectónica mediante código y cierra con los resultados y sus límites.
Prepara los servicios, la cuenta sintética y las pestañas antes de proyectar.
Los resultados de cierre corresponden a `193fd1a` y `67e6892`. La evidencia
remota citada corresponde a la base publicada `632c566`.

## Guion y texto de las 12 diapositivas

| Tiempo | Diapositiva y texto para proyectar | Qué decir o mostrar |
|---|---|---|
| 00:00–01:00 | **1. GanadoConecta.** «Tu ganadería, siempre conectada». Sprint 2 · Arquitectura implementada y calidad medida. | Abre con el logo y la portada. Explica que el productor registra animales, conserva historiales y autoriza los datos que llegan al catálogo. |
| 01:00–02:30 | **2. Actividad 1 · Estilo cliente-servidor.** React/PWA → API REST → PostgreSQL. | Usa C4 nivel 1 para señalar actores y límites. Describe HTTPS, los pendientes en el dispositivo y la confirmación del servidor. |
| 02:30–04:00 | **3. Actividad 2 · Monolito modular y Clean Architecture.** Spring Boot concentra el negocio. FastAPI ejecuta inferencia local. | Abre C4 niveles 2 y 3. Distingue monolito, IA, PostgreSQL e IndexedDB. Redis acelera exclusivamente el catálogo público. |
| 04:00–06:00 | **4. Actividad 3 · SDD.** Requisito → SPEC → criterio → prueba → evidencia. | Abre SPEC-09, sus tareas y la matriz. Relaciona venta, cierre de anuncios y preservación del historial con EC05 y su prueba de reversión. |
| 06:00–07:30 | **5. Actividad 4 · REST y Swagger.** 53 operaciones explícitas, 44 rutas, 13 módulos. | Muestra la ruta del catálogo y sus filtros o el contrato de CSRF. Las rutas privadas mantienen los controles de la API. |
| 07:30–09:00 | **6. Actividad 5 · Persistencia y caché.** PostgreSQL confirma. Redis sirve consultas públicas repetidas con TTL de 30 s. | Ejecuta las consultas preparadas. Explica el respaldo en PostgreSQL y la invalidación de anuncios tras confirmar cambios. |
| 09:00–11:00 | **7. Actividad 6 · SOLID aplicado.** El caso de uso depende de puertos. JDBC queda en el adaptador. | Abre `RegistrarVenta` y `UnidadDeTrabajoPostgres`. Sigue la actualización del estado y el cierre de anuncios en una sola transacción. |
| 11:00–12:30 | **8. Actividad 7 · Jest, cobertura e integración continua.** Jest 11/11. Tres trabajos y tres artefactos publicados. | Ejecuta Jest, abre el HTML y aclara que el 98,36 % de líneas de Jest corresponde a dos archivos. Muestra la corrida de `632c566`. |
| 12:30–14:30 | **9. Entregable funcional · PWA y asistente.** Alta, listado y ficha. IA de solo lectura con referencias a registros. | Abre la ganadería sintética y la ficha. Pregunta por dos pesajes al Qwen3 precargado. Contrasta los valores y las fechas. |
| 14:30–16:30 | **10. Actividad 8 · Rendimiento.** EC01 aprobado a 100 VU. 1.500 VU sostenidos durante 16 s. 2.000 VU alcanzados, tramo interrumpido. | Ejecuta únicamente el `smoke` de 2 VU y 5 s. Usa el gráfico guardado para mostrar la escalada. |
| 16:30–18:30 | **11. Actividad 8 · Diagnóstico y calidad.** Espera en Hikari y Tomcat. Ocho escenarios aprobados en el alcance probado y cinco parciales. | Explica por qué se conservó el pool de tres. Abre la matriz y señala qué falta en carga, integraciones, UX y conflictos versionados. |
| 18:30–20:00 | **12. Cierre · Cinco entregables.** Código, aplicación funcional, Swagger, pruebas y cobertura, atributos medidos. | Señala los respaldos de cada entregable. Cierra con siete actividades con evidencia y la octava parcial. |

### Cinco entregables

| Entregable | Demostración y respaldo |
|---|---|
| 1. Código fuente conforme a la arquitectura | Diapositivas 2–4 y 7. C4, Clean Architecture, SPEC-09 y `RegistrarVenta`. |
| 2. Primera versión funcional | Diapositivas 1 y 9. Portada, panel, ficha y asistente en la PWA con datos sintéticos. Capturas y resultados E2E de respaldo. |
| 3. API mediante Swagger | Diapositiva 5. Swagger de Spring Boot y contrato guardado. FastAPI se presenta con su contrato interno exportado. |
| 4. Jest y cobertura | Diapositiva 8. Jest, HTML de LCOV y artefactos de GitHub Actions. |
| 5. Atributos de calidad | Diapositivas 6, 10 y 11. Evidencia PostgreSQL, Redis, k6 y matriz EC01–EC13. Conserva cinco escenarios parciales. |

## Ensayo y demostración en PowerShell

En cada terminal empieza con `Set-Location D:\GanadoConecta`. Arranca
**T0 infraestructura → T1 Ollama → T2 FastAPI → T3 Spring → T4 PWA**.
Usa T0 para comprobaciones y reserva T5 para k6. La PWA usa la base
`ganadoconecta_e2e_sustentacion`. k6 usa `ganadoconecta_rendimiento_v4`
del mismo Compose aislado. Los puertos están limitados a loopback.

### T0 · Infraestructura y preparación previa

Comprueba JDK compatible, Maven, Node 22, dependencias de frontend y Docker
Desktop. Reutiliza el entorno Python del [manual de IA](../../servicio-ia/README.md).
Comprueba si existen los pares RSA y TLS de prueba en `.cache/sprint2-tls/`.
El certificado del ensayo vence el 1 de diciembre de 2026. Si faltan los
archivos o el certificado no cubre la fecha, prepara sus equivalentes según el
[README de HTTPS y claves](../../README.md#variables-de-entorno) y ajusta las
rutas de T3, T4 y Chrome. TLS requiere `127.0.0.1` en el SAN y vigencia hasta
el día de la exposición.

```powershell
Set-Location D:\GanadoConecta
docker desktop status
# Si está detenido, ejecutar docker desktop start y esperar al motor.
docker info --format '{{.ServerVersion}}'

$carpeta = (New-Item -ItemType Directory -Force .cache/rendimiento-sprint2).FullName
$archivo = Join-Path $carpeta 'postgres.txt'
if (-not (Test-Path $archivo)) {
  [IO.File]::WriteAllText($archivo, [Guid]::NewGuid().ToString('N'))
}
$env:GC_RENDIMIENTO_PASSWORD = [IO.File]::ReadAllText($archivo).Trim()
docker compose -f tests/rendimiento/compose.yaml up -d --wait
if ($LASTEXITCODE -ne 0) { throw 'Docker aislado no disponible' }
docker compose -f tests/rendimiento/compose.yaml ps

$base = 'ganadoconecta_e2e_sustentacion'
$existe = docker compose -f tests/rendimiento/compose.yaml exec -T postgres psql `
  -XAt -U rendimiento -d postgres -c "SELECT 1 FROM pg_database WHERE datname = '$base'"
if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL aislado no disponible' }
if ($existe -ne '1') {
  docker compose -f tests/rendimiento/compose.yaml exec -T postgres createdb -U rendimiento $base
  if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear la base de sustentación' }
}
if (-not (Test-Path .cache/rf19-secreto)) {
  node -p "require('node:crypto').randomBytes(32).toString('base64url')" |
    Set-Content -Encoding ascii -LiteralPath .cache/rf19-secreto
}
mvn.cmd -f backend/pom.xml -B -ntp -DskipTests package
if ($LASTEXITCODE -ne 0) { throw 'No se pudo compilar el backend' }
```

### T1 · Ollama local

```powershell
$env:OLLAMA_HOST = '127.0.0.1:11439'
$env:OLLAMA_NO_CLOUD = '1'
$env:OLLAMA_NUM_PARALLEL = '1'
$env:OLLAMA_MAX_LOADED_MODELS = '1'
$pesos = Join-Path $env:TEMP 'ganadoconecta-rf19-modelos-0edcdef34593'
$env:OLLAMA_MODELS = if (Test-Path -LiteralPath $pesos) {
  $pesos
} else {
  Join-Path $env:LOCALAPPDATA 'GanadoConecta/ollama-modelos'
}
& ./.cache/ollama-0.40.17/ollama.exe serve
```

En T0, confirma el modelo `0edcdef34593` y precárgalo antes de exponer.
Si falta, prepara la descarga durante el ensayo según el
[manual de IA](../../servicio-ia/README.md#terminal-1--ollama-local).

```powershell
$env:OLLAMA_HOST = '127.0.0.1:11439'
& ./.cache/ollama-0.40.17/ollama.exe list
$carga = @{
  model = 'qwen3:4b-instruct-2507-q4_K_M'
  keep_alive = '20m'
  options = @{ num_ctx = 2048; num_batch = 64 }
} | ConvertTo-Json -Depth 4
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:11439/api/generate `
  -ContentType 'application/json' -Body $carga -TimeoutSec 180 | Out-Null
& ./.cache/ollama-0.40.17/ollama.exe ps
```

### T2 · FastAPI

```powershell
$env:GANADOCONECTA_IA_SECRETO = (Get-Content .cache/rf19-secreto -Raw).Trim()
$env:GANADOCONECTA_OLLAMA_PUERTO = '11439'
Set-Location servicio-ia
& ../.cache/rf19-venv/Scripts/python.exe -m uvicorn ganadoconecta_ia.api:crear_app `
  --factory --host 127.0.0.1 --port 18081 --no-access-log --no-proxy-headers
```

### T3 · Spring Boot con PostgreSQL, Redis y Swagger

```powershell
$env:SPRING_CONFIG_IMPORT = 'optional:file:.cache/rendimiento-sprint2/config.properties'
$env:POSTGRES_DB = 'ganadoconecta_e2e_sustentacion'
$env:POSTGRES_USER = 'rendimiento'
$env:POSTGRES_PASSWORD = (Get-Content .cache/rendimiento-sprint2/postgres.txt -Raw).Trim()
$env:DB_HOST = '127.0.0.1'
$env:POSTGRES_PORT = '15433'
$env:SERVER_ADDRESS = '127.0.0.1'
$env:SERVER_PORT = '18082'
$env:GANADOCONECTA_ORIGEN_PERMITIDO = 'https://127.0.0.1:15173'
$env:GANADOCONECTA_JWT_PRIVATE_KEY_PATH = (Resolve-Path .cache/sprint2-tls/jwt-privada.pem).Path
$env:GANADOCONECTA_JWT_PUBLIC_KEY_PATH = (Resolve-Path .cache/sprint2-tls/jwt-publica.pem).Path
$env:GANADOCONECTA_FOTOS_DIRECTORIO = (New-Item -ItemType Directory -Force .cache/sustentacion-sprint2/fotos).FullName
$env:GANADOCONECTA_REPORTES_PRUEBA_AUTENTICADA = 'false'
$env:GANADOCONECTA_CACHE_CATALOGO = 'true'
$env:REDIS_HOST = '127.0.0.1'
$env:REDIS_PORT = '16379'
$env:REDIS_USER = ''
$env:REDIS_PASSWORD = ''
$env:REDIS_TLS = 'false'
$env:GANADOCONECTA_IA_ACTIVA = 'true'
$env:GANADOCONECTA_IA_PUERTO = '18081'
$env:GANADOCONECTA_IA_SECRETO = (Get-Content .cache/rf19-secreto -Raw).Trim()
java.exe -Xms128m -Xmx512m '-XX:ActiveProcessorCount=2' -jar backend/target/ganadoconecta-backend-0.1.0-SNAPSHOT.jar --spring.profiles.active=postgres,openapi --debug=false
```

Revisa que Flyway muestre la base `ganadoconecta_e2e_sustentacion`.
Desde T0 comprueba la salud y abre Swagger. Usa Swagger para leer el
contrato y la PWA para las operaciones privadas.

```powershell
Invoke-RestMethod http://127.0.0.1:18082/api/health
(Invoke-RestMethod http://127.0.0.1:18082/v3/api-docs).openapi
Start-Process 'http://127.0.0.1:18082/swagger-ui/index.html'
```

### T4 · Frontend HTTPS y PWA

```powershell
$env:DEV_HTTPS_CERT_PATH = (Resolve-Path .cache/sprint2-tls/local.pem).Path
$env:DEV_HTTPS_KEY_PATH = (Resolve-Path .cache/sprint2-tls/local.key).Path
$env:API_PROXY_TARGET = 'http://127.0.0.1:18082'
Set-Location frontend
npm.cmd run build
if ($LASTEXITCODE -ne 0) { throw 'No se pudo compilar la PWA' }
npm.cmd exec -- vite preview --host 127.0.0.1 --port 15173 --strictPort
```

Abre [https://127.0.0.1:15173/](https://127.0.0.1:15173/) en Chrome con
un perfil de prueba. Si el certificado del ensayo es autofirmado,
desde T0 abre ese perfil fijando la huella SPKI del certificado público
presentado por Vite. La configuración se limita al perfil de prueba y
conserva el origen HTTPS. En este equipo, `servido.pem` contiene el
certificado público que se comprobó en el puerto 15173. Si renuevas el
certificado, sustituye esa ruta por el nuevo certificado público que
presente tu preview, verificado con el navegador durante el ensayo.

```powershell
$chrome = 'C:/Program Files/Google/Chrome/Application/chrome.exe'
$cert = (Resolve-Path .cache/sprint2-tls/servido.pem).Path
$huella = node -e "const c=require('node:crypto');const x=new c.X509Certificate(require('node:fs').readFileSync(process.argv[1]));if(!x.checkIP('127.0.0.1')||Date.parse(x.validTo)<=Date.now())process.exit(1);console.log(c.createHash('sha256').update(x.publicKey.export({type:'spki',format:'der'})).digest('base64'))" $cert
if ($LASTEXITCODE -ne 0 -or -not $huella) { throw 'Revisa el certificado HTTPS local' }
$perfil = (New-Item -ItemType Directory -Force .cache/sustentacion-sprint2/chrome).FullName
Start-Process -FilePath $chrome -ArgumentList @(
  "--user-data-dir=`"$perfil`"", "--ignore-certificate-errors-spki-list=$huella",
  'https://127.0.0.1:15173/'
)
```

Comprueba `/api/health` y, en **Application**, el manifest y el Service
Worker. Antes de proyectar, crea una cuenta con correo terminado en
`@example.invalid`, una ganadería «Sustentación sintética» y un «Animal
demo». Registra pesajes de 120 kg y 135 kg en fechas distintas del
período que vas a consultar en **Análisis y alertas**. Inicia sesión antes
de compartir pantalla.

En vivo, abre **Mi ganado → Animal demo → Peso** y luego **Análisis y
alertas → Asistente → Peso**. Pregunta «¿Cuánto cambió el peso entre los
últimos pesajes?». Contrasta los dos registros, las fechas y los +15 kg.
Si falla el asistente, muestra la evidencia RF19 y el cálculo determinista
en **Reportes**.

### T0 · Código, Jest, cobertura e integración continua

```powershell
code.cmd --reuse-window backend/src/main/java/pe/ganadoconecta/modulos/ganado/aplicacion/RegistrarVenta.java `
  backend/src/main/java/pe/ganadoconecta/modulos/ganado/adaptadores/salida/UnidadDeTrabajoPostgres.java

Push-Location frontend
npm.cmd run test:jest:coverage
if ($LASTEXITCODE -eq 0) {
  Start-Process (Resolve-Path coverage/jest/lcov-report/index.html).Path
}
Pop-Location

Start-Process 'https://github.com/edilsonz/GanadoConecta/actions/runs/46106485585'
```

En Actions muestra los trabajos `frontend`, `backend` e `ia` y los
artefactos `cobertura-frontend`, `pruebas-cobertura-java` y
`cobertura-openapi-python`. La corrida terminó en 1 min 44 s.
[Cobertura por herramienta](cobertura.json)

### T0 · PostgreSQL y Redis

```powershell
docker compose -f tests/rendimiento/compose.yaml exec -T postgres psql -X -U rendimiento `
  -d ganadoconecta_e2e_sustentacion -c 'SELECT current_database(), current_setting(''server_version''), count(*) AS animales FROM ganado.animal'
docker compose -f tests/rendimiento/compose.yaml exec -T postgres psql -X -U rendimiento `
  -d ganadoconecta_e2e_sustentacion -c 'SELECT version, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 3'
docker compose -f tests/rendimiento/compose.yaml exec -T redis redis-cli PING
docker compose -f tests/rendimiento/compose.yaml exec -T redis redis-cli INFO stats |
  Select-String '^keyspace_(hits|misses):'
1..2 | ForEach-Object {
  $null = Invoke-RestMethod 'http://127.0.0.1:18082/api/v1/catalogo?pagina=1'
}
docker compose -f tests/rendimiento/compose.yaml exec -T redis redis-cli INFO stats |
  Select-String '^keyspace_(hits|misses):'
```

Compara el antes y el después. Los contadores son acumulados y el TTL
puede cambiar la lectura entre ensayos. Proyecta solo conteos.

### T5 · k6 seguro y breve

Durante el ensayo, comprueba que `ganadoconecta_rendimiento_v4` contiene
su semilla. Si está vacía, usa [Preparar](../../tests/rendimiento/README.md#preparar)
antes de exponer. Este backend usa el par RSA de
`.cache/rendimiento-sprint2/`. Después de mostrar el asistente, libera el
modelo y detén T4, T3, T2 y T1 con Ctrl+C antes de cargar k6. Así ambos
backends usan Redis en momentos separados. El monitor exige al menos
1.536 MiB libres y vuelve a comprobarlos durante la ejecución.

```powershell
$env:OLLAMA_HOST = '127.0.0.1:11439'
& ./.cache/ollama-0.40.17/ollama.exe stop qwen3:4b-instruct-2507-q4_K_M
Get-CimInstance Win32_PerfFormattedData_PerfOS_Memory |
  Select-Object AvailableMBytes, PercentCommittedBytesInUse
```

En T5, arranca el backend del arnés. Flyway debe mostrar
`ganadoconecta_rendimiento_v4` y la salud responder en 18083.

```powershell
./tests/rendimiento/backend.ps1 -Cache
```

En T0, renueva las sesiones sintéticas inmediatamente antes del smoke.
Si falla una comprobación del entorno, abre el resultado guardado.

```powershell
Invoke-RestMethod http://127.0.0.1:18083/api/health
node tests/rendimiento/preparar.mjs 8
if ($LASTEXITCODE -ne 0) { throw 'No hay sesiones sintéticas válidas para la prueba' }
$k6 = (Resolve-Path '.cache/rendimiento-sprint2/k6-v2.4.0-windows-amd64/k6.exe').Path
$nombre = 'sustentacion-' + (Get-Date -Format 'yyyyMMdd-HHmmss')
./tests/rendimiento/medir.ps1 -K6 $k6 -Modo smoke -Nombre $nombre
if ($LASTEXITCODE -ne 0) { throw 'El smoke falló. Consulta el JSON guardado.' }
$r = (Get-Content ".cache/rendimiento-sprint2/$nombre.json" -Raw | ConvertFrom-Json).metrics
[pscustomobject]@{
  Solicitudes = $r.http_reqs.values.count
  p95_ms = $r.http_req_duration.values.'p(95)'
  ErrorHTTP = $r.http_req_failed.values.rate
  Comprobaciones = $r.checks.values.rate
}
```

El smoke comprueba el listado privado y el catálogo público a 2 VU
durante 5 s. EC01 y la escalada se explican con el registro del cierre.
Al terminar, detén T5 a T1 con Ctrl+C y ejecuta en T0
`docker compose -f tests/rendimiento/compose.yaml stop`. Se conservan
los contenedores del ensayo y sus datos.

## Índice de recursos para el PowerPoint

Los enlaces apuntan a los originales. Importa los PNG y SVG directamente.
Para Jest y GitHub utiliza las vistas en vivo o los resultados guardados
como tabla. El HTML de Jest se genera en local.

| Diapositiva | Recurso recomendado y respaldo |
|---|---|
| 1 | [Logo oficial · ganadoconecta-logo.png](../../img/marca/ganadoconecta-logo.png) y [portada 1440 · despues-portada-1440.png](../../tests/e2e/evidencia/rediseno-2026-11-21/despues-portada-1440.png). |
| 2 | [C4 nivel 1 · nivel-1-contexto.svg](../04-modelo-c4/diagramas/nivel-1-contexto.svg). |
| 3 | [C4 nivel 2 · nivel-2-contenedores.svg](../04-modelo-c4/diagramas/nivel-2-contenedores.svg), [nivel 3 · nivel-3-componentes.svg](../04-modelo-c4/diagramas/nivel-3-componentes.svg), [despliegue local](../02-arquitectura-software/diagramas/despliegue-local.svg) y [fuente Draw.io C4](../04-modelo-c4/diagramas/ganadoconecta-c4.drawio). |
| 4 | [SPEC-09](../../specs/SPEC-09-ventas-moderacion/spec.md), [tareas de SPEC-09](../../specs/SPEC-09-ventas-moderacion/tasks.md) y [matriz de trazabilidad SDD](../06-evaluacion-sdd/matriz-trazabilidad.md). |
| 5 | [Swagger por módulos · swagger-modulos.png](openapi/swagger-modulos.png), [CSRF · swagger-csrf.png](openapi/swagger-csrf.png), [contrato Spring](openapi/spring.json) y [contrato interno FastAPI](openapi/fastapi-interno.json). |
| 6 | [PostgreSQL · postgres-tiempos.svg](rendimiento/postgres-tiempos.svg), [planes SQL](rendimiento/postgres-planes.json), [continuidad con Redis detenido](rendimiento/cierre/continuidad-redis.json) y [estadísticas Redis del tramo poblado](rendimiento/cierre/cierre-instrumentado-poblada-redis-despues.txt). |
| 7 | [Clean Architecture · clean-architecture.svg](../02-arquitectura-software/diagramas/clean-architecture.svg), [fuente Draw.io técnica](../02-arquitectura-software/diagramas/ganadoconecta-tecnico.drawio) y [código de RegistrarVenta](../../backend/src/main/java/pe/ganadoconecta/modulos/ganado/aplicacion/RegistrarVenta.java). |
| 8 | [cobertura.json](cobertura.json), [ejecución #46106485585](https://github.com/edilsonz/GanadoConecta/actions/runs/46106485585) y `frontend/coverage/jest/lcov-report/index.html` tras generar el informe. |
| 9 | [Dashboard 1440 · despues-panel-1440.png](../../tests/e2e/evidencia/rediseno-2026-11-21/despues-panel-1440.png), [ficha real · ux-1440-ficha.png](rendimiento/cierre/ux-1440-ficha.png), [ficha móvil · ficha-360.png](../../tests/e2e/evidencia/rediseno-2026-11-21/ficha-360.png), [asistente móvil · asistente-360.png](../../tests/e2e/evidencia/auditoria-2026-11-21/asistente-360.png) y [resultados de inferencia RF19](../../specs/SPEC-12-analisis-alertas-asistente/tasks.md#fase-2--validación-local-de-rf19). |
| 10 | [EC01 · k6-comparacion.svg](rendimiento/k6-comparacion.svg) para la comparación del segundo incremento, [escalada final · comparacion-escalada.svg](rendimiento/cierre/comparacion-escalada.svg), [resultados EC01 del cierre instrumentado](rendimiento/cierre/cierre-instrumentado-resumen.json) y [smoke guardado](rendimiento/k6-smoke-v4.json). |
| 11 | [Pool a 500 VU · comparacion-pool.svg](rendimiento/cierre/comparacion-pool.svg), [matriz EC01–EC13](matriz-calidad.md), [UX móvil](rendimiento/cierre/ux-360-catalogo.png) y [revocación offline](rendimiento/cierre/chrome-revocacion.json). |
| 12 | [Informe del Sprint 2](README.md), [ruta SDD y rúbrica](../06-evaluacion-sdd/ruta-sdd-rubrica.md) y [auditoría funcional](../AUDITORIA-FUNCIONAL.md). |

## Preguntas del docente y resultados

- **¿EC01 se cumplió?** Sí. A 100 VU, p95 global de 26,63 ms con Redis
  frío y 30,97 ms poblado en el cierre instrumentado. Los p95 de animales
  y catálogo fueron 26,32 y 42,85 ms en frío y 32,80 y 25,29 ms con caché
  poblada. El límite era de 2 s por consulta. Hubo cero errores y 100 %
  de comprobaciones. La mezcla excluye inferencia y transferencia de
  fotografías. [Datos](rendimiento/cierre/cierre-instrumentado-resumen.json)
- **¿Qué significa alcanzar 2.000 VU?** El generador llegó a 2.000 VU
  activos. El tramo se interrumpió tras unos 7 s, con p95 de 5,01 s y
  110 errores en 114 solicitudes. Se sostuvieron 1.500 VU durante 16 s,
  con p95 de 3,46 s. VU incluye espera y pausas del guion.
  [Escalada](rendimiento/cierre/cierre-resumen.json)
- **¿Dónde se observó el cuello de botella y por qué conservaron tres
  conexiones?** A 500 VU se midieron 200/200 hilos Tomcat ocupados, 3/3
  conexiones Hikari activas y hasta 197 esperas de conexión. Cinco
  conexiones empeoraron el p95 de 1.947 a 2.294 ms y el caudal de 257,36
  a 251,76 solicitudes/s en condiciones equivalentes. Falta medir la
  retención de conexiones por operación y separar k6 en otro equipo.
  [Diagnóstico](rendimiento/cierre/README.md)
- **¿Por qué usar Redis?** Guarda temporalmente las proyecciones públicas.
  Si cae, el catálogo consulta PostgreSQL. Una revisión transaccional
  mantiene vigentes las lecturas tras publicar, pausar o vender.
  [Evidencia de Redis](rendimiento/redis.md)
- **¿Cómo se preservan privacidad e integridad?** Spring revalida sesión
  y membresía. El catálogo proyecta los campos autorizados. La venta
  actualiza el animal y cierra sus anuncios dentro de una unidad de
  trabajo. Si falla el cierre, ambas operaciones se revierten y el
  historial se conserva. [SPEC-09](../../specs/SPEC-09-ventas-moderacion/spec.md)
- **¿Qué pasa con un alta offline si revocan al editor?** En Chrome,
  el botón avisó del cambio de permiso e IndexedDB conservó el payload.
  Un reenvío directo recibió HTTP 403. Queda pendiente la edición
  versionada. [Evidencia EC12](rendimiento/cierre/chrome-revocacion.json)
- **¿Cómo se controla la respuesta de la IA?** Spring delimita el ámbito
  y prepara hechos mínimos. FastAPI selecciona referencias y Spring
  verifica que pertenezcan a ese conjunto. La aplicación informa si
  faltan datos o si el modelo está indisponible. Continúa disponible el
  análisis determinista. La IA de registros propios se probó en español.
  El corpus para orientación general sigue pendiente.
  [SPEC-12](../../specs/SPEC-12-analisis-alertas-asistente/spec.md) · [Pruebas RF19](../../specs/SPEC-12-analisis-alertas-asistente/tasks.md#fase-2--validación-local-de-rf19)
- **¿Cuánta cobertura se obtuvo?** Jest midió 60/61 líneas y 75/82 ramas
  en dos archivos, con 11 pruebas aprobadas. Vitest, JaCoCo y Python tienen
  alcances propios. [Cobertura por herramienta](cobertura.json)
- **¿Está completa la rúbrica?** Siete actividades tienen evidencia y la
  octava es parcial. Los ocho escenarios aprobados en el alcance probado
  son EC01, EC04, EC05, EC06, EC09, EC10, EC11 y EC13. Los parciales son
  EC02 y EC07 por el proveedor externo de notificaciones, EC03 por
  capacidad sostenida, EC08 por dispositivos físicos y accesibilidad, y
  EC12 por edición versionada. [Matriz de calidad](matriz-calidad.md)

## Lista previa y plan alternativo

- [ ] Revisar vigencia del certificado, rutas, puertos libres y memoria.
- [ ] Ver ambos contenedores `healthy`, Flyway en la base sintética y
  `UP` en Spring. Abrir Swagger y PWA. Verificar manifest y Service Worker.
- [ ] Ensayar con perfil y cuenta sintéticos. Confirmar animal, dos
  pesajes y período. Cerrar vistas que puedan mostrar datos privados.
- [ ] Precargar Qwen3. Consultar `ollama ps` y probar una pregunta antes
  de exponer. Abrir el reporte de Jest y la corrida de GitHub.
- [ ] Confirmar el smoke. Dejar en el PowerPoint los gráficos de EC01 y
  escalada ya medidos. Evitar proyectar credenciales, cookies o variables.
- [ ] Llevar una copia local de la presentación y abrir los recursos
  del índice con antelación. Conservar los datos PostgreSQL e IndexedDB.

| Si falla | Qué mostrar para seguir |
|---|---|
| Internet o GitHub | El informe del Sprint 2, `cobertura.json` y el HTML local ya generado. Identifica la ejecución y sus artefactos. Continúa con la demo local si Docker, dependencias y pesos ya están disponibles. |
| Docker o memoria | Portada, panel y ficha del índice, resultados Chrome y gráficos k6. Abre los planes SQL y los resultados guardados de Redis. |
| Swagger | Capturas de los módulos y CSRF, contrato Spring JSON y contrato interno exportado de FastAPI. |
| Ollama o FastAPI | Captura del asistente y prueba RF19 guardada. Si la PWA está disponible, muestra los registros y el cálculo determinista en **Peso** o **Reportes**. |
| HTTPS o PWA | Capturas sintéticas y resultados Chrome. Retoma los problemas de certificado después de la exposición. |
| k6 o sus comprobaciones iniciales | `k6-smoke-v4.json`, resumen EC01 y gráfico de escalada. Preséntalos como resultados guardados. |

### Comprobación de los comandos de esta guía

El 28 de noviembre de 2026 se comprobaron los contenedores aislados,
Flyway hasta V19, Swagger HTTP 200, 44 rutas OpenAPI y el proxy HTTPS
en Chrome con manifest y Service Worker registrado. La consulta
repetida al catálogo produjo un fallo de caché seguido de un acierto
en Redis. Se precargó el Qwen3 existente y se arrancó FastAPI. La
conversación completa se respalda con la evidencia RF19 previa.
Jest aprobó 11/11 y generó el informe HTML. Se verificaron los tres
trabajos y artefactos publicados en GitHub.

El monitor bloqueó el smoke con los servicios abiertos y detuvo el
intento posterior por memoria durante la ejecución. Para esta estación,
deja abierto [el smoke aprobado anterior](rendimiento/k6-smoke-v4.json).
Las cifras de EC01 y la escalada proceden de las corridas registradas
en el cierre del Sprint 2.
