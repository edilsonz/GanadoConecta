# GanadoConecta — Ruta de implementación SDD alineada con la rúbrica

**Estado:** plan de trabajo, no implementación ni evidencia de aprobación. Elaborado a partir de `GanadoConecta-main (2).zip`, el paquete v2 y la rúbrica de 16 criterios facilitada por el usuario.

## 1. Distinción fundamental

- **Fases del proyecto:** ordenan *cuándo* trabajamos en cada módulo e infraestructura.
- **SDD (Spec-Driven Development):** define *cómo* abordamos cada funcionalidad: especificación revisada → diseño técnico → tareas → implementación → pruebas → evidencia → revisión.
- No es necesario generar de nuevo las especificaciones existentes. Reutilizar `specs/plantilla-spec.md` y `AGENTS.md`, ampliándolas con un plan, tareas y evidencias ejecutadas por funcionalidad.
- No confundir una SPEC redactada con una funcionalidad implementada o comprobada.

## 2. Rúbrica: 16 criterios y evidencia que debemos entregar

| N.º | Criterio docente | Evidencia concreta prevista | Momento |
|---:|---|---|---|
| 1 | Identificación del problema y requerimientos | Planteamiento del problema, actores, historias de usuario y RF/AC actualizados con trazabilidad | Fase 0 |
| 2 | Análisis y diseño del sistema | Casos de uso, modelos de datos, reglas de negocio, flujos, interfaces propuestas y decisiones | Fase 0 y por módulo |
| 3 | Arquitectura de bajo y alto nivel | C4 niveles 1–3 actualizados; módulos, dependencias, API, modelo físico y despliegue | Fases 0–1 |
| 4 | Desarrollo basado en SDD | SPECS aprobadas, planes, tareas, código y criterios de aceptación comprobados; evidencia en repositorio | Todas las fases |
| 5 | Pruebas unitarias e integración | Código de pruebas + resultados automatizados para dominio, API, PostgreSQL y permisos | Por funcionalidad |
| 6 | Pruebas End-to-End | Flujos reales de usuario automatizados en navegador con Playwright, incluso desconexión/reconexión | Desde primer flujo completo |
| 7 | Interfaz y experiencia de usuario | Interfaz verde/blanca responsiva, pruebas en celular/escritorio, validación de tareas y accesibilidad | Desde primer frontend |
| 8 | Escalabilidad del frontend y backend | Diseño modular, paginación, recursos estáticos optimizados, mediciones y resultados al incrementar carga | Diseño temprano; verificar al final |
| 9 | Escalabilidad de base de datos | Índices, migraciones, planes de consulta y mediciones con crecimiento de datos; sin afirmar resultados no medidos | Diseño temprano; verificar al final |
| 10 | Dominio/subdominio, DNS y CDN | Despliegue accesible por dominio o subdominio, HTTPS, DNS documentado y CDN para recursos públicos cuando corresponda | Fase de despliegue |
| 11 | Implementación caché | Redis para el catálogo público: TTL, invalidación, seguridad y comportamiento sin Redis, con pruebas | Fase catálogo |
| 12 | CI/CD | GitHub Actions para compilar, ejecutar pruebas y desplegar según entorno; logs de ejecuciones | Desde base del proyecto |
| 13 | Pruebas de carga k6 | Scripts k6, configuración, resultados y conclusiones; contrastar AC01: p95 ≤ 2 s con 100 usuarios concurrentes en consultas definidas | Integración y preentrega |
| 14 | Código fuente y base de datos en GitHub | Código, migraciones SQL/Flyway, esquema, scripts de datos de prueba y README para levantar la BD; jamás credenciales ni datos reales | Desde fase 1 |
| 15 | Informe final | Documento con metodología, arquitectura, evidencias, métricas, resultados y limitaciones honestas | Continuo; cerrar al final |
| 16 | Presentación y dominio del tema | Demostración del flujo completo, explicación de decisiones, SDD, pruebas y preguntas de defensa | Fase final |

