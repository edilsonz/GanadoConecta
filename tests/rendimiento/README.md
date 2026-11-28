# Rendimiento aislado de GanadoConecta

El arnés usa solo `http://127.0.0.1:18083`. Compose crea el proyecto
`ganadoconecta-rendimiento` con PostgreSQL en 15433 y Redis en 16379.
La base objetivo es `ganadoconecta_rendimiento_v4`. Los scripts generan
sesiones de cuentas sintéticas y conservan los archivos privados en
`.cache/rendimiento-sprint2/` fuera de Git. Usar un puerto libre y una
estación sin otras cargas intensivas.

## Preparar

Desde la raíz en PowerShell, con Docker Desktop, Java 21 o más reciente,
Maven y Node 22.19.0:

```powershell
$carpeta = (New-Item -ItemType Directory -Force .cache/rendimiento-sprint2).FullName
$archivo = Join-Path $carpeta 'postgres.txt'
if (-not (Test-Path $archivo)) {
  [IO.File]::WriteAllText($archivo, [Guid]::NewGuid().ToString('N'))
}
$env:GC_RENDIMIENTO_PASSWORD = [IO.File]::ReadAllText($archivo)
# Crear el par RSA de esta prueba una sola vez con openssl.
openssl genpkey -quiet -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$carpeta/jwt-privada.pem"
openssl pkey -in "$carpeta/jwt-privada.pem" -pubout -out "$carpeta/jwt-publica.pem"
docker compose -f tests/rendimiento/compose.yaml up -d --wait
mvn.cmd -f backend/pom.xml -B -ntp -DskipTests package
```

En otra terminal en la raíz, ejecutar
`./tests/rendimiento/backend.ps1` sin argumentos para la línea base o con
`-Cache` para habilitar Redis. Usar el mismo jar en ambas mediciones.
El backend limita la JVM a 4 CPU lógicas, heap inicial de 256 MiB y máximo
de 768 MiB. El adaptador de PostgreSQL fija Hikari a 3 conexiones
y un plazo de adquisición de 3.000 ms.

```powershell
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$OutputEncoding = [Text.UTF8Encoding]::new($false)
node tests/rendimiento/preparar.mjs 8
Get-Content -Encoding utf8 -Raw tests/rendimiento/semilla.sql |
  docker compose -f tests/rendimiento/compose.yaml exec -T postgres psql -q -U rendimiento -d ganadoconecta_rendimiento_v4 -v animales_por_ganaderia=3000 -v anuncios_por_ganaderia=1500 -v eventos_por_animal=4
node tests/rendimiento/planes.mjs |
  docker compose -f tests/rendimiento/compose.yaml exec -T postgres psql -q -U rendimiento -d ganadoconecta_rendimiento_v4
docker compose -f tests/rendimiento/compose.yaml exec -T postgres psql -q -U rendimiento -d ganadoconecta_rendimiento_v4 -c "CREATE EXTENSION IF NOT EXISTS pg_stat_statements"
```

Para repetir la comparación SQL se usa `planes.mjs ea72e80` para el código
base y `planes.mjs` para el actual. Alternar tres pares en la misma base,
guardar como `sql-base-1.txt` a `sql-base-3.txt` y `sql-opt-1.txt` a
`sql-opt-3.txt` bajo `.cache/rendimiento-sprint2/` y ejecutar
`node tests/rendimiento/resumir-planes.mjs`.

## Protocolo de carga

Instalar o descargar el binario oficial k6 2.4.0 y verificar su SHA-256.
Fijar `$k6` a la ruta del ejecutable. Regenerar las sesiones con
`node tests/rendimiento/preparar.mjs 8` antes de medir. Los JWT duran
10 minutos. La mezcla es 50 % listado privado con cursor y 50 % catálogo
público, con cuatro combinaciones de filtros y tres páginas. Cada VU
espera entre 0,85 y 1,45 segundos tras la respuesta. La prueba comprueba
estado, cantidad, pertenencia, unicidad, filtros y ausencia de campos
privados. Excluye archivos de fotos e inferencia.

```powershell
./tests/rendimiento/medir.ps1 -K6 $k6 -Modo smoke -Nombre smoke-base
./tests/rendimiento/medir.ps1 -K6 $k6 -Modo calentar -Nombre calentamiento-base
./tests/rendimiento/medir.ps1 -K6 $k6 -Modo cien -Nombre cien-base
```

El smoke usa dos VU durante cinco segundos. El calentamiento usa 20 VU
durante 15 segundos y el tramo de EC01 usa 100 VU durante 25 segundos.
Para cada caso con Redis, calentar el backend antes de medir. Para la
caché fría, vaciar exclusivamente Redis del Compose de rendimiento justo
antes de medir. Para la poblada, volver a llenar las 12 claves con las
combinaciones del arnés mediante `node tests/rendimiento/poblar-cache.mjs`
justo antes de medir y confirmar con `DBSIZE`.
Registrar `INFO stats` antes y después. El TTL predeterminado es 30 s.
Medir la caída de Redis en un tramo independiente tras detener solo el
servicio Redis del Compose de rendimiento.

