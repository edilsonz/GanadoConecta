# GanadoConecta

Sistema Inteligente para la Gestión Integral de la Actividad Ganadera.

## Datos del proyecto

- **Autor:** Carlos Edilson Huarcaya Palomino
- **Universidad:** Universidad Nacional de San Cristóbal de Huamanga
- **Curso:** Arquitectura de Software

## Descripción

GanadoConecta es una aplicación web orientada a productores de ganado
vacuno. Permitirá gestionar animales, mantener sus historiales sanitarios,
reproductivos y productivos, programar actividades, consultar reportes
y analizar registros históricos mediante inteligencia artificial.

También contará con un catálogo de anuncios de animales en venta,
donde los interesados podrán consultar la información publicada y
contactar al vendedor mediante el medio que este autorice.

## Alcance inicial

- Gestión individual de animales e historiales.
- Registro de actividades sanitarias, reproductivas y productivas.
- Calendario, recordatorios y alertas.
- Análisis de registros históricos mediante inteligencia artificial.
- Consulta de reportes.
- Publicación, búsqueda y moderación de anuncios de venta.
- Gestión de usuarios y permisos.

Los historiales completos serán privados. Los anuncios mostrarán
únicamente información autorizada por el productor.

La negociación, los pagos y el transporte se realizarán fuera de la
plataforma. La IA no realizará diagnósticos ni sustituirá la evaluación
de un profesional veterinario.

## Arquitectura propuesta

Arquitectura cliente-servidor con un backend monolítico modular
organizado en capas y un servicio propio de IA independiente.

El backend aplicará Clean Architecture para separar las reglas del
negocio, los casos de uso y los detalles tecnológicos.

## Tecnologías seleccionadas

| Parte | Tecnología |
|---|---|
| Interfaz web | React |
| Backend | Java y Spring Boot |
| Base de datos | PostgreSQL |
| Caché | Redis |
| Servicio de IA | Python y FastAPI |
| Control de versiones | Git y GitHub |

El proveedor de notificaciones y la solución de almacenamiento de
fotografías están pendientes de selección.

## Organización del repositorio

| Carpeta | Contenido |
|---|---|
| docs/01-analisis-de-sistema/ | Análisis y requisitos del sistema |
| docs/02-arquitectura-software/ | Propuesta arquitectónica |
| docs/03-diseno-de-software/ | Diseño interno del software |
| docs/04-modelo-c4/ | Documentación de los modelos C4 |
| docs/05-preparacion-implementacion/ | Propuestas de preparación; pendientes de revisión |
| docs/06-evaluacion-sdd/ | Ruta SDD y criterios de evaluación |
| img/ | Imágenes de los diagramas arquitectónicos y modelos C4 |
| tecnologia/ | Propuesta tecnológica |
| specs/ | Una carpeta por funcionalidad con spec.md, plan.md y tasks.md; plantilla original conservada |
| prompts-codex/ | Instrucciones reutilizables para tareas de Codex |

Consultar la [guía de preparación](docs/05-preparacion-implementacion/README.md),
la [ruta SDD](docs/06-evaluacion-sdd/ruta-sdd-rubrica.md) y las
[convenciones de especificaciones](specs/README.md).

Organización de implementación (frontend/ y backend/ existen; las demás rutas se crearán cuando tengan contenido):

| Ruta | Responsabilidad |
|---|---|
| frontend/ | Aplicación React y sus pruebas unitarias y de integración |
| backend/ | Monolito modular Java/Spring Boot, migraciones y pruebas unitarias y de integración |
| servicio-ia/ | Servicio Python/FastAPI independiente y sus pruebas |
| tests/e2e/ | Pruebas de flujos completos entre servicios |
| tests/carga/ | Escenarios, datos sintéticos y resultados de carga documentados |
| infra/ | Configuración de entornos, servicios locales y despliegue sin secretos |
| .github/workflows/ | Automatización de CI/CD cuando se defina |

Las herramientas de pruebas se revisarán y documentarán antes de implementarlas.
Las carpetas se crearán cuando tengan contenido real; la infraestructura
de despliegue de infra/ se distingue de los adaptadores internos del backend.

## Estado del proyecto

Primera base ejecutable de frontend y backend disponible para revisión.
SPEC-01 está aprobada documentalmente y el registro de cuentas AU01 funciona
desde React hasta PostgreSQL. El backend expone AU02–AU04 para login, renovación,
logout y consulta de sesión con JWT/cookie segura. React ya integra inicio,
restauración y cierre de sesión sin persistir el JWT. Ganaderías, membresías
operativas, animales y funcionamiento offline continúan pendientes; el backend
ya permite crear una ganadería con su propietario inicial mediante AU05.