**Estado verificado:** el ZIP original contiene documentación de requerimientos, diseño, arquitectura C4 y plantilla SDD; las modificaciones propuestas v2 aún necesitan revisión e integración. No hay evidencia de software implementado, pipelines ejecutados, despliegue o resultados de pruebas dentro de este ZIP. La presencia de un ZIP tampoco verifica por sí misma un repositorio remoto de GitHub.

## 3. Ruta de trabajo revisada

### Fase 0 — Consolidación documental y SDD (criterios 1–4)
- Revisar material actual con la rúbrica y los acuerdos del proyecto.
- Actualizar RF/AC/ADR, C4, actores, historias de usuario y modelo de datos, marcando propuestas pendientes de aprobación.
- Conservar `AGENTS.md` y `specs/plantilla-spec.md` como base del proceso SDD.
- Crear especificación y criterios de aceptación del **primer flujo vertical**: propietario/familia autorizada → registrar animal → verlo en «Mi ganado» → guardarlo sin conexión → sincronizar sin duplicados.
- Para cada SPEC mantener: `specs/SPEC-XX-nombre-descriptivo/spec.md`, `plan.md`, `tasks.md`, pruebas y evidencia.

### Fase 1 — Proyecto ejecutable y automatización básica (criterios 4, 7, 12, 14)
- Crear React responsivo/PWA, Spring Boot, PostgreSQL y configuración local. Mantener FastAPI preparado, sin requerir IA desde el primer día.
- Crear migraciones Flyway y ejemplos de variables de entorno sin secretos.
- Introducir GitHub Actions desde el comienzo (build/lint/tests), y configurar ramas/commits claros.
- Incluir componentes de UI compartidos y estado de conectividad.

### Fase 2 — Primer flujo completo SDD (criterios 4–7 y 14)
- Roles/ganadería familiar, permisos, registro flexible del animal y listado «Mi ganado».
- Implementar IndexedDB, cola de sincronización, idempotencia en API y prevención de duplicados.
- Escribir pruebas unitarias, integración y E2E de crear animal online/offline y control de permisos.
- Conservar reportes reales de pruebas y capturas de la experiencia móvil y escritorio.

### Fase 3 — Gestión ganadera y seguimiento sanitario (criterios 4–7)
- Registro rápido y masivo, nacimientos, genealogía, seguimiento de peso e historial.
- Campañas de vacunación, baños y desparasitaciones generales; confirmar aplicaciones por animal.
- Controles individuales cuando proceda; calendarios y recordatorios colectivos/individuales.
- Trabajar SPEC por SPEC, pruebas y evidencia por módulo, sin promesas veterinarias infundadas.

### Fase 4 — IA y catálogo no transaccional (criterios 4–7, 11)
- FastAPI para análisis general del ganado y asistente que use consultas autorizadas y fuentes confiables, sin diagnosticar ni prescribir.
- Catálogo de ganado: fotografías, información básica, genealogía expresamente autorizada y contacto.
- Cerrar anuncio al marcar venta sin transferencia automática entre cuentas.
- Integrar Redis exclusivamente para el catálogo público con TTL, invalidación y fallback a PostgreSQL; comprobar fallos.

### Fase 5 — Escalabilidad, calidad y despliegue (criterios 6, 8–13)
- Medir rendimiento del frontend, backend y BD; añadir paginación, índices y optimizaciones justificadas.
- Probar flujos End-to-End globales, incluso reconexión, roles, ventas y privacidad.
- Preparar k6 con escenarios documentados y compararlos con `AC01` (100 concurrentes, p95 ≤ 2 segundos; excluir IA y transferencia de fotografías conforme al documento original).
- Configurar hosting, HTTPS, DNS, dominio/subdominio y CDN para contenidos estáticos/públicos, sin cachear historiales privados.
- Ampliar CI/CD hacia despliegue de entorno de prueba y producción según infraestructura disponible.