```powershell
./tests/rendimiento/redis-ciclo.ps1
./tests/rendimiento/medir.ps1 -K6 $k6 -Modo progresiva -Nombre progresiva
```

La progresiva sube en cinco segundos a cada nivel y mantiene 16 segundos
en 10, 50, 100, 250, 500, 1.000, 1.500 y 2.000 VU. EC01 conserva
p95 ≤ 2 s por tipo de consulta con 100 VU. Como reglas operativas de esta
estación, k6 para ante 2 % de errores HTTP o comprobaciones fallidas y
p95 ≥ 4 s en un nivel sostenido de la progresiva. El monitor impide
iniciar con menos de 1.536 MiB libres o memoria comprometida ≥ 95 %.
También detiene por memoria durante la ejecución o tras cuatro muestras
consecutivas de saturación de CPU de Java, PostgreSQL o k6. Los límites de
memoria por proceso son 1.500 MiB para Java y 1.800 MiB para k6.

La réplica local registra percentiles por nombre de operación y nivel,
solicitudes por segundo, errores, tamaño, VU activos, CPU, RSS y conexiones.
Los VU son sesiones activas en el generador. Incluyen el tiempo de pensar y
la espera de red, por lo que no equivalen a personas ni a solicitudes en
vuelo simultáneamente.

Los informes resumidos y los CSV se guardan en `.cache/rendimiento-sprint2/`.
Con los nombres de ejecución indicados se pueden consolidar en el informe
mediante `node tests/rendimiento/resumir-carga.mjs`.
Para consultar estadísticas SQL acumuladas después de la carga:

```powershell
Get-Content -Encoding utf8 -Raw tests/rendimiento/sentencias.sql |
  docker compose -f tests/rendimiento/compose.yaml exec -T postgres psql -q --csv -U rendimiento -d ganadoconecta_rendimiento_v4
```

El [informe académico](../../docs/07-sprint-2/README.md) enlaza la evidencia
seleccionada. Para cerrar, detener solo el proceso Spring de esta prueba y
usar `docker compose -f tests/rendimiento/compose.yaml stop`.

## Diagnóstico con JMX local

Arrancar el backend con `./tests/rendimiento/backend.ps1 -Cache -Diagnostico`.
El lanzador registra métricas de Tomcat y Hikari. El colector usa el
conector local del JDK y queda restringido al proceso de prueba.
El lanzador admite `-Pool 5` para la comparación acotada con cinco
conexiones. El valor habitual se conserva en tres.

```powershell
./tests/rendimiento/diagnostico.ps1 -K6 $k6 -Serie cierre-instrumentado
node tests/rendimiento/resumir-diagnostico.mjs cierre-instrumentado
```

El diagnóstico comprueba EC01 en ambas condiciones antes de escalar.
Mide tiempos HTTP por fase, hilos y solicitudes de Tomcat, conexiones y
esperas Hikari, memoria y GC, conexiones TCP, esperas PostgreSQL y
estadísticas Redis. Para repetir EC02, renueva las sesiones sintéticas
y detén Redis exclusivamente en este Compose. Ejecuta
`node tests/rendimiento/continuidad-redis.mjs` y reinicia Redis después.
Esa prueba agrega un animal sintético. Ejecútala tras las comparaciones
de carga para conservar el volumen del dataset medido.

Para repetir la comparación de tamaño, detener el backend temporal y
crear una segunda base a partir de la semilla aislada.

```powershell
docker compose -f tests/rendimiento/compose.yaml exec -T postgres createdb -U rendimiento -T ganadoconecta_rendimiento_v4 ganadoconecta_rendimiento_crecimiento
$OutputEncoding = [Text.UTF8Encoding]::new($false)
Get-Content -Encoding utf8 -Raw tests/rendimiento/semilla.sql |
  docker compose -f tests/rendimiento/compose.yaml exec -T postgres psql -q -U rendimiento -d ganadoconecta_rendimiento_crecimiento -v animales_por_ganaderia=6000 -v anuncios_por_ganaderia=3000 -v eventos_por_animal=4
```

Alternar tres pares del mismo `planes.mjs` en ambas bases. Guardar los
planes de la base de 24.000 animales como `crecimiento-base-1.txt` a
`crecimiento-base-3.txt` y los de 48.000 como `crecimiento-opt-1.txt`
a `crecimiento-opt-3.txt` bajo `.cache/rendimiento-sprint2/`. Generar
las evidencias con `node tests/rendimiento/resumir-planes.mjs --crecimiento`.