Se utilizará Redis como caché para consultas frecuentes del catálogo
público de anuncios. PostgreSQL conservará los datos permanentes
y será la fuente principal de información.

Los resultados almacenados en caché tendrán un tiempo de caducidad.
Al publicar, editar, pausar, retirar, moderar o marcar como vendido
un anuncio, se invalidará la caché afectada después de confirmar
el cambio en PostgreSQL.

Si la información no está en caché o Redis no está disponible,
el backend consultará PostgreSQL. La caché no almacenará
historiales privados ni archivos de fotografías.


## Sprint 01: Documentación técnica del proyecto

### Objetivo del Sprint

Organizar, revisar y consolidar la documentación técnica de GanadoConecta, estableciendo las bases para el desarrollo del Sistema Inteligente para la Gestión Integral de la Actividad Ganadera.

### Entregables desarrollados

- Análisis del sistema: actores, historias de usuario y requisitos.
- Identificación de atributos de calidad, restricciones y drivers arquitectónicos.
- Definición de la arquitectura de software y propuesta tecnológica.
- Diagramas C4 de contexto, contenedores y componentes (niveles 1, 2 y 3).
- Diseño interno de los módulos del sistema.
- Aplicación propuesta de principios SOLID y patrones de diseño.
- Revisión de la documentación y coherencia arquitectónica.

### Gestión del Sprint

Las actividades del Sprint 01 se organizan mediante un tablero Kanban en Trello, utilizando las columnas:

BACKLOG → SPRINT ACTUAL → EN PROCESO → EN REVISIÓN → TERMINADO.

El tablero permite realizar el seguimiento de actividades, verificar avances y registrar observaciones durante la revisión.

### Estado del Sprint

**Estado:** Documentación técnica preparada para entrega y pendiente de evaluación docente.

La implementación del sistema se desarrollará en etapas posteriores.

## Alcance arquitectónico consolidado

Se han integrado las decisiones aprobadas de PWA Offline-First con IndexedDB,
acceso familiar autorizado, registro flexible individual y masivo, categorías,
nacimientos y genealogía, campañas sanitarias, recordatorios colectivos e
individuales sin IA y conversación con IA basada en datos autorizados y fuentes.
La interfaz será en español, con «Mi ganado». El catálogo publicará genealogía
solo autorizada y la venta conservará historial sin transferir fichas entre cuentas.
Las fotografías usan un adaptador sin microservicio adicional. La IA no diagnostica
ni prescribe. React, Spring Boot modular/Clean Architecture, PostgreSQL, Redis
público y FastAPI independiente se conservan.

Consultar la [consolidación y pendientes](docs/05-preparacion-implementacion/05-consolidacion-arquitectonica.md)
y la [matriz de evaluación](docs/06-evaluacion-sdd/matriz-trazabilidad.md).
La aprobación de arquitectura no acredita implementación ni aprobación docente.
SPEC-01 y su contrato están aprobados documentalmente; las imágenes C4 siguen siendo originales,
sin fuentes editables encontradas en el repositorio.

## Ejecutar la primera base local

Esta iteración incluye una interfaz React responsiva, registro, login/logout y
salud de la API. Los botones
«Mi ganado» y «Calendario» están deshabilitados porque esas funciones aún no
existen. No hay usuarios de demostración, ganaderías, animales, PWA ni sincronización.
La salud sin perfil postgres comprueba el proceso, no conexión a una base de datos.

### Herramientas y versiones

| Parte | Versión fijada / entorno comprobado |
|---|---|
| Java | Compilación release 21; validada con JDK/Javac 22.0.1 instalado. Para trabajo habitual usar JDK 21 LTS. |
| Maven | 3.9.16 disponible; Spring Boot requiere Maven 3.6.3 o superior. |
| Spring Boot | 3.5.16, compatible con JUnit 5 del plan aprobado. |
| Node / npm | 22.19.0 / 10.9.3; frontend exige Node 22 >=22.12 y npm 10. |
| React / TypeScript / Vite | 19.3.1 / 5.9.3 / 8.3.8, dependencias exactas y package-lock. |
| Pruebas frontend | Vitest 5.1.0, Testing Library React 16.3.3, jest-dom 7.0.1 y happy-dom 21.0.4. |
| Pruebas backend | Spring Boot Test y JUnit 5 gestionados por el parent fijado. |
| Docker / Compose | Motor 29.9.3 y Compose 5.7.0; contenedor PostgreSQL comprobado healthy. |
| PostgreSQL | Imagen postgres:17.12; JDBC validado en 127.0.0.1:5433 hacia el puerto 5432 del contenedor. |
| Migraciones | Flyway 11.7.2 gestionado por Spring Boot 3.5.16; esquema inicial de Identidad validado con PostgreSQL 17.12. |
| Contraseñas | Argon2id de Spring Security con Bouncy Castle 1.86; parámetros y medición documentados para AU01. |

