param([string]$Jar = 'backend/target/ganadoconecta-backend-0.1.0-SNAPSHOT.jar', [switch]$Cache)

$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '../..')
$temporal = (Resolve-Path '.cache/rendimiento-sprint2').Path
$env:POSTGRES_DB = 'ganadoconecta_rendimiento_v4'
$env:POSTGRES_USER = 'rendimiento'
$env:POSTGRES_PASSWORD = [IO.File]::ReadAllText((Join-Path $temporal 'postgres.txt')).Trim()
$env:DB_HOST = '127.0.0.1'
$env:POSTGRES_PORT = '15433'
$env:SERVER_ADDRESS = '127.0.0.1'
$env:SERVER_PORT = '18083'
$env:GANADOCONECTA_ORIGEN_PERMITIDO = 'http://127.0.0.1:18083'
$env:GANADOCONECTA_IA_ACTIVA = 'false'
$env:GANADOCONECTA_FOTOS_DIRECTORIO = (New-Item -ItemType Directory -Force (Join-Path $temporal 'fotos')).FullName
$env:GANADOCONECTA_REPORTES_PRUEBA_AUTENTICADA = 'false'
$env:GANADOCONECTA_JWT_PRIVATE_KEY_PATH = Join-Path $temporal 'jwt-privada.pem'
$env:GANADOCONECTA_JWT_PUBLIC_KEY_PATH = Join-Path $temporal 'jwt-publica.pem'
$env:GANADOCONECTA_CACHE_CATALOGO = $Cache.IsPresent.ToString().ToLowerInvariant()
$env:REDIS_HOST = '127.0.0.1'
$env:REDIS_PORT = '16379'
$env:REDIS_USER = ''
$env:REDIS_PASSWORD = ''
$env:REDIS_TLS = 'false'
$env:REDIS_COMMAND_TIMEOUT = '100ms'
$env:REDIS_CONNECT_TIMEOUT = '150ms'
$env:GANADOCONECTA_CACHE_TTL_SEGUNDOS = '30'
$env:SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE = '10'
$env:SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE = '4'
$env:SPRING_CONFIG_LOCATION = 'classpath:/'
$env:SPRING_CONFIG_IMPORT = 'optional:file:.cache/rendimiento-sprint2/config.properties'
java.exe -Xms256m -Xmx768m '-XX:ActiveProcessorCount=4' -jar $Jar --spring.profiles.active=postgres --debug=false --logging.level.root=INFO --logging.level.org.springframework.web=INFO
exit $LASTEXITCODE
