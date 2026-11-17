# BASE-01 — Primera estructura ejecutable y evidencia

Fecha inicial: 16 de noviembre de 2026, America/Lima. Validación PostgreSQL/JDBC:
17 de noviembre de 2026. Estado: **base técnica y conectividad PostgreSQL/JDBC
verificadas**. No se ha implementado SPEC-01 ni se habilitan datos reales.

## Alcance realizado

La [base SDD](../../specs/BASE-01-estructura-ejecutable/spec.md) concreta una parte
de T03 de SPEC-01, sin alterar sus requisitos ni marcar sus 18 CA como cumplidos.
T02 de autenticación/membresías permanece pendiente. No se implementan registro,
login, animales, IndexedDB, sincronización, PWA, catálogo, Redis ni IA.

```text
frontend/
  package.json, package-lock.json, index.html, tsconfig*.json, vite.config.ts
  src/
    App.tsx, ApiStatus.tsx, main.tsx, styles.css
    App.test.tsx
    test/setup.ts
backend/
  pom.xml
  src/main/java/pe/ganadoconecta/
    GanadoConectaApplication.java
    configuracion/SeguridadConfiguracion.java
    infraestructura/persistencia/PostgresConfiguracion.java
    modulos/identidad/package-info.java
    modulos/ganado/dominio/package-info.java
    modulos/ganado/aplicacion/package-info.java
  src/main/resources/application.yml, application-postgres.yml
  src/test/java/pe/ganadoconecta/{ApiBase,PostgresConectividad}IntegrationTest.java
compose.yaml
.env.example
.editorconfig
specs/BASE-01-estructura-ejecutable/{spec,plan,tasks}.md
```

También se actualizan `.gitignore`, el README principal, el índice de specs y
la guía de preparación. No se modifican los documentos aprobados de SPEC-01.
No se crean carpetas sin contenido ni microservicios adicionales.

La interfaz verde y blanca usa recursos locales, navegación futura deshabilitada
y una ilustración decorativa, sin registros ficticios de animales. El estado de
API consulta realmente `/api/health`; error HTTP, estado DOWN, respuesta inesperada
o fallo de red no se presentan como conexión disponible. Hay reintento y timeout.

El backend es un solo proyecto Maven con límites de módulos documentados. Los
paquetes de dominio/aplicación no dependen de Spring. Todavía no existen entidades,
casos de uso ni repositorios de negocio. Se usa Actuator para GET `/api/health`,
sin componentes ni detalles. Spring Security permite solo esa ruta GET y deniega
las demás; no ofrece login básico, formulario ni usuario/contraseña generados.
CSRF se conserva. La configuración stateless actual no implementa el contrato
de sesión DP01; deberá integrarse al desarrollar T02 y sus pruebas.

## Entorno observado y versiones

| Herramienta | Observación real |
|---|---|
| JDK/Javac | Oracle 22.0.1 disponible; JAVA_HOME no definido. Compilación con release 21. JDK 21 LTS recomendado para trabajo habitual, no instalado en esta tarea. |
| Maven | 3.9.16, `C:\Herramientas\apache-maven-3.9.16\bin\mvn.cmd`. Gradle no disponible. |
| Node / npm | 22.19.0 / 10.9.3. Se usa `npm.cmd` en PowerShell. |
| Docker | Cliente/servidor 29.9.3 y Compose 5.7.0 disponibles el 17 de noviembre. |
| PostgreSQL | Contenedor `postgres:17.12`, healthy, base ganadoconecta y volumen Compose. |
| Navegador | Chrome disponible para comprobación real de pantalla y conexión. |

Versiones fijadas en manifests: Spring Boot 3.5.16, React 19.3.1, TypeScript 5.9.3,
Vite 8.3.8, plugin-react 6.3.0, Vitest 5.1.0, Testing Library React 16.3.3,
jest-dom 7.0.1 y happy-dom 21.0.4. PostgreSQL se configura con imagen `postgres:17.12`.
JUnit 5 y dependencias backend se gestionan por el parent Spring Boot fijado.
Las dependencias frontend exactas y transitivas se registran en package-lock.

