# BASE-01 — Primera estructura ejecutable y evidencia

Fecha: 16 de noviembre de 2026, America/Lima. Rama `develop`, partiendo de
`1689b40`. Estado: **base técnica implementada para revisión; PostgreSQL/Compose
pendientes de validación**. No se ha implementado SPEC-01 ni se habilitan datos reales.

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
  src/test/java/pe/ganadoconecta/ApiBaseIntegrationTest.java
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
| Docker | No se encontró comando, instalación estándar de Docker Desktop ni servicio correspondiente. |
| PostgreSQL | `psql`, instalación estándar y servicio local no detectados. |
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

`.env.example` contiene campos vacíos para usuario y contraseña; no se creó `.env`
ni una credencial real. `.gitignore` excluye `.env` privados, dependencias,
compilaciones, logs y `.cache/`; conserva únicamente el ejemplo público.
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

## Pendiente para cerrar la validación de infraestructura

1. Instalar/configurar Docker Desktop/Compose con WSL2 siguiendo el README,
   o proporcionar PostgreSQL local administrado. No se hizo instalación automática.
2. Crear `.env` privado con valores elegidos por el desarrollador.
3. Ejecutar `docker compose config --quiet`, iniciar PostgreSQL y comprobar salud
   y persistencia del volumen. Estos comandos **no se ejecutaron** por Docker ausente.
4. Arrancar perfil `postgres`; verificar salud UP con BD disponible y DOWN/503
   al no poder conectar, sin filtrar credenciales ni detalles. Conexión JDBC real
   **no validada** en esta iteración. No borrar volúmenes para resolver errores.
5. Revisar esta base antes del siguiente incremento. Mantener cerrados los flujos
   de animales hasta implementar autenticación/membresías y gates de SPEC-01.

BASE-CA05 permanece parcialmente verificado por revisión de configuración;
su validación ejecutable está pendiente. Los demás criterios cuentan con la
evidencia básica indicada, sin equivaler a preparación para producción.
