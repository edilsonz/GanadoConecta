# Sprint 2 · Implementación y validación de arquitectura

Evidencia del 27 de noviembre de 2026. El primer incremento partió de
`d5d110a` y el segundo de `ea72e80`.
[Matriz de calidad](matriz-calidad.md) · [OpenAPI y Swagger](openapi/README.md) ·
[Cobertura medida](cobertura.json) · [Auditoría funcional](../AUDITORIA-FUNCIONAL.md)

## Estado de las ocho actividades

| Actividad académica | Estado local y evidencia |
|---|---|
| 1. Estilo arquitectónico | Implementado y contrastado. Cliente-servidor, React/PWA y API REST; PostgreSQL es la fuente principal. |
| 2. Patrón arquitectónico | Monolito modular en Spring Boot y Clean Architecture para sus dependencias internas. Se verificó el bytecode de dominio y aplicación. |
| 3. SDD | BASE-01 y SPEC-01–12 tienen especificación, plan y tareas. Se corrigió el índice y se vinculó la evidencia a los escenarios originales. Las brechas funcionales siguen en cada SPEC y en la auditoría. |
| 4. REST y OpenAPI | 53 operaciones explícitas, 44 rutas y 13 módulos. Swagger UI comprobada en Chrome y especificaciones Spring y FastAPI validadas. |
| 5. Base de datos y caché | Planes comparables de seis consultas, paginación SQL optimizada y caché pública Redis con revisión transaccional. [PostgreSQL](rendimiento/postgresql.md) · [Redis](rendimiento/redis.md) |
| 6. SOLID y patrones | Revisión de contratos, adaptadores y coordinación de transacciones con ejemplos de código verificables. |
| 7. Jest, cobertura y CI | Cobertura medida con Jest, Vitest, JaCoCo y coverage.py. GitHub Actions aprobó los tres trabajos y publicó tres artefactos en `ea72e80`. Las pruebas Redis se incorporaron al workflow y su ejecución remota queda pendiente. |
| 8. Atributos y k6 | Parcial. EC01 pasó con Redis frío y poblado a 100 VU. EC03 alcanzó 1.000 VU y el umbral operativo detuvo la escalada. [Matriz EC01–EC13](matriz-calidad.md) · [Carga](rendimiento/k6.md) |

## Concordancia con el Sprint 1

- **ADR-02, cliente-servidor.** React consume la API REST por HTTPS. IndexedDB
  conserva el contexto y las colas permitidas. El Service Worker precacha
  recursos estáticos. Spring vuelve a validar sesión y membresía al recibir
  operaciones. PostgreSQL conserva los registros confirmados.
- **ADR-01, monolito modular.** `GanadoConectaApplication` arranca los módulos
  de identidad, ganado, sanidad, reproducción, catálogo, reportes y análisis
  como un desplegable. FastAPI se ejecuta como proceso independiente para
  inferencia. Spring obtiene y filtra los datos, `MotorAsistenteLocal` llama
  a FastAPI y este consulta Qwen3 en Ollama por loopback. La inferencia real
  tiene evidencia previa, mientras que esta iteración ejecutó el contrato
  interno y los fallos controlados.
- **ADR-03, Clean Architecture.** `AnimalesControlador.registrar` traduce
  HTTP al caso de uso `RegistrarAnimal.ejecutar`. El caso de uso valida el
  ámbito durante la transacción y usa `RepositorioAnimales` y
  `UnidadDeTrabajo`. `RepositorioAnimalesPostgres` y
  `UnidadDeTrabajoPostgres` implementan los puertos. La composición se hace
  en `GanadoConfiguracion`. La prueba de arquitectura revisa clases
  compiladas. Comprueba que el dominio no dependa de aplicación y que el
  núcleo quede libre de dependencias hacia Spring, Jackson, SQL, Servlet,
  clientes HTTP y adaptadores.

[Estilo original](../02-arquitectura-software/estilo-arquitectonico.md) ·
[Decisiones Sprint 1](../01-analisis-de-sistema/07-decisiones-arquitectonicas.md) ·
[Diseño interno](../03-diseno-de-software/dise%C3%B1o-interno-de-modulos.md) ·
[C4 vigente](../04-modelo-c4/Nivel2-DiagramaContenedores.md)

### SOLID y patrones comprobados