En Windows se usan npm.cmd y mvn.cmd para no depender de la política de scripts
PowerShell. No se modificó Java, Node, PATH ni configuración global del equipo.
Las dependencias indirectas frontend están fijadas en frontend/package-lock.json;
las versiones Maven gestionadas se resuelven desde el parent 3.5.16.

### Backend (primera terminal)

Desde la raíz del repositorio:

```powershell
Set-Location backend
mvn.cmd -B -ntp verify
java -jar target/ganadoconecta-backend-0.1.0-SNAPSHOT.jar
```

API: http://127.0.0.1:8080/api/health, respuesta `{"status":"UP"}`.
Alternativamente, ejecutar `mvn.cmd spring-boot:run`. Detener con Ctrl+C.
Sin el perfil `postgres`, las únicas rutas públicas son GET `/api/health` y GET
`/api/v1/auth/csrf`; el registro no persiste cuentas. Con ese perfil se habilitan
registro, login, renovación, logout y consulta de identidad. Las demás rutas
permanecen denegadas. No se crea ningún usuario o contraseña automática y la
salud no expone detalles.

### Frontend (segunda terminal)

```powershell
Set-Location frontend
npm.cmd ci
npm.cmd run dev
```

Abrir http://127.0.0.1:5173/. Vite reenvía /api al backend mediante proxy local,
sin abrir CORS de la API. Si el backend no está listo, la pantalla informa que
no pudo comprobar conexión y permite volver a intentar. Detener con Ctrl+C.
La pantalla de creación de cuenta requiere que el backend se ejecute con el
perfil `postgres`; crear la cuenta no inicia sesión ni asigna una ganadería. El
formulario de acceso tampoco crea roles, membresías ni ganaderías.

```powershell
npm.cmd run typecheck
npm.cmd test
npm.cmd run build
```

`npm.cmd install` se usa para cambiar dependencias conscientemente; `npm.cmd ci`
reproduce el lockfile en un checkout existente. No usar dist/ como evidencia de
funciones de negocio. Las pruebas básicas de esta base no completan los CA de SPEC-01.

### Variables de entorno

Desde la raíz, copiar `.env.example` a `.env` solo si este último no existe y
completar usuario/contraseña locales antes de iniciar PostgreSQL. No versionar `.env`.
Los campos vacíos del ejemplo no son credenciales utilizables.

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Spring carga opcionalmente .env como propiedades desde su directorio de ejecución
o su padre; usar backend/ como directorio para los comandos del backend. Entorno
del proceso tiene prioridad. Usar valores KEY=VALUE sin comillas para mantener
compatibilidad con ambos lectores (Compose y Spring); no incluir secretos en logs.
POSTGRES_PASSWORD nunca se transforma en variable VITE_* ni se publica al cliente.
API_PROXY_TARGET solo cambia el proxy de desarrollo y puede configurarse en .env.
Las rutas `GANADOCONECTA_JWT_PUBLIC_KEY_PATH` y
`GANADOCONECTA_JWT_PRIVATE_KEY_PATH` son obligatorias con el perfil `postgres`.
Deben apuntar fuera del repositorio a una clave pública X.509 y una privada
PKCS#8, ambas RSA de al menos 2048 bits en PEM. No existe clave predeterminada.
Por ejemplo, con OpenSSL disponible, crear el par en una carpeta privada externa:

```powershell
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out C:/ruta-privada/ganadoconecta-private.pem
openssl pkey -in C:/ruta-privada/ganadoconecta-private.pem -pubout -out C:/ruta-privada/ganadoconecta-public.pem
```

No guardar esas claves en el repositorio ni mostrar su contenido en logs.

La cookie `__Host-GC-Refresh` conserva `Secure`, por lo que el flujo real de
sesión debe revisarse bajo HTTPS también en desarrollo. Vite acepta las rutas
externas `DEV_HTTPS_CERT_PATH` y `DEV_HTTPS_KEY_PATH`; ambas se configuran juntas
y nunca se versionan. El certificado debe ser confiable para el navegador y
válido para `127.0.0.1` o el nombre local elegido. Al usarlas, establecer también
`GANADOCONECTA_ORIGEN_PERMITIDO=https://127.0.0.1:5173` y abrir esa URL. El proxy
puede seguir apuntando internamente a `http://127.0.0.1:8080`. La generación e
instalación del certificado local depende de la herramienta administrada por el
equipo; no se incluye certificado ni clave privada en este repositorio.