### Fase 6 — Entrega y defensa (criterios 14–16)
- Verificar repositorio GitHub (código, migraciones, documentación de instalación) y ejecutar instalación desde cero.
- Preparar informe técnico con resultados reales, capturas y problemas conocidos.
- Ensayar demo: registro → campañas → offline → sincronización → IA → catálogo, mostrando pruebas y decisiones arquitectónicas.

## 4. Ciclo SDD de cada funcionalidad

1. **SPEC:** qué se espera y por qué; RF/HU/AC, actores, reglas, errores, permisos y criterios Given-When-Then.
2. **PLAN:** entidades, endpoints, contratos, componentes, persistencia, migraciones, controles de seguridad y observabilidad.
3. **TASKS:** trabajos pequeños, ordenados y con verificaciones.
4. **IMPLEMENT:** Codex programa lo descrito, sin ampliar silenciosamente el alcance.
5. **TEST:** pruebas unitarias + integración + E2E según escenario; carga para los endpoints relevantes.
6. **EVIDENCE:** dejar comandos, resultados reales, correcciones, capturas/logs y vínculos a commits; revisar con los requisitos originales.

Ejemplo de SPEC inicial: `SPEC-01 Registro flexible del ganado`.
- Dado un integrante editor autorizado, cuando registra un animal con sexo válido, entonces se crea una ficha con identificador inmutable y nombre/arete opcionales.
- Dado un lector, cuando intenta modificar un animal, entonces el backend rechaza la acción.
- Dado un dispositivo sin internet con datos previamente preparados, cuando registra un vacuno, entonces se guarda localmente y queda pendiente; al reconectar se sincroniza exactamente una vez, incluso con reintentos.
- Dado un usuario de otra ganadería, cuando intenta leer esa ficha privada, entonces se rechaza el acceso.

## 5. Decisiones de infraestructura a concretar durante la implementación

No bloquean el inicio de la Fase 0: versiones exactas de herramientas, servicio de archivos/fotografías, fuente y modelo del asistente, alojamiento/despliegue, nombre del dominio y organización de ambientes. Sí deben resolverse antes de implementar o desplegar la dependencia correspondiente. No introducir credenciales o datos reales en GitHub. Revisar recursos/costos del despliegue y expectativas de la rúbrica con el profesor.

## 6. Primer encargo recomendado a Codex

> Lee `AGENTS.md`, `README.md`, todos los documentos de `docs/`, la propuesta v2 en `docs/05-preparacion-implementacion/`, `specs/plantilla-spec.md` y este plan de rúbrica. No implementes toda la aplicación.
>
> 1. Elabora matriz de trazabilidad para los 16 criterios de evaluación y marca evidencia existente, pendiente y riesgo.
> 2. Actualiza de forma coherente RF, AC, ADR y diagramas C4 sin inventar documentos aprobados ni resultados de pruebas.
> 3. Prepara la primera SPEC, su PLAN y TASKS para registro flexible de ganado con roles familiares y sincronización offline.
> 4. Define criterios de aceptación verificables y pruebas unitarias, de integración y E2E para ese flujo.
> 5. Diseña la estructura mínima de frontend, backend, migraciones y CI; no generes funcionalidades adicionales en esta tarea.
> 6. Devuelve archivos modificados, decisiones pendientes, contradicciones detectadas y comandos verificables. No declares ninguna prueba como aprobada sin ejecutarla.

## Referencias técnicas externas (opcionales)

- GitHub Spec Kit: https://github.github.com/spec-kit/index.html
- CI con GitHub Actions: https://docs.github.com/en/actions/get-started/continuous-integration
- Playwright E2E: https://playwright.dev/docs/intro
- Umbrales k6: https://grafana.com/docs/k6/latest/using-k6/thresholds/