| Principio | Ejemplo ejecutable | Límite observado |
|---|---|---|
| Responsabilidad única | `ConsultasAnimalesControlador` recibe filtros y proyecta HTTP; `ConsultarFichaAnimal` y `ListarAnimales` consultan el ámbito; `RepositorioAnimalesPostgres` ejecuta persistencia. | Los adaptadores HTTP también construyen algunas proyecciones pequeñas de respuesta. |
| Abierto/cerrado | `ConsultarAsistente.Motor` permite sustituir `MotorAsistenteLocal`; la prueba de asistente usa un doble de ese puerto para respuestas inválidas. | La sustitución se observa en esta integración, sin exigir puntos de extensión universales. |
| Sustitución | El caso de uso admite implementaciones de `RepositorioAnimales` con el mismo contrato. Las pruebas del núcleo sustituyen persistencia con dobles. | La observación se limita a los contratos probados. Reglas SQL, bloqueos y transacciones tienen pruebas PostgreSQL aparte. |
| Segregación | `UnidadDeTrabajo.ejecutar`, `CodificadorContrasena` y el puerto `Motor` separan necesidades del caso de uso. | Algunos contratos de repositorio agrupan operaciones del mismo agregado. |
| Inversión | `RegistrarAnimal` y `RegistrarVenta` dependen de puertos que la configuración vincula a JDBC. `MotorAsistenteLocal` y `CodificadorContrasenaArgon2` dependen de los contratos de aplicación. | La integración entre módulos usa contratos de identidad para permisos. Los límites de evolución siguen en EC06. |

Los patrones se observan en el **repositorio** (`RepositorioAnimalesPostgres`),
el **adaptador** (`MotorAsistenteLocal`, `FotosDirectorioPrivado`) y la **unidad
de trabajo** (`UnidadDeTrabajoPostgres`). En venta, el caso de uso autoriza
la operación y el adaptador ejecuta las escrituras dentro de la misma
transacción.

En C4, los reportes y la IA seguían etiquetados como previstos y el contenedor
PostgreSQL indicaba V1–V16. Se corrigieron la fuente editable y sus dos
exportaciones. El diagrama del modelo de datos conserva su alcance parcial de
V1–V16. El segundo incremento incorporó Redis opcional. Entrega externa,
almacenamiento definitivo de fotos y producción siguen previstos.
El asistente actual tiene un plazo acotado y
capacidad limitada. Las tareas persistentes de análisis y notificación de
ADR-09 siguen pendientes de implementación.

### Revisión SDD

Se revisaron requisitos, historias, criterios de aceptación, plan, tareas y
evidencias en el [índice SDD](../../specs/README.md) y se cruzaron con la
[matriz funcional](../MATRIZ-DE-TRAZABILIDAD.md). Los contratos separan el alta
y sincronización parcial (SPEC-01/02), la gestión ganadera (SPEC-03–07),
el catálogo y venta (SPEC-08/09), la plataforma y los reportes (SPEC-10/11)
y el asistente (SPEC-12). La auditoría conserva el detalle de las brechas.
Los estados originales de las SPEC y sus umbrales de calidad se preservan.

## Pruebas y cobertura local

Entorno Windows, Node 22.19.0, npm 10.9.3 y Java 22.0.1 con destino de
compilación 21. PostgreSQL Docker 17.12 escuchó en 5433 del host y 5432 del
contenedor, con base sintética y migraciones V1–V17. FastAPI se probó con
Python 3.14 y adaptador controlado. El navegador usó perfiles temporales.

| Suite | Resultado | Líneas | Sentencias o instrucciones | Funciones o métodos | Ramas |
|---|---:|---:|---:|---:|---:|
| Jest 30.5.2 · formulario de ganado y cliente privado | 11/11 | 98,36 % | 98,43 % sentencias | 100 % | 91,46 % |
| Vitest 5.1.0 · código de producción del frontend | 251/251 | 81,47 % | 75,09 % sentencias | 74,44 % | 66,00 % |
| JaCoCo 0.8.14 · regresión Java | 185/185 | 91,28 % | 91,51 % instrucciones | 93,16 % métodos | 68,44 % |
| coverage.py 7.14.1 · servicio y exportador | 7/7 | 86,07 % líneas ejecutables | 86,07 % sentencias | Sin contador de funciones | 66,67 % |

Los informes incluyen denominadores reales. Jest ejecuta una suite pequeña en
Node con TypeScript transpilado. Vitest conserva la regresión de componentes,
estado, API y PWA. Se informan las coberturas de cada herramienta por separado.
El porcentaje combinado de líneas y ramas en Python fue 82,88 %.

En Vitest los puntos con baja cobertura incluyen `ReportarAnuncio.tsx` (0 %),
`AnunciosPanel.tsx` (3,57 %) y la capa común `solicitudPrivada.ts` (7,14 %),
que Jest cubrió al 95,83 % de líneas. En Java, `analisis` (70,59 %) y
`sanidad` (88,70 %) tienen la menor cobertura de líneas entre los módulos
medidos. En Python el exportador se probó por separado al generar y validar
el JSON, por lo que la invocación CLI no se incluye en el archivo de cobertura.