Se eligió Maven porque está instalado. Boot 3.5 conserva JUnit 5 previsto por
SPEC-01; no se introdujo una migración a Boot 4/JUnit 6. Node instalado satisface
Vite; happy-dom evita exigir la versión más reciente de Node necesaria para
jsdom actual. Estas elecciones fueron documentadas en BASE-01 antes del código.
El coste Argon2id, pruebas de negocio y navegadores objetivo completos de SPEC-01
siguen pendientes: la fijación parcial de herramientas no completa T03.

Referencias de compatibilidad consultadas:
[Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html),
[Vite](https://vite.dev/guide/),
[versiones PostgreSQL](https://www.postgresql.org/support/versioning/).
Las versiones y motores frontend se comprobaron también contra metadatos del
registro npm; el parent contra Maven Central. No se modificó software del sistema.

## Configuración local

`.env.example` contiene campos vacíos para usuario y contraseña. El `.env` privado
local fue creado por el responsable y permanece ignorado; sus valores no se
imprimieron ni incorporaron a archivos versionados. `.gitignore` excluye `.env`
privados, dependencias, compilaciones, logs y `.cache/`; conserva el ejemplo público.
Spring importa opcionalmente `.env` de su directorio o padre como propiedades.
Vite solo utiliza `API_PROXY_TARGET` para su proxy; no inyecta secretos de BD.
Los comandos de instalación y ejecución están en el [README](../../README.md).

Compose define PostgreSQL, volumen nombrado, healthcheck, zona UTC, puerto ligado
a 127.0.0.1 y variables obligatorias externas. No contiene credenciales por defecto.
El perfil `postgres` crea un DataSource JDBC explícito sin tablas ni migraciones.
El modo básico permite iniciar la API sin BD; su salud UP no demuestra persistencia.
Con el perfil habilitado se agrega la salud JDBC sin publicar detalles.

## Comandos ejecutados y resultados

| Comprobación / comando | Resultado real | Criterios |
|---|---|---|
| `git branch --show-current`, `git status` inicial | develop; árbol limpio antes de cambios, HEAD 1689b40. | BASE-CA06 |
| Inventario `java -version`, `javac -version`, `mvn.cmd -version`, `node --version`, `npm.cmd --version` y detección de comandos/servicios | Herramientas anteriores; sin Docker/PostgreSQL detectados. | BASE-CA05/06 |
| `npm.cmd install --save-exact` con dependencias documentadas | Instalación real: 75 paquetes; auditoría npm inicial 0 vulnerabilidades reportadas. No garantiza ausencia de vulnerabilidades futuras. | BASE-CA01/04 |
| `npm.cmd run typecheck` | Correcto. | BASE-CA01 |
| `npm.cmd run build` | Compilación TypeScript y bundle Vite correctos. | BASE-CA01 |
| `npm.cmd test` | 5 pruebas de componente correctas: inicio, red fallida con reintento, HTTP fallido, DOWN y protocolo inesperado. | BASE-CA01/04 |
| `mvn.cmd -B -ntp verify` | Primer intento bloqueado por confianza TLS PKIX; no se consideró exitoso. | BASE-CA02/03 |
| `mvn.cmd -B -ntp -U verify` con almacén Windows-ROOT solo en MAVEN_OPTS del proceso | BUILD SUCCESS; 3 pruebas HTTP reales, 0 fallos, 0 errores, 0 omitidas; JAR generado. | BASE-CA02/03 |
| `java -jar target/ganadoconecta-backend-0.1.0-SNAPSHOT.jar --debug=false --logging.level.org.springframework=INFO` | API arrancó en 127.0.0.1:8080, sin BD. | BASE-CA02 |
| `npm.cmd run dev` | Vite arrancó en 127.0.0.1:5173. | BASE-CA01 |
| Peticiones HTTP locales con PowerShell | Frontend 200; API salud 200 y cuerpo exacto `{"status":"UP"}`; proxy frontend `/api/health` 200; `/api/animales` 403. | BASE-CA01/02/03 |
| Chrome headless con perfil temporal propio, API de depuración local y comprobación visual | Pantalla y conexión real disponibles a 1440, 390 y 320 px; sin desbordamiento horizontal. Se corrigió un desbordamiento detectado durante la revisión. | BASE-CA01/04 |
| `git diff --check` y revisión adicional de archivos nuevos | Sin errores de espacios; comprobados también los archivos aún no seguidos por Git. | BASE-CA06 |
| Revisión de secretos/archivos privados, enlaces y exclusiones Git | Sin hallazgos en 76 archivos versionados/candidatos; 23 enlaces relativos de documentos cambiados resueltos. `.env` no creado; ejemplo sin credenciales. Dependencias, artefactos y capturas ignorados. | BASE-CA06 |
| `git diff --cached --name-only`, rama y referencias | Staging vacío; develop y HEAD 1689b40; main sin cambios. No se ejecutó add, commit ni push. | BASE-CA06 |

Maven necesitó el almacén de confianza de Windows del proceso Java. No se
desactivó TLS ni se importaron certificados. El README contiene el ajuste con
restauración de MAVEN_OPTS en finally. Véase el proveedor Windows-ROOT en la
[referencia oficial JSSE](https://docs.oracle.com/en/java/javase/25/security/java-secure-socket-extension-jsse-reference-guide.html).

Las capturas locales y el pequeño script de verificación de navegador se guardan
en `.cache/`, ignorado por Git; son evidencia local de esta revisión, no una suite
E2E mantenida ni una prueba de los CA de SPEC-01. Los informes automatizados se
regeneran con Maven/Vitest. No se ejecutaron pruebas de carga de animales/anuncios,
porque esos flujos todavía no existen. Tampoco se acreditan accesibilidad integral,
offline ni navegadores adicionales mediante esta comprobación visual.

## Validación PostgreSQL/JDBC posterior

El 17 de noviembre de 2026 se completó BASE-CA05 con el entorno local del
responsable. Se preservaron los secretos únicamente en `.env`, ignorado por Git.

| Comprobación | Resultado real |
|---|---|
| `docker version` y `docker compose version` | Cliente/servidor 29.9.3; Compose 5.7.0. |
| `docker compose config --quiet` | Configuración válida; no se imprimió la expansión que contiene secretos. |
| `docker compose ps` e inspección del contenedor | `postgres:17.12`, running/healthy, `127.0.0.1:5433` hacia 5432. |
| Consultas `psql` de solo lectura dentro del contenedor | Base ganadoconecta, servidor 17.12 y configuración del servidor UTC confirmados. |
| Primer `mvn ... verify` JDBC | Abrió JDBC pero falló una aserción ajena a conectividad: el driver fijó la zona de sesión en America/Lima. No se declaró exitoso. |
| `mvn.cmd -B -ntp verify` ordinario final | BUILD SUCCESS; 3 pruebas ejecutadas y 2 JDBC omitidas expresamente por depender del entorno local. |
| `mvn.cmd -B -ntp "-Dganadoconecta.postgres.integration-test=true" verify` corregido | BUILD SUCCESS; 5 pruebas, 0 fallos/errores/omitidas. Dos pruebas validaron JDBC y salud con BD. |
| JAR con `--spring.profiles.active=postgres` | Backend arrancó, salud UP y `/api/animales` continuó en 403. |
| Conexiones TCP del PID Java | Tres conexiones establecidas a 127.0.0.1:5433; ninguna a 5432. |

La consulta JDBC verificó `current_database()`, `inet_server_port()` y
`server_version_num`: base ganadoconecta, puerto del servidor 5432 y versión mayor
17. La URL configurada fue exactamente `jdbc:postgresql://127.0.0.1:5433/ganadoconecta`.
No se añadió un endpoint de diagnóstico ni se expusieron URL, usuario o contraseña
en `/api/health`; la respuesta se mantuvo en `{"status":"UP"}`.

El ajuste de zona horaria retirado de la prueba no oculta un fallo de conexión:
el servidor Compose arranca con UTC, mientras el driver adapta la zona de la sesión
JDBC al proceso Java en America/Lima. La persistencia futura deberá probar por
separado instantes UTC y fechas de negocio según DP08.

El esquema inicial de Identidad y sus migraciones se implementaron después y se
documentan en [07-esquema-identidad.md](07-esquema-identidad.md). Siguen pendientes
los casos de uso de autenticación y toda persistencia de animales y demás negocio.
También queda pendiente verificar el comportamiento de salud con PostgreSQL caído;
no se detuvo el contenedor healthy del responsable durante esta tarea. Mantener
cerrados los flujos de animales hasta implementar autenticación/membresías y los
gates de SPEC-01. Esta evidencia no equivale a preparación para producción.
