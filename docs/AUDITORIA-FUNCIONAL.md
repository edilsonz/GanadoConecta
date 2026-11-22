# Auditoría funcional de GanadoConecta

**Fecha:** 21 de noviembre de 2026. **Base auditada:** `97f68b8` en `develop`,
confirmada mediante consulta de solo lectura a `origin/develop`. Alcance: 22 RF,
23 HU, tres actores, diez módulos y los 16 criterios docentes. Las fuentes de
alcance son los [requisitos](01-analisis-de-sistema/03-requisitos-funcionales.md),
las [historias y sus criterios originales](01-analisis-de-sistema/02-historias-de-usuario.md),
los [actores](01-analisis-de-sistema/01-actores.md) y los [contratos SDD](../specs/README.md).
La [matriz funcional](MATRIZ-DE-TRAZABILIDAD.md) enlaza cada requisito con su SPEC.

**Completo** exige cobertura suficiente del alcance aprobado. **Parcial** indica
una parte operativa con brechas importantes. **Pendiente** identifica el flujo que
todavía carece de disponibilidad operativa. **No verificado** se reserva para un
estado sin evidencia suficiente. La auditoría evalúa el producto local, no otorga
calificación docente y conserva pendientes las decisiones de negocio abiertas.

| Inventario | Completo | Parcial | Pendiente | No verificado | Total |
|---|---:|---:|---:|---:|---:|
| Requisitos | 10 | 12 | 0 | 0 | 22 |
| Historias | 11 | 11 | 1 | 0 | 23 |

El total de cada inventario es la suma de sus cuatro estados. Un estado parcial
incluye las funcionalidades sin implementar y las comprobaciones específicas
que faltan, aunque se haya podido verificar el resto del requisito.

## Matriz RF01–RF22

Los códigos A–M corresponden a los recorridos y combinaciones de pruebas descritos
en la sección de evidencias. PostgreSQL conserva los registros confirmados, salvo
la cola local de RF17 y los archivos de foto gestionados por su adaptador.