**Evidencia dirigida adicional.** EC05 pasó 1/1 en PostgreSQL aislado.
El ensayo fuerza un fallo al cerrar anuncios después de actualizar el
animal y comprueba que se revierta la venta completa y se conserve el
historial. Se ejecutó por separado del informe JaCoCo de 185 pruebas.
Las tres pruebas dirigidas de OpenAPI también pasaron tras excluir los
argumentos que inyecta Spring y conservar los filtros públicos reales.

**Regresión de navegador.** Chrome headless completó por HTTPS 4/4
recorridos del arnés existente contra Spring Boot y PostgreSQL aislado:
campañas (SPEC-07), catálogo (SPEC-08), ventas (SPEC-09) y navegación UX/UI.
La preparación HTTPS también pasó. El certificado de prueba anterior
había caducado y se reemplazó en el entorno temporal. La prueba de Swagger
se ejecutó aparte en Chrome y comprobó salud HTTP 200 y acceso anónimo
rechazado a una ruta privada con HTTP 401.

### Reproducir pruebas y CI

Los comandos locales se corresponden con los del
[workflow](../../.github/workflows/integracion.yml). Se ejecutaron
`npm ci` en carpeta aislada, TypeScript, build PWA y las pruebas. La
configuración se validó con actionlint 1.7.12. GitHub Actions ejecutará tres
trabajos al publicar en `develop` o `main` y en pull requests hacia esas
ramas. Los trabajos guardan las coberturas y el resultado Java como
artefactos. El workflow carece de despliegue y de inferencia de modelos.

```powershell
# frontend/
npm.cmd ci
npm.cmd run test:coverage
npm.cmd run test:jest:coverage
npm.cmd run typecheck
npm.cmd run build

# backend/, con PostgreSQL sintético preparado según tests/e2e/README.md
mvn.cmd -B -ntp "-Dganadoconecta.postgres.integration-test=true" verify

# servicio-ia/
python -m pip install -r requirements-test.txt
New-Item -ItemType Directory -Force coverage/python | Out-Null
$env:COVERAGE_FILE = 'coverage/python/.coverage'
python -m coverage run --branch --source=ganadoconecta_ia -m unittest discover -s tests -v
python -m coverage json -o coverage/python/coverage.json
```

