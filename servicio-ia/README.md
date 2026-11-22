# Servicio local de inferencia de GanadoConecta

El alcance inicial de [SPEC-12](../specs/SPEC-12-analisis-alertas-asistente/spec.md)
cubre preguntas en español sobre registros propios confirmados. Spring Boot es
el único intermediario de PostgreSQL. FastAPI recibe hechos mínimos y consulta
Ollama en `127.0.0.1`. Solo los valores verificados en Spring aparecen en la
respuesta. Los servicios de inferencia escuchan en loopback y el frontend usa
la sesión y la protección CSRF del monolito.

## Preparación en PowerShell

Se usó Python 3.14.5, Ollama 0.40.17 y el modelo
`qwen3:4b-instruct-2507-q4_K_M` (`0edcdef34593`, 2,5 GB en el catálogo).
El paquete portable de Windows se obtiene de la
[publicación oficial](https://github.com/ollama/ollama/releases/tag/v0.40.17).
Para usar las rutas de esta guía, extrae `ollama-windows-amd64.zip` en
`.cache/ollama-0.40.17/` y compara el SHA-256 con el de la publicación.
El equipo verificado tiene unos 16 GiB de RAM y una RTX 2060 de 6 GiB.
La memoria requerida depende del contexto y de los demás procesos activos.

Desde la raíz, configura el intérprete completo si el comando `python` abre
Microsoft Store. Usa el entorno virtual y un secreto de servicio generado
localmente. Conserva la misma copia del secreto para Spring Boot y FastAPI.

```powershell
$python = Join-Path $env:LOCALAPPDATA 'Python/pythoncore-3.14-64/python.exe'
& $python -m venv .cache/rf19-venv
& .cache/rf19-venv/Scripts/python.exe -m pip install -r servicio-ia/requirements.txt
& .cache/rf19-venv/Scripts/python.exe -m pip check
if (!(Test-Path .cache/rf19-secreto)) {
  node -p "require('node:crypto').randomBytes(32).toString('base64url')" |
    Set-Content -Encoding ascii -LiteralPath .cache/rf19-secreto
}
```

### Terminal 1 · Ollama local

```powershell
$env:OLLAMA_HOST = '127.0.0.1:11439'
$env:OLLAMA_NO_CLOUD = '1'
$env:OLLAMA_NUM_PARALLEL = '1'
$env:OLLAMA_MAX_LOADED_MODELS = '1'
$pesos = Join-Path $env:TEMP 'ganadoconecta-rf19-modelos-0edcdef34593'
if (Test-Path -LiteralPath $pesos) {
  $env:OLLAMA_MODELS = $pesos
} else {
  $env:OLLAMA_MODELS = Join-Path $env:LOCALAPPDATA 'GanadoConecta/ollama-modelos'
}
& .cache/ollama-0.40.17/ollama.exe serve
```

Desde otra terminal, con el servidor iniciado, consulta `ollama list` y ejecuta
`pull` si falta el modelo. Antes de una demostración, reserva tiempo para
precargar el modelo. La duración en frío varió entre sesiones. Los resultados
medidos están en la [evidencia de RF19](../specs/SPEC-12-analisis-alertas-asistente/tasks.md#fase-2--validación-local-de-rf19).

```powershell
$env:OLLAMA_HOST = '127.0.0.1:11439'
& .cache/ollama-0.40.17/ollama.exe pull qwen3:4b-instruct-2507-q4_K_M
$carga = @{model='qwen3:4b-instruct-2507-q4_K_M'; keep_alive='5m';
  options=@{num_ctx=2048; num_batch=64}} | ConvertTo-Json -Depth 4
Invoke-RestMethod -Method Post -Uri http://127.0.0.1:11439/api/generate `
  -ContentType 'application/json' -Body $carga -TimeoutSec 180 | Out-Null
```

### Terminal 2 · FastAPI

Desde la raíz:

```powershell
$env:GANADOCONECTA_IA_SECRETO = (Get-Content .cache/rf19-secreto -Raw).Trim()
$env:GANADOCONECTA_OLLAMA_PUERTO = '11439'
Set-Location servicio-ia
& ../.cache/rf19-venv/Scripts/python.exe -m uvicorn ganadoconecta_ia.api:crear_app `
  --factory --host 127.0.0.1 --port 18081 --no-access-log --no-proxy-headers
```

### Terminales 3 y 4 · Spring Boot y PWA

Sigue la [guía HTTPS con PostgreSQL aislado](../tests/e2e/README.md) para
certificados, credenciales, `POSTGRES_DB` y puerto. En la terminal de Spring,
carga además estas variables desde la raíz antes de `Set-Location backend`:

```powershell
$env:GANADOCONECTA_IA_ACTIVA = 'true'
$env:GANADOCONECTA_IA_PUERTO = '18081'
$env:GANADOCONECTA_IA_SECRETO = (Get-Content .cache/rf19-secreto -Raw).Trim()
```

`GANADOCONECTA_OLLAMA_PLAZO` admite segundos hasta 25 en FastAPI. El
adaptador Spring tiene un límite de 30 segundos y devuelve 503 si el motor
no responde o entrega una salida inválida. Se admite una sola inferencia
simultánea. El análisis y las alertas operan de forma independiente. Al
desactivar `GANADOCONECTA_IA_ACTIVA`, Spring mantiene el resto del módulo.

## Demostración y pruebas

Con una cuenta sintética, elige la ganadería en la PWA, entra en **Análisis y
alertas**, aplica un período y selecciona un animal con dos pesajes conocidos.
En el asistente, elige **Peso** y pregunta «¿Cuánto cambió el peso entre los
últimos pesajes?». Contrasta la diferencia y las dos fechas de origen. Usa un
animal sin historial para comprobar el estado de información insuficiente.

```powershell
Set-Location servicio-ia
& ../.cache/rf19-venv/Scripts/python.exe -m unittest discover -s tests
```

Tras la descarga inicial se puede ejecutar en el equipo sin internet,
manteniendo disponibles los servicios locales. La PWA en otro dispositivo
requiere alcanzar Spring Boot y una sesión vigente. El chat conserva sus
turnos únicamente en la vista activa.