| RF | Estado | Alcance comprobado y capas | Evidencia y brecha |
|---|---|---|---|
| RF01 | Parcial | React y API de identidad registran la cuenta, inician sesión, renuevan y cierran. PostgreSQL conserva hashes Argon2id, sesiones y verificadores. | A, `RegistroUsuarioPostgresIntegrationTest`, `AutenticacionHttpPostgresIntegrationTest`. Falta actualizar los datos personales y de contacto del titular. |
| RF02 | Parcial | Alta, listado y ficha web/API; UUID interno, sexo obligatorio, seis categorías e identificación externa opcional. | D, `RegistrarAnimalHttpPostgresIntegrationTest`, `ConsultarAnimalesHttpPostgresIntegrationTest`. No hay flujo de edición con autorización, versión y manejo de conflictos. |
| RF03 | Completo | La ficha consulta historiales sanitarios, reproductivos y productivos mediante APIs privadas y persistencia por animal. Peso es el indicador productivo actualmente concretado. | E, F, G y sus integraciones. Otros indicadores se siguen en RF06. |
| RF04 | Parcial | Vacunas, desparasitación y tratamientos por animal con fechas y observaciones. Se permite programar una próxima actividad indicada. Baño registrado al confirmar campaña. | E, `VacunacionesPostgresIntegrationTest`. Falta la captura de baño individual desde el formulario de historial. |
| RF05 | Parcial | Monta, inseminación, evaluaciones, partos y crías persistentes. Fecha probable introducida por la persona cuando corresponde. | F, `ReproduccionPostgresIntegrationTest`. La estimación automática requiere acordar datos y método antes de programarla. |
| RF06 | Parcial | Alta de peso fechado en kg, medidas positivas con precisión acordada, historial, variación y gráfico. | G, `ReproduccionPostgresIntegrationTest`, `PesosPanel.test.tsx`. Falta definir e incorporar otros datos productivos. |
| RF07 | Parcial | Agenda independiente de IA para actividades individuales y colectivas sanitarias, reproductivas y productivas. Avisos de hoy y vencidos. | E, SPEC-07 y sus pruebas. Anticipación y canal externo requieren decisión e integración. Las futuras se consultan en el calendario. |
| RF08 | Parcial | Reportes y análisis descriptivos en SQL con datos autorizados, período y variación. El flujo conversacional local usa inferencia en RF19. | K/L, `AnalisisPostgresIntegrationTest`. Falta un análisis de patrones históricos mediante modelo con evaluación propia. |
| RF09 | Parcial | Avisos internos por fecha en Lima, ligados al UUID de la actividad. La consulta y las recargas no crean nuevas alertas clínicas. | E/L, `AnalisisPostgresIntegrationTest`. Falta aprobar reglas de tendencias y métricas, además de verificar su implementación. |
| RF10 | Completo | Reportes sanitarios, reproductivos y de peso con filtros por ganadería, fecha y animal, gráfica y tabla. | K y `ReportesPostgresIntegrationTest` con fechas inclusivas, ceros, conciliación SQL, revocación y aislamiento. |
| RF11 | Parcial | Administración de plataforma mediante permiso explícito: lista, filtros, cursores, estadísticas y auditoría. | J y `AdministracionPostgresIntegrationTest`. Faltan procedimientos aprobados y ejecutables para alta/revocación del permiso y cambios de cuenta. |
| RF12 | Completo | Propietario crea y edita el anuncio con foto, precio, características, zona y consentimiento de contacto. API pública proyecta campos permitidos. Fotos por directorio privado. | H y `CatalogoPostgresIntegrationTest`. El despliegue necesitará configurar un adaptador de fotos duradero. |
| RF13 | Completo | Catálogo anónimo de anuncios activos, filtros de categoría, raza, sexo, ubicación y precio, con paginación. | H verifica catálogo público, foto, sexo, ubicación y paginación. `CatalogoPostgresIntegrationTest` comprueba también filtros combinados y privacidad. |
| RF14 | Completo | El propietario publica, pausa, republica o retira conforme al estado y versión. La publicación deja de estar visible tras pausa o retiro. | H y `CatalogoPostgresIntegrationTest`. |
| RF15 | Completo | Venta registrada en PostgreSQL mediante una transacción, estado vendido y cierre de anuncios activos o pausados. Conserva ficha e historial. | I y `CatalogoPostgresIntegrationTest` con transacción, reenvío y rechazo de publicación de animal vendido. La venta queda en la ganadería original. |
| RF16 | Parcial | Bandeja y retiro auditado por permiso específico. El modo de recepción autenticada se ha probado en aislamiento y está desactivado en la configuración predeterminada. | I/J y `CatalogoPostgresIntegrationTest`. Hace falta una decisión sobre quién reporta y los controles de abuso antes de abrir el flujo. |
| RF17 | Parcial | PWA, contexto offline optativo, cola IndexedDB para animales y ciertos eventos sanitarios, reintentos conservando UUID e idempotencia. | M comprueba navegando el alta local y sincronización manual. Vitest cubre sincronización automática, doble ventana simulada, expiración, conflictos y cola sanitaria. Faltan consulta privada confirmada offline y cobertura E2E de permisos/versiones y sanidad. |
| RF18 | Completo | Nacimientos, partos, ascendencia y descendencia con auditoría de correcciones. La proyección pública exige consentimiento y excluye identificación privada. | F/H, `ReproduccionPostgresIntegrationTest` y `CatalogoPostgresIntegrationTest`. La genealogía incompleta conserva progenitores desconocidos. |
| RF19 | Parcial | Chat en español de solo lectura sobre peso, sanidad, reproducción y agenda propios, con citas y abstención. Spring filtra, FastAPI transporta y Ollama infiere localmente. | L, `AsistentePostgresIntegrationTest` y Python. Falta corpus revisado y orientación general con fuentes, y evaluación de precisión con más preguntas. |
| RF20 | Completo | Alta flexible individual y lote mediante tabla online. Revisión previa, envío por fila, errores y persistencia sin duplicar. | D, `AltaMasivaPanel.test.tsx` e integración de alta individual. Los archivos externos y la cola offline de lotes quedan fuera de SPEC-06. |
| RF21 | Completo | Cuentas propias, invitación ligada al correo, roles vigentes, revocación y nueva comprobación de acceso en sincronización. | C/M, `AccesoFamiliarPostgresIntegrationTest`, integración de ganadería y sincronización. La transferencia de titularidad queda fuera del requerimiento aprobado. |
| RF22 | Completo | Seguimiento individual y campañas de vacunación, desparasitación y baños. Cierre confirma participantes concretos y enlaza sus eventos, con reenvío idempotente. | E y `VacunacionesPostgresIntegrationTest`, con reenvío, conflicto y exclusión de animales no atendidos. |

