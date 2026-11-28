$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '../..')
$env:GC_RENDIMIENTO_PASSWORD = [IO.File]::ReadAllText((Resolve-Path '.cache/rendimiento-sprint2/postgres.txt').Path)
$env:SSLKEYLOGFILE = ''
$formato = [Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = $formato
$OutputEncoding = $formato
$comun = @('compose','-f','tests/rendimiento/compose.yaml')
$sql = $comun + @('exec','-T','postgres','psql','-qAt','-v','ON_ERROR_STOP=1','-U','rendimiento','-d','ganadoconecta_rendimiento_v4')
$redis = $comun + @('exec','-T','redis','redis-cli','--raw')
$url = 'http://127.0.0.1:18083/api/v1/catalogo'

function Ejecutar-Docker([string[]]$Argumentos) {
    & docker @Argumentos
    if ($LASTEXITCODE -ne 0) { throw 'Falló un comando de los contenedores de rendimiento.' }
}
function Obtener-Catalogo {
    $respuesta = Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 -Uri $url
    if ($respuesta.StatusCode -ne 200) { throw 'Falló la consulta del catálogo.' }
    $respuesta.Content
}
function Aciertos-Redis {
    $linea = Ejecutar-Docker ($redis + @('info','stats')) | Select-String '^keyspace_hits:(\d+)'
    if (-not $linea) { throw 'Redis no devolvió estadísticas.' }
    [long]$linea.Matches[0].Groups[1].Value
}

$antes = Obtener-Catalogo
$id = [guid](($antes | ConvertFrom-Json).items[0].id)
$revision = Ejecutar-Docker ($sql + @('-c','SELECT revision FROM catalogo.revision_publica WHERE unica'))
$sha = [Security.Cryptography.SHA256]::Create()
$filtros = $formato.GetBytes('{"categoria":null,"sexo":null,"raza":null,"ubicacion":null,"minimo":null,"maximo":null,"pagina":1}')
$huella = [BitConverter]::ToString($sha.ComputeHash($filtros)).Replace('-','').ToLowerInvariant()
$sha.Dispose()
$clave = "gc:catalogo:publico:v1:publicado-desc-id-desc:${revision}:$huella"
$limite = (Get-Date).AddSeconds(8)
do {
    $null = Obtener-Catalogo
    $valor = Ejecutar-Docker ($redis + @('get',$clave))
    if ($valor) { break }
    Start-Sleep -Milliseconds 250
} while ((Get-Date) -lt $limite)
if (-not $valor) { throw 'Falta la entrada inicial. Inicia Spring con caché habilitada.' }
if (($valor | ConvertFrom-Json).items.id -notcontains "$id") { throw 'La entrada inicial no coincide con el anuncio sintético.' }
$pausado = $false
try {
    Ejecutar-Docker ($comun + @('stop','redis')) | Out-Null
    $respaldo = Obtener-Catalogo
    if ($respaldo -cne $antes) { throw 'Cambió la respuesta al activar el respaldo.' }
    $confirma = "UPDATE catalogo.anuncio SET estado='PAUSADO' WHERE id='$id' AND titulo LIKE 'Anuncio sintético %' AND estado='PUBLICADO' RETURNING id"
    if ((Ejecutar-Docker ($sql + @('-c',$confirma))) -ne "$id") { throw 'No se encontró el anuncio sintético esperado.' }
    $pausado = $true
    if (((Obtener-Catalogo | ConvertFrom-Json).items.id) -contains "$id") { throw 'El respaldo devolvió el anuncio pausado.' }
    Ejecutar-Docker ($comun + @('up','-d','--wait','redis')) | Out-Null
    $valor | docker @redis -x set $clave | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo reinyectar la clave histórica sintética.' }
    Ejecutar-Docker ($redis + @('expire',$clave,'30')) | Out-Null
    $aciertos = Aciertos-Redis
    $reintentado = $false
    for ($intento = 0; $intento -lt 20; $intento++) {
        $pagina = Obtener-Catalogo | ConvertFrom-Json
        if ($pagina.items.id -contains "$id") { throw 'La revisión antigua se volvió visible.' }
        if ((Aciertos-Redis) -gt $aciertos) {
            $reintentado = $true
            break
        }
        Start-Sleep -Milliseconds 250
    }
    if (-not $reintentado) { throw 'Redis no recuperó el servicio dentro del plazo de prueba.' }
    $resultado = [ordered]@{ igualdadDuranteCaida=$true; pausaDuranteCaida=$true; claveHistoricaReinyectada=$true; recuperacionConAciertos=$true }
    $resultado | ConvertTo-Json | Set-Content -LiteralPath '.cache/rendimiento-sprint2/redis-ciclo.json' -Encoding utf8
    $resultado | ConvertTo-Json
} finally {
    if ($pausado) {
        Ejecutar-Docker ($sql + @('-c',"UPDATE catalogo.anuncio SET estado='PUBLICADO' WHERE id='$id' AND titulo LIKE 'Anuncio sintético %'")) | Out-Null
    }
    Ejecutar-Docker ($comun + @('up','-d','--wait','redis')) | Out-Null
}
