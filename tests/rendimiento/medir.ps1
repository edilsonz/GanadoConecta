param(
    [Parameter(Mandatory)][string]$K6,
    [Parameter(Mandatory)][ValidateSet('smoke','calentar','cien','progresiva')][string]$Modo,
    [Parameter(Mandatory)][ValidatePattern('^[a-z0-9-]+$')][string]$Nombre
)
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '../..')
$gcCarpeta = '.cache/rendimiento-sprint2'
$env:GC_RENDIMIENTO_PASSWORD = [IO.File]::ReadAllText((Resolve-Path "$gcCarpeta/postgres.txt").Path)
$env:K6_NO_USAGE_REPORT = 'true'
Remove-Item Env:SSLKEYLOGFILE -ErrorAction SilentlyContinue
$env:MODO = $Modo
$env:RESULTADO = "$gcCarpeta/$Nombre.json"
$gcLinea = netstat -ano -p tcp | Select-String '127.0.0.1:18083\s+.*LISTENING\s+(\d+)' | Select-Object -First 1
if (-not $gcLinea) { throw 'No está escuchando el backend aislado.' }
$gcApiId = [int]$gcLinea.Matches[0].Groups[1].Value
$gcApi = Get-CimInstance Win32_Process -Filter "ProcessId = $gcApiId"
if ($gcApi.CommandLine -notmatch 'ActiveProcessorCount=4.+-jar\s+.+\.jar') { throw 'Backend aislado no identificado.' }
$gcCsv = "$gcCarpeta/$Nombre-recursos.csv"
if (Test-Path -LiteralPath $gcCsv) { throw 'Usa un nombre de ejecución nuevo.' }
$gcSistema = Get-CimInstance Win32_PerfFormattedData_PerfOS_Memory
if ($gcSistema.AvailableMBytes -lt 1536 -or $gcSistema.PercentCommittedBytesInUse -ge 95) {
    throw 'Memoria insuficiente para iniciar una carga controlada.'
}
$gcInicio = Get-Date
$gcCpuApi = (Get-Process -Id $gcApiId).CPU
$gcCpuK6 = 0.0
$gcInstanteAnterior = $gcInicio
$gcSaturadas = 0
$gcParada = $null
$gcControl = 'http://127.0.0.1:16655/v1/status'
if (netstat -ano -p tcp | Select-String '127.0.0.1:16655\s+.*LISTENING') {
    throw 'El puerto de control local de k6 ya está en uso.'
}
$gcOpciones = @{
    FilePath = (Resolve-Path -LiteralPath $K6).Path
    ArgumentList = 'run','--quiet','--address=127.0.0.1:16655','tests/rendimiento/consultas.k6.js'
    RedirectStandardOutput = "$gcCarpeta/$Nombre-k6.log"
    RedirectStandardError = "$gcCarpeta/$Nombre-k6-errores.log"
    WindowStyle = 'Hidden'
    PassThru = $true
}
$gcProceso = Start-Process @gcOpciones
$null = $gcProceso.Handle