## Matriz HU01–HU23

Los resúmenes preservan el actor y las relaciones expresas de la fuente. La primera
parte de HU01 se aplica a la cuenta usada por el productor; la actualización de
otros perfiles sigue fuera de la autorización. HU16 conserva la propuesta de
autenticación pendiente de confirmar.

| HU y objetivo original | Actor | RF | Estado | Criterio comprobado, prueba y brecha |
|---|---|---|---|---|
| HU01 · Acceder y mantener datos personales | Usuario | RF01 | Parcial | Alta sin correo duplicado, credenciales inválidas rechazadas y cierre/recarga en A e integración. Edición del perfil propio pendiente. |
| HU02 · Registrar, editar y consultar animales | Productor | RF02 | Parcial | D confirma alta y ficha de la ganadería; C y backend verifican rechazo ajeno. Falta editar. |
| HU03 · Consultar antecedentes por animal | Productor | RF03 | Completo | E/F/G y backend muestran historiales de sanidad, reproducción y peso del animal autorizado. |
| HU04 · Registrar controles sanitarios | Productor | RF04 | Parcial | E y backend asocian tipo, fecha, observaciones y programación. Baño individual sin formulario. |
| HU05 · Seguimiento reproductivo y fecha probable | Productor | RF05 | Parcial | F e integración comprueban monta, evaluaciones, parto y crías. Fecha informada, estimación automática pendiente. |
| HU06 · Registrar y consultar evolución productiva | Productor | RF06 | Parcial | G y backend comprueban fechas y delta, y rechazan peso no positivo. Falta definir y cubrir los demás indicadores. |
| HU07 · Agenda y recordatorios | Productor | RF07 | Parcial | E/L muestran agenda, avisos internos y operación independiente de IA. Anticipación y canal externo pendientes. |
| HU08 · Analizar patrones con IA | Productor | RF08 | Parcial | L/K verifican SQL autorizado y degradación. Falta método para identificar y validar patrones mediante inferencia. |
| HU09 · Advertir registros o tendencias | Productor | RF09 | Parcial | L verifica avisos con fecha y origen; la agenda usa reglas aprobadas. Falta criterio de tendencias. |
| HU10 · Consultar reportes de gestión | Productor | RF10 | Completo | K verifica indicadores de los tres ámbitos, filtros, gráfico, ausencia de datos y acceso vigente. |
| HU11 · Gestionar usuarios y consultar estadísticas | Administrador | RF11 | Parcial | J verifica permiso exclusivo, consultas y auditoría. Gestión de cuentas pendiente. |
| HU12 · Crear y editar anuncios autorizados | Productor | RF12 | Completo | H y backend verifican publicación, foto y proyección pública. Al editar se exigen propietario, versión y valores válidos. |
| HU13 · Consultar y filtrar anuncios | Visitante o interesado | RF13 | Completo | H y backend verifican catálogo anónimo, filtros, paginación y estados de visibilidad. |
| HU14 · Publicar, pausar o retirar | Productor | RF14 | Completo | H y backend comprueban transiciones y exclusión pública. |
| HU15 · Registrar venta y mantener antecedentes | Productor | RF15 | Completo | I y backend verifican cierre de anuncios, historial y reenvío de la misma operación. |
| HU16 · Reportar un anuncio | Usuario, autenticación propuesta | RF16 | Pendiente | El modo autenticado experimental tiene pruebas de integración. La recepción en el producto está deshabilitada hasta aprobar la política. |
| HU17 · Revisar y retirar anuncios | Administrador autorizado para moderar | RF16 | Completo | I/J y backend verifican permiso independiente, retiro, exclusión anónima y auditoría. El reporte se preparó como fixture. |
| HU18 · Consultar y registrar sin conexión | Integrante autorizado | RF17 | Parcial | M confirma cola, reconexión y persistencia. Quedan consulta privada offline y casos de expiración/revocación y sanidad en Chrome. |
| HU19 · Nacimientos, parentescos y genealogía | Productor autorizado | RF18, RF05 | Completo | F/H prueban genealogía y proyección autorizada. La estimación de parto corresponde a HU05. |
| HU20 · Conversación con datos y orientación | Integrante autorizado | RF19, RF08 | Parcial | L e integración verifican acceso autorizado, citas y abstención. Corpus general pendiente. |
| HU21 · Alta flexible individual o masiva | Productor autorizado | RF20, RF02 | Completo | D comprueba alta sin arete, registro por lote, error por fila y persistencia. Edición se sigue en HU02. |
| HU22 · Autorizar acceso familiar con cuentas propias | Titular de ganadería | RF21, RF01 | Completo | C y backend prueban invitación, lector/editor, exclusión de terceros y pérdida de acceso tras revocación. |
| HU23 · Conservar historial de campañas por animal | Integrante autorizado | RF22, RF04 | Completo | E y pruebas PostgreSQL confirman solo el subconjunto atendido y evitan duplicar el evento al reenviar. |

