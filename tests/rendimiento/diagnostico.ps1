param([Parameter(Mandatory)][string]$K6, [ValidatePattern('^[a-z0-9-]+$')][string]$Serie = 'cierre', [switch]$Solo500)
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '../..')
$directorio = (Resolve-Path '.cache/rendimiento-sprint2').Path
$env:GC_RENDIMIENTO_PASSWORD = [IO.File]::ReadAllText((Join-Path $directorio 'postgres.txt'))
$linea = netstat -ano -p tcp | Select-String '127.0.0.1:18083\s+.*LISTENING\s+(\d+)' | Select-Object -First 1
if (-not $linea) { throw 'Inicie el backend aislado con -Cache y -Diagnostico.' }
$api = [int]$linea.Matches[0].Groups[1].Value
$proceso = Get-CimInstance Win32_Process -Filter "ProcessId = $api"
if ($proceso.CommandLine -notmatch 'ActiveProcessorCount=4.+tomcat.mbeanregistry.enabled=true.+-jar') {
    throw 'Backend de diagnostico no identificado.'
}
$csv = Join-Path $directorio "$Serie-jmx.csv"
if (Test-Path $csv) { throw 'Use un nombre de serie nuevo.' }
$clases = (New-Item -Force -ItemType Directory (Join-Path $directorio 'clases-jmx')).FullName
& javac.exe --add-modules jdk.attach -d $clases tests/rendimiento/MuestreoJmx.java
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$java = (Get-Command java.exe).Source
$argumentos = @('-Xms16m','-Xmx64m','--add-modules','jdk.attach','-cp',('"' + $clases + '"'),'MuestreoJmx',$api,('"' + $csv + '"'))
$muestreo = Start-Process -FilePath $java -ArgumentList $argumentos -PassThru -WindowStyle Hidden -RedirectStandardError (Join-Path $directorio "$Serie-jmx-errores.log")
$compose = @('compose','--progress','quiet','-f','tests/rendimiento/compose.yaml')

function Medir([string]$modo, [string]$variante) {
    if ($muestreo.HasExited) { throw 'El muestreo JMX ya no esta activo.' }
    & tests/rendimiento/medir.ps1 -K6 $K6 -Modo $modo -Nombre "$Serie-$variante" -Diagnostico
}

function Redis-Estadisticas([string]$variante, [string]$momento) {
    docker @compose exec -T redis redis-cli INFO stats commandstats latencystats |
        Set-Content -Encoding utf8 (Join-Path $directorio "$Serie-$variante-redis-$momento.txt")
    if ($LASTEXITCODE -ne 0) { throw 'No se pudieron leer las estadisticas de Redis.' }
}

try {
    $limite = (Get-Date).AddSeconds(12)
    while ((Get-Date) -lt $limite -and -not $muestreo.HasExited -and
            (Get-Content -LiteralPath $csv -ErrorAction SilentlyContinue | Measure-Object -Line).Lines -lt 2) {
        Start-Sleep -Milliseconds 250
    }
    if ($muestreo.HasExited -or (Get-Content -LiteralPath $csv -ErrorAction SilentlyContinue | Measure-Object -Line).Lines -lt 2) {
        Get-Content (Join-Path $directorio "$Serie-jmx-errores.log") -Tail 10
        throw 'No inicio el muestreo JMX local.'
    }
    node tests/rendimiento/preparar.mjs 8
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Medir calentar calentamiento
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    if ($Solo500) {
        docker @compose exec -T redis redis-cli FLUSHDB
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        node tests/rendimiento/poblar-cache.mjs
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        Redis-Estadisticas quinientos antes
        Medir quinientos quinientos
        $resultado = $LASTEXITCODE
        Redis-Estadisticas quinientos despues
        exit $resultado
    }
    docker @compose exec -T redis redis-cli FLUSHDB
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Redis-Estadisticas fria antes
    Medir cien fria
    $fria = $LASTEXITCODE
    Redis-Estadisticas fria despues
    if ($fria -ne 0) { exit $fria }
    docker @compose exec -T redis redis-cli FLUSHDB
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    node tests/rendimiento/poblar-cache.mjs
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    if ((docker @compose exec -T redis redis-cli DBSIZE) -ne '12') { throw 'Precarga incompleta.' }
    Redis-Estadisticas poblada antes
    Medir cien poblada
    $poblada = $LASTEXITCODE
    Redis-Estadisticas poblada despues
    if ($poblada -ne 0) { exit $poblada }
    node tests/rendimiento/preparar.mjs 8
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    node tests/rendimiento/poblar-cache.mjs
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Redis-Estadisticas progresiva antes
    Medir progresiva progresiva
    $escalada = $LASTEXITCODE
    Redis-Estadisticas progresiva despues
    exit $escalada
} finally {
    if (-not $muestreo.HasExited) { Stop-Process -Id $muestreo.Id }
}