### PostgreSQL opcional

No se requiere para arrancar el modo básico. Para usar persistencia en desarrollo,
configurar `.env` y ejecutar desde la raíz:

```powershell
docker compose config --quiet
docker compose up -d postgres
docker compose ps
```

No publicar la salida completa de `docker compose config`, pues puede contener
el secreto expandido. El puerto predeterminado queda ligado a `127.0.0.1:5433`;
el contenedor escucha en 5432. Se mantiene el volumen `postgres_data` y zona UTC.
Después iniciar backend con el perfil opcional:

```powershell
Set-Location backend
$env:DEBUG = "false"
$env:LOGGING_LEVEL_ROOT = "INFO"
mvn.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

Estas dos variables limitan el diagnóstico de esa terminal y evitan salida
innecesariamente detallada; no desactivan CSRF, TLS ni validaciones. Los DTO de
registro y CSRF también ocultan sus valores en representaciones de logging.

Con perfil postgres, la salud incluye la disponibilidad de conexión JDBC en su
resultado agregado, pero no publica detalles. Flyway valida y aplica en orden las
migraciones versionadas de `backend/src/main/resources/db/migration`; `clean` está
deshabilitado y una migración aplicada no debe editarse. El esquema inicial solo
cubre Identidad (usuarios, ganaderías, membresías y sesiones): de autenticación
están disponibles AU01, los endpoints backend AU02–AU04 y su cliente React de
login/logout; no hay ganaderías operativas ni migraciones de animales. Para
detener la BD sin eliminar datos:
`docker compose stop postgres`.
No ejecutar `down -v` para una parada normal. Si cambia usuario/contraseña en .env,
la imagen no reconfigura por sí sola un volumen ya inicializado: revisar la
administración local, sin borrar el volumen como solución automática.

La prueba real de integración se habilita explícitamente porque necesita el
contenedor y el `.env` privado; un `verify` ordinario no depende de servicios locales:

```powershell
Set-Location backend
mvn.cmd -B -ntp "-Dganadoconecta.postgres.integration-test=true" verify
```

La prueba ejecuta una consulta mediante `JdbcTemplate`, comprueba la base,
PostgreSQL 17, el puerto externo 5433 y el interno 5432, valida las migraciones y
las restricciones del esquema de Identidad, y confirma que la salud no exponga
detalles. Los datos sintéticos se revierten al terminar. Una alternativa PostgreSQL
local no fue validada. Mantener
`POSTGRES_PORT=5433` evita conectar por error con el PostgreSQL 18 de Windows en 5432.

### Registro de cuentas — incremento AU01

Con el perfil `postgres`, `POST /api/v1/auth/registro` acepta únicamente `correo`
y `contrasena`. Normaliza el correo, exige 15–128 caracteres Unicode sin recortar
la contraseña, crea una cuenta ACTIVO y no crea sesión, rol ni membresía. Rechaza
campos adicionales y responde sin hash. React obtiene un token no autenticador
mediante `GET /api/v1/auth/csrf`, conserva el valor solo en memoria y lo envía en
`X-CSRF-TOKEN`; Spring mantiene además la cookie `__Host-GC-CSRF` como Secure,
HttpOnly y SameSite=Strict. Un 403 `CSRF_INVALIDO` provoca una sola renovación y
reintento; no se desactiva CSRF ni se guardan credenciales en almacenamiento web.

El formulario evita doble envío, valida correo, confirmación y la longitud Unicode,
y diferencia errores de validación, conflicto, límite, seguridad, servicio y red.
El éxito informa honestamente que no existe una sesión iniciada y permite pasar
al formulario de acceso. Invitaciones, ganaderías y animales continúan cerrados.
Los DTO sensibles ocultan sus valores en representaciones de diagnóstico y los
procesadores HTTP que podrían registrar cuerpos permanecen en nivel INFO.

La protección inicial limita de forma configurable diez intentos por origen en
15 minutos y se reinicia con cada instancia. No confía en `X-Forwarded-For`.
Antes de producción se debe aprobar un límite distribuido y la configuración de
proxy confiable.

### Autenticación y sesiones backend — AU02/AU03/AU04

El backend contiene casos de uso y adaptadores internos para verificar
credenciales Argon2id, crear sesiones con máximo absoluto de 24 horas, emitir y
validar JWT RS256 de hasta 10 minutos y rotar credenciales opacas de renovación.
PostgreSQL guarda únicamente huellas SHA-256 de los valores de renovación y de
las claves de los contadores de fallos. La migración V3 agrega esos contadores
persistentes sin modificar V1/V2.

Con el perfil `postgres` se exponen `POST /api/v1/auth/login`, `/renovar`,
`/logout` y `GET /api/v1/auth/me`. La renovación solo viaja en
`__Host-GC-Refresh` con Secure, HttpOnly, SameSite=Strict, Path=/ y sin Domain;
el JWT de acceso se devuelve para memoria del cliente. Cada petición protegida
revalida sesión y cuenta en PostgreSQL. Las claves RSA se cargan desde rutas
externas obligatorias y las pruebas generan pares efímeros bajo `target/`.

React implementa login, restauración mediante renovación, consulta de identidad,
logout y creación/listado/selección de ganaderías. El JWT de acceso y la selección
se conservan solo en memoria y JavaScript no intenta leer la cookie HttpOnly. Solo
una marca no secreta de cierre remoto pendiente puede guardarse en `localStorage`.
No están disponibles invitaciones, altas familiares, animales ni acceso offline.

`POST /api/v1/ganaderias` implementa AU05. Exige
Bearer JWT y CSRF, acepta solo UUIDv4 y nombre, y crea en una transacción la
ganadería ACTIVA y la membresía PROPIETARIO del actor autenticado. No acepta un
propietario o rol enviado por el cliente. `GET /api/v1/ganaderias` devuelve solo
ganaderías activas donde el actor tiene membresía activa.
`GET /api/v1/ganaderias/{id}/acceso` revalida en PostgreSQL la ganadería y
membresía activas y devuelve rol/estado con hora UTC. La revocación y la
autorización de futuros recursos continúan pendientes.

### Problemas de entorno observados

Maven falló inicialmente con `PKIX path building failed`. En este equipo funcionó
usar el almacén de certificados confiables de Windows solo en el proceso Maven,
sin desactivar TLS, importar certificados ni cambiar el almacén global:

```powershell
$opcionesMavenPrevias = $env:MAVEN_OPTS
try {
  $env:MAVEN_OPTS = "$opcionesMavenPrevias -Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE"
  mvn.cmd -B -ntp verify
} finally {
  $env:MAVEN_OPTS = $opcionesMavenPrevias
}
```

No aplicar este ajuste en otros sistemas operativos sin comprobar su almacén de
confianza. Si no funciona, revisar JDK/red/cadena de certificados con el responsable
del equipo; nunca usar opciones para omitir la validación de certificados.
Docker Desktop fue instalado y validado posteriormente con PostgreSQL 17.12.

Consultar [BASE-01](specs/BASE-01-estructura-ejecutable/spec.md) y el
[informe de implementación y evidencia](docs/05-preparacion-implementacion/06-base-ejecutable.md)
para la base ejecutable, y la
[evidencia del esquema inicial de Identidad](docs/05-preparacion-implementacion/07-esquema-identidad.md)
para la base de datos, y la
[evidencia del registro AU01](docs/05-preparacion-implementacion/08-registro-usuarios.md)
y la [evidencia backend AU02–AU04](docs/05-preparacion-implementacion/09-nucleo-autenticacion-sesiones.md),
además de la [evidencia del cliente React](docs/05-preparacion-implementacion/10-login-react.md),
y la [evidencia de creación de ganadería AU05](docs/05-preparacion-implementacion/11-creacion-ganaderia.md)
y de su [listado/cliente React](docs/05-preparacion-implementacion/12-ganaderias-react-listado.md),
y la [evidencia de contexto AU06](docs/05-preparacion-implementacion/13-acceso-ganaderia.md),
y la [evidencia de revocación AU07](docs/05-preparacion-implementacion/14-revocacion-membresias.md),
y la [evidencia del núcleo T04–T07](docs/05-preparacion-implementacion/15-nucleo-registro-animales.md),
y las evidencias del [endpoint T08](docs/05-preparacion-implementacion/16-endpoint-registro-animales.md),
[consulta T09](docs/05-preparacion-implementacion/17-consulta-animales.md),
[cliente React T10](docs/05-preparacion-implementacion/18-animales-react.md) e
[identificación externa T19](docs/05-preparacion-implementacion/19-identificacion-externa-animales.md),
para resultados realmente ejecutados y limitaciones.