Reportes de frontend en `frontend/coverage/jest/` y
`frontend/coverage/vitest/`, Java en `backend/target/site/jacoco/` y
Python en `servicio-ia/coverage/python/`. [Jest y TypeScript](https://jestjs.io/docs/getting-started#using-typescript) ·
[Servicios PostgreSQL en GitHub Actions](https://docs.github.com/en/actions/tutorials/use-containerized-services/create-postgresql-service-containers)

### Primera ejecución remota

El 27 de noviembre de 2026, la integración continua del commit `3fbd3cc` en
`develop` terminó en **1 min 57 s**, con los tres trabajos y sus artefactos
confirmados en GitHub. [Ejecución #45950155833](https://github.com/edilsonz/GanadoConecta/actions/runs/45950155833)

| Trabajo | Resultado | Artefacto publicado |
|---|---|---|
| `frontend` | Aprobado | `cobertura-frontend` |
| `backend` | Aprobado | `pruebas-cobertura-java` |
| `ia` | Aprobado | `cobertura-openapi-python` |

Tras las advertencias de GitHub sobre las versiones anteriores, el workflow
actualizó `checkout` a 7.0.1, `setup-node` a 7.1.0, `setup-java` a 6.3.0,
`setup-python` a 7.1.0 y `upload-artifact` a 7.0.2. Los cinco manifiestos
declaran Node.js 24 como runtime de la acción. El entorno del proyecto
conserva Node.js 22.19.0, Java Temurin 21 y Python 3.14.5. La actualización
pasó `actionlint` 1.7.12.

### Corrección de la prueba de foco de la ficha de animales

La ejecución de `0111481` aprobó los trabajos de backend e IA. Vitest
aprobó 250 de 251 pruebas en frontend. La prueba de la ficha encontró el
encabezado «Luna» cuando el foco todavía estaba en `body`. El fallo de
Vitest dejó el trabajo sin el artefacto de cobertura de frontend.
[Ejecución #46099458060](https://github.com/edilsonz/GanadoConecta/actions/runs/46099458060)

La aparición del encabezado y la asignación de foco por `useEffect`
ocurren en momentos distintos. La prueba ahora espera con `waitFor` a que
el encabezado reciba el foco y conserva la verificación del retorno del
foco a la fila al cerrar. El cambio se limita a la prueba.

La prueba original pasó en las repeticiones directas locales. En una copia
temporal, un `MutationObserver` permitió reproducir el fallo con Vitest al
comprobar el foco al aparecer el encabezado. El foco seguía en `body`.
El mismo caso pasó al esperar la condición de foco.

Con Node.js 22.19.0 y `CI=true`, la prueba corregida pasó **12/12** veces
en Windows y **6/6** en un contenedor Linux. La regresión frontend pasó
**251/251** en Vitest y **11/11** en Jest. TypeScript y build PWA
terminaron correctamente. Se regeneraron los resúmenes JSON y LCOV
en `frontend/coverage/vitest/` y `frontend/coverage/jest/`. Queda
pendiente comprobar la publicación del artefacto en la siguiente
ejecución de GitHub Actions.

### Integración continua remota

La ejecución publicada de `ea72e80` terminó en 1 min 37 s con frontend,
backend e IA aprobados y tres artefactos. La configuración nueva añade un
Redis aislado al trabajo Java. La carga k6 se mantiene fuera de GitHub
Actions. [Ejecución #46100816517](https://github.com/edilsonz/GanadoConecta/actions/runs/46100816517)

## Segundo incremento

[Análisis PostgreSQL](rendimiento/postgresql.md) ·
[Estrategia Redis](rendimiento/redis.md) ·
[Informe y evidencias de k6](rendimiento/k6.md) ·
[Protocolo y scripts de k6](../../tests/rendimiento/README.md) ·
[Matriz de calidad](matriz-calidad.md)

Las migraciones V18–V19 agregan la revisión del catálogo. PostgreSQL sigue
siendo la fuente de verdad. No se agregaron índices en esta iteración.
Los diagramas C4 de contenedores y componentes y el despliegue local
se actualizaron a partir de sus fuentes Draw.io.

La regresión Java con PostgreSQL 17.12 y Redis 8.4.6 aislados pasó
**190/190**, con cuatro pruebas nuevas de caché. JaCoCo registró 90,97 %
de líneas y 68,15 % de ramas. En Windows el informe se generó con un
archivo de ejecución nuevo y `-Dsurefire.exitTimeout=120` después de
que el límite anterior interrumpiera su escritura. Se reutilizan las
verificaciones vigentes de frontend, PWA y FastAPI del incremento anterior.
[Cobertura completa por herramienta](cobertura.json)

### Cinco entregables del docente

| Entregable | Evidencia |
|---|---|
| 1. Código fuente según la arquitectura | [Arquitectura contrastada](#concordancia-con-el-sprint-1), [C4](../04-modelo-c4/Nivel2-DiagramaContenedores.md) y [puerto de caché](../../backend/src/main/java/pe/ganadoconecta/modulos/catalogo/aplicacion/CatalogoAnuncios.java). |
| 2. Primera versión funcional | [Arranque local](../../README.md#ejecutar-la-primera-base-local), [auditoría funcional](../AUDITORIA-FUNCIONAL.md) y resultados de navegador. |
| 3. API mediante Swagger | [Capturas reales y contratos Spring y FastAPI](openapi/README.md). |
| 4. Jest y cobertura | [Pruebas y cobertura](#pruebas-y-cobertura-local), [informes por herramienta](cobertura.json) y [workflow](../../.github/workflows/integracion.yml). |
| 5. Atributos de calidad | [Matriz EC01–EC13](matriz-calidad.md), planes, pruebas Redis y el alcance medido con k6. Los estados parciales y pendientes figuran en la matriz. |

### Demostración

1. Arrancar la PWA y el backend según el [README](../../README.md), con
   PostgreSQL local. Usar el [Compose aislado](../../tests/rendimiento/README.md)
   para demostrar rendimiento con datos sintéticos.
2. Activar el perfil `openapi` para consultar [Swagger y sus contratos](openapi/README.md).
3. En `frontend/`, ejecutar `npm.cmd run test:jest:coverage` y abrir
   `coverage/jest/lcov-report/index.html`. Revisar los artefactos de la
   [ejecución publicada](https://github.com/edilsonz/GanadoConecta/actions/runs/46100816517).
4. Mostrar `EXPLAIN (ANALYZE, BUFFERS)` y la prueba de caída y recuperación
   de Redis según el [protocolo aislado](../../tests/rendimiento/README.md).
5. Ejecutar k6 en modo `smoke` y contrastar EC01 con EC03. El primero
   tiene un umbral aprobado para 100 VU. El segundo mide la escalada y
   sus límites en esta estación.