## Actores y módulos

| Actor | Permisos demostrados | Límite |
|---|---|---|
| ACT01 · Productor e integrantes autorizados | Titular crea una ganadería e invita. Lector consulta y editor escribe según membresía. La autorización se verifica en servidor. | C y M. Seleccionar una ganadería en React no concede permisos. Un colaborador revocado pierde acceso remoto. |
| ACT02 · Visitante o interesado | H/I permiten ver el catálogo activo, fotografías y proyección autorizada. Solicitar historial anónimo devuelve HTTP 401. | Recepción de reportes pendiente. Negociación, pagos y logística quedan fuera. |
| ACT03 · Administrador | J/I verifican administración y moderación con permisos independientes y evidencias de auditoría. | El permiso de plataforma no concede acceso a historiales de una ganadería ajena. No se añadió rol veterinario. |

| Módulo de la hoja de ruta | RF principales | Conclusión |
|---|---|---|
| 1. Autenticación y perfiles | RF01 | Acceso probado. Falta perfil editable. |
| 2. Ganaderías y acceso familiar | RF21 | Roles online y revalidación de sincronización comprobados. |
| 3. Registro individual y masivo | RF02, RF20 | Ambas altas disponibles. Falta edición versionada. |
| 4. Sanidad, calendario y recordatorios | RF03, RF04, RF07, RF22 | Historial y campañas operativos. Baño individual y entrega externa pendientes. |
| 5. Reproducción, partos y genealogía | RF05, RF18 | Hechos y parentescos disponibles. Estimación de parto pendiente. |
| 6. Peso e historial productivo | RF06 | Peso completo como indicador inicial. Faltan otros indicadores. |
| 7. Catálogo, anuncios y ventas | RF12–RF16 | Catálogo y venta operativos. Falta habilitar la recepción aprobada de reportes. |
| 8. Administración | RF11 | Consultas, indicadores y auditoría operativos. Gestión de estados pendiente. |
| 9. Reportes | RF10 | Reportes sanitarios, reproductivos y productivos verificados. |
| 10. IA y alertas | RF08, RF09, RF19 | Estadísticas, avisos por fecha e inferencia de preguntas propias disponibles. Faltan patrones, tendencias y corpus. |

## Criterios docentes

De los 16 criterios, 4 tienen evidencia validada, 7 tienen evidencia parcial,
4 están pendientes y 1 requiere revisión manual.