try {
    while (-not $gcProceso.HasExited) {
        $gcAhora = Get-Date
        $gcSegundos = ($gcAhora - $gcInstanteAnterior).TotalSeconds
        if ($gcSegundos -ge 2) {
            $gcJava = Get-Process -Id $gcApiId
            $gcCarga = Get-Process -Id $gcProceso.Id -ErrorAction SilentlyContinue
            if (-not $gcCarga) { break }
            $gcMemoria = Get-CimInstance Win32_PerfFormattedData_PerfOS_Memory
            $gcLibre = $gcMemoria.AvailableMBytes
            $gcJavaCpu = 100 * ($gcJava.CPU - $gcCpuApi) / $gcSegundos
            $gcK6Cpu = 100 * ($gcCarga.CPU - $gcCpuK6) / $gcSegundos
            $gcSql = "SELECT count(*)||','||count(*) FILTER (WHERE state='active') FROM pg_stat_activity WHERE datname=current_database() AND pid<>pg_backend_pid()"
            $gcDatos = docker compose -f tests/rendimiento/compose.yaml exec -T postgres psql -qAt -U rendimiento -d ganadoconecta_rendimiento_v4 -c $gcSql
            if ($LASTEXITCODE -ne 0) { throw 'No se pudo muestrear PostgreSQL.' }
            $gcContenedores = docker stats --no-stream --format '{{.CPUPerc}} / {{.MemUsage}}' ganadoconecta-rendimiento-postgres-1
            $gcRedis = docker ps --filter 'name=^ganadoconecta-rendimiento-redis-1$' --format '{{.Names}}'
            $gcRedisUso = if ($gcRedis) { docker stats --no-stream --format '{{.CPUPerc}} / {{.MemUsage}}' $gcRedis }
            $gcVu = try {
                (Invoke-RestMethod -Method Get -Uri $gcControl -TimeoutSec 2).data.attributes.vus
            } catch { $null }
            [pscustomobject]@{
                segundo = [int]($gcAhora - $gcInicio).TotalSeconds
                java_cpu_una_cpu_pct = [int]$gcJavaCpu
                java_rss_mib = [int]($gcJava.WorkingSet64 / 1MB)
                k6_cpu_una_cpu_pct = [int]$gcK6Cpu
                k6_rss_mib = [int]($gcCarga.WorkingSet64 / 1MB)
                vu_activos = $gcVu
                host_disponible_mib = [int]$gcLibre
                host_comprometida_pct = $gcMemoria.PercentCommittedBytesInUse
                pg_conexiones = ($gcDatos -split ',')[0]
                pg_activas = ($gcDatos -split ',')[1]
                pg_cpu_y_memoria = $gcContenedores
                redis_cpu_y_memoria = $gcRedisUso
            } | Export-Csv -LiteralPath $gcCsv -NoTypeInformation -Append
            $gcCpuApi = $gcJava.CPU
            $gcCpuK6 = $gcCarga.CPU
            $gcInstanteAnterior = $gcAhora
            $gcPgCpu = if ($gcContenedores -match '^([\d.]+)%') {
                [double]::Parse($Matches[1], [Globalization.CultureInfo]::InvariantCulture)
            } else { 0 }
            $gcSaturada = $gcJavaCpu -ge 380 -or $gcK6Cpu -ge 900 -or $gcPgCpu -ge 195
            $gcSaturadas = if ($gcSaturada) { $gcSaturadas + 1 } else { 0 }
            if ($gcLibre -lt 1536 -or $gcMemoria.PercentCommittedBytesInUse -ge 95 -or
                    $gcCarga.WorkingSet64 -gt 1800MB -or $gcJava.WorkingSet64 -gt 1500MB) {
                $gcParada = 'Límite de memoria de la estación o de un proceso temporal.'
                break
            }
            if ($gcSaturadas -ge 4) {
                $gcParada = 'Cuatro muestras seguidas de saturación de CPU del backend, PostgreSQL o el generador.'
                break
            }
        }
        Start-Sleep -Milliseconds 400
    }
} finally {
    if (-not $gcProceso.HasExited) {
        try {
            $gcCuerpo='{"data":{"type":"status","id":"default","attributes":{"stopped":true}}}'
            Invoke-RestMethod -Method Patch -Uri $gcControl -ContentType 'application/json' -Body $gcCuerpo -TimeoutSec 2 | Out-Null
            if (-not $gcProceso.WaitForExit(4000)) { Stop-Process -Id $gcProceso.Id }
        } catch {
            if (-not $gcProceso.HasExited) { Stop-Process -Id $gcProceso.Id }
        }
    }
    $gcProceso.WaitForExit()
}
if ($gcParada) {
    $gcParada | Set-Content -LiteralPath "$gcCarpeta/$Nombre-parada.txt"
    Write-Output $gcParada
    exit 3
}
Get-Content -LiteralPath "$gcCarpeta/$Nombre-k6.log" | Select-Object -Last 8
Get-Content -LiteralPath "$gcCarpeta/$Nombre-k6-errores.log" | Select-Object -Last 8
if ($null -eq $gcProceso.ExitCode) { throw 'k6 no devolvió código de salida.' }
if ($gcProceso.ExitCode -eq 0 -and -not (Test-Path -LiteralPath $env:RESULTADO)) { throw 'Falta el resumen de k6.' }
exit $gcProceso.ExitCode