| N.º | Criterio | Evidencia | Pendiente |
|---:|---|---|---|
| 1 | Problema y requerimientos | Validada | Revisión con usuarios y docente. Este informe contrasta todas las fuentes. |
| 2 | Análisis y diseño | Parcial | Existen casos de uso, contratos y persistencia reproducible. Acordar el diseño de las brechas funcionales. |
| 3 | Arquitectura de alto y bajo nivel | Parcial | Monolito, Spring, PostgreSQL, PWA y FastAPI locales comprobados. Actualizar los C4 y despliegue respecto de V17 y RF19 cuando se versionen los diagramas de cierre. |
| 4 | SDD | Validada | Especificaciones y trazabilidad por incremento en Git. Formalizar las funcionalidades aún abiertas en sus SPEC. |
| 5 | Pruebas unitarias e integración | Validada | Suites ejecutadas con PostgreSQL aislado. La cobertura futura sigue el avance funcional. |
| 6 | End to End | Parcial | Recorridos HTTPS con PostgreSQL y Ollama local. Faltan casos offline con revocación en tránsito, sanitaria, doble ventana real y recarga completamente desconectada. |
| 7 | Interfaz y experiencia | Parcial | Evidencia a 360, 768 y 1440 px, teclado y ARIA en rutas cubiertas. Faltan lector de pantalla, dispositivos físicos y revisión con productores. |
| 8 | Escalabilidad de frontend y backend | Parcial | Módulos, paginación y separación del motor de IA. Faltan mediciones con concurrencia, recursos y rendimiento de consultas. |
| 9 | Escalabilidad de base de datos | Parcial | Flyway V1–V17, índices y restricciones reproducidos. Faltan planes y costos con datos representativos. |
| 10 | Dominio, DNS y CDN | Pendiente | HTTPS por loopback verificado. Falta publicar y configurar la infraestructura externa. |
| 11 | Caché | Pendiente | ADR de Redis disponible. La caché del app shell PWA no cubre Redis público, invalidación ni recuperación. |
| 12 | CI/CD | Pendiente | Comandos locales disponibles. Falta pipeline ejecutado. |
| 13 | Carga con k6 | Pendiente | Objetivo AC01 definido. Faltan scripts y pruebas de animales y catálogo con caché fría, poblada e indisponible. |
| 14 | Fuente y base en GitHub | Validada | Se comprobó la base `97f68b8` publicada y se reprodujo el esquema con Flyway en una base nueva. Los datos reales se conservan fuera de Git. |
| 15 | Informe final | Parcial | Informe de auditoría y evidencia listos. Falta integrar el informe institucional con resultados de despliegue y rendimiento. |
| 16 | Presentación y dominio | Revisión manual | Guía, escenarios sintéticos y demostración local reproducibles. La defensa ante el docente requiere verificación humana. |

## Discrepancias de documentación y diseño

- La [arquitectura inicial](02-arquitectura-software/arquitectura-inicial.md) y la
  [fuente de diagramas](02-arquitectura-software/diagramas/README.md) conservan la
  referencia `c961a0b`, esquema V1–V16 y FastAPI pendiente. El código auditado
  migró hasta V17 y expone RF19 local como integración opcional. Se conservaron
  las fuentes Draw.io y las figuras existentes.
- Algunas secciones de [README](../README.md#estado-del-proyecto) y de
  [tareas de SPEC-01](../specs/SPEC-01-registro-ganado-offline/tasks.md) describen
  entregas anteriores. El ámbito actual incluye membresías, animales y colas que
  esas secciones todavía marcan pendientes. El informe registra la evidencia vigente.
- El objetivo arquitectónico de trabajos persistentes en segundo plano para IA
  y notificaciones permanece abierto. RF19 local usa una consulta síncrona y acotada.
- Fecha probable de parto, cambios administrativos, reglas de tendencias y
  audiencia de reportes requieren decisiones, según sus SPEC. El asistente de
  registros propios no cubre el análisis de patrones ni la orientación general.

## Pruebas y evidencias

La auditoría usó Node.js 22, Chrome headless por HTTPS con perfil temporal,
Spring Boot 3.5.16, PostgreSQL Docker 17.12 en una base nueva aislada y Flyway
V1–V17. FastAPI usó Python 3.14 y Ollama 0.40.17 con Qwen3 local. Las fotos se
restringieron a un directorio de pruebas y se mantuvo desactivada la recepción
de reportes. Los registros de prueba fueron exclusivamente sintéticos.

| Verificación | Resultado ejecutado |
|---|---|
| Backend Maven `verify` | **180/180**, 59 unitarias y de base, 121 de integración PostgreSQL. Sin fallos ni omisiones. |
| Frontend Vitest sobre `97f68b8` | **247/247** en 38 archivos. |
| Frontend dirigido tras corregir el cierre | **23/23** en tres archivos de cuenta y aplicación. Solapa la suite de 247 y añade un caso. |
| TypeScript y build PWA | Correctos. `npm run build` incluye `tsc -b` y genera el app shell. |
| FastAPI | **4/4** del adaptador Python y `pip check` correcto. |

### Recorridos Chrome A–M

| Recorrido | Resultado comprobado | Alcance que queda por cubrir |
|---|---|---|
| A. Cuenta y autorización | Registro, login HTTPS, restauración al recargar, cierre y consulta privada posterior con HTTP 401. | Edición de perfil y verificación de nuevos datos de contacto. |
| B. Ganaderías | Creación con propietario inicial, selección y listado tras recargar. | La selección local sigue siendo temporal. |
| C. Familia | Invitación aceptada por su destinatario, lectura inicial, escritura denegada como lector, rol editor aplicado, escritura autorizada y HTTP 403 tras revocar. El tercero sin membresía recibió HTTP 403. | El ciclo de membresía online está cubierto. |
| D. Alta individual y por lote | Validación de sexo, alta flexible y lote con una fila válida y otra inválida. Una sola alta de la fila válida tras recargar. | Edición versionada dentro de RF02. |
| E. Sanidad y agenda | Campaña de vacunación por la interfaz y desparasitación por API autenticada desde Chrome. En cada caso, un animal de dos recibió atención. El historial del atendido conservó un solo evento al repetir. | Baño sanitario individual y recordatorios por canal externo. |
| F. Reproducción y genealogía | Monta, evaluación, parto con cría vinculada y parentesco visible. | Validación E2E de correcciones de parentesco y estimación aprobada de parto. |
| G. Pesajes | Dos pesajes, variación de 15,25 kg y gráfica. | Otros indicadores productivos. |
| H. Catálogo | Foto sintética, navegación anónima, filtros, paginación y desaparición al pausar. El historial privado devolvió HTTP 401. | El adaptador de fotos debe configurarse en el entorno de publicación. |
| I. Ventas y moderación | Venta conservó el historial y cerró ambos anuncios. En un caso independiente, un moderador sintético retiró otro anuncio, se comprobó su exclusión pública y su auditoría en PostgreSQL. | Recepción de reportes en el producto desactivada; el reporte de moderación fue un dato sintético preparado en la base. |
| J. Administración | Indicadores contrastados con SQL, listado, búsqueda, filtros, cursor, auditoría, y HTTP 403 sin permiso. El permiso administrativo fue independiente de moderación. | Mutaciones sobre cuentas y administración de permisos de plataforma. |
| K. Reportes | Agregados sanitarios y reproductivos, gráfica de peso, fechas incluyentes, vacío real, permisos y aislamiento. Los conteos se contrastaron con SQL. | Prueba de rendimiento con volumen representativo. |
| L. Análisis, alertas e IA | Análisis descriptivo, cinco consultas reales a Qwen3 en español con referencias, abstención fuera del ámbito y para datos insuficientes. Alerta visible el día previsto y retirada al completar sin crear evento clínico. Con Ollama detenido, la API devolvió HTTP 503 y el análisis descriptivo siguió disponible. | Análisis de patrones, alertas por tendencias y orientación con corpus revisado. |
| M. Offline | Chrome sin red, alta pendiente en IndexedDB, recuperación de conexión, sincronización manual y reenvío con el identificador original. La recarga confirmó un solo animal y conservó el comprobante. | Recarga totalmente desconectada, revocación en tránsito, sanidad y dos ventanas reales. |
| UX transversal | 360, 768 y 1440 px. Navegación, etiquetas y controles de formularios, pestañas con asociación ARIA, teclado y foco en los recorridos cubiertos, estados vacíos, gráficas y ausencia de desbordamiento horizontal. | Lector de pantalla, dispositivos físicos y sesiones con productores. |

**28 casos Chrome aprobados, 0 fallidos y 0 bloqueados en el alcance ejecutado.**
El total suma 4 casos de campañas, catálogo, venta y navegación, 10 de recorridos
de productor, 8 de administración, reportes, moderación e interfaz, y 6 de IA e
interfaz. Se excluyen la preparación HTTPS y los casos repetidos. Las rutas de
ejecución están en la [guía Chrome](../tests/e2e/README.md). Los resultados y
capturas seleccionadas usan exclusivamente datos sintéticos.

[Resultados E2E](../tests/e2e/evidencia/auditoria-2026-11-21/resultado.json) ·
[Alta masiva](../tests/e2e/evidencia/auditoria-2026-11-21/lote.png) ·
[Genealogía](../tests/e2e/evidencia/auditoria-2026-11-21/genealogia.png) ·
[Pendiente offline](../tests/e2e/evidencia/auditoria-2026-11-21/pendiente-offline.png) ·
[Moderación](../tests/e2e/evidencia/auditoria-2026-11-21/moderacion-retiro.png) ·
[Administración móvil](../tests/e2e/evidencia/auditoria-2026-11-21/administracion-360.png) ·
[Asistente móvil](../tests/e2e/evidencia/auditoria-2026-11-21/asistente-360.png).

### Incidencias y correcciones

- **Recarga durante cierre de sesión.** Chrome volvió a restaurar una sesión
  que aún no se había revocado en el servidor. La prueba React que mantiene
  pendiente el bloqueo de IndexedDB también falló con el código previo. La
  pantalla guarda la marca no secreta antes de iniciar el cierre. Al recargar
  retoma la revocación. Se verificó con una nueva prueba React y en Chrome tras
  aplicar `098643c`.
- **Ejecución del arnés.** La primera carga de foto obtuvo HTTP 503 porque
  faltaba crear el directorio privado del entorno sintético. Los primeros
  intentos de lote, permisos y peso también mostraron selectores y un cuerpo de
  solicitud de prueba desactualizados. Se corrigió la configuración y el arnés.

## Próximos incrementos

1. **RF01, editar perfil propio.** Aprobar campos de contacto, cambios de correo
   y su verificación. Añadir actualización autenticada, pruebas de unicidad y
   reautenticación según la decisión. Es el siguiente incremento recomendado
   por su impacto transversal y su alcance acotado.
2. **RF02/RF17, edición de ganado y consistencia.** Aprobar qué campos pueden
   cambiarse, política de versiones y resolución de conflictos antes de ampliar
   IndexedDB. Validar recarga offline, dos ventanas, permisos revocados y eventos
   sanitarios en Chrome.
3. **RF16/RF11, gobierno de plataforma.** Resolver quién puede reportar y la
   protección contra abuso. Definir cambios de cuentas, trazabilidad y
   aprovisionamiento administrativo.
4. **RF04/RF05/RF06/RF07, cerrar alcances ganaderos.** Añadir el baño individual.
   Aprobar estimación de parto, indicadores productivos y anticipación antes de
   implementarlos. Acordar canal y proveedor de notificaciones.
5. **RF08/RF09/RF19, IA con evidencia.** Aprobar criterios de patrones y tendencias
   a partir de hechos confirmados y un corpus de orientación revisado. Mantener
   abstención y fuente por cada afirmación. Medir calidad con preguntas variadas.

La etapa de carga, Redis, CI/CD y despliegue permanece identificada en la
[rúbrica](RUBRICA-DE-EVALUACION.md). No se asignaron puntajes.
