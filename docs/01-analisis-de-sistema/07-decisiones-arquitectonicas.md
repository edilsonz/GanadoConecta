# Decisiones arquitectónicas

## 1. Descripción

Las decisiones arquitectónicas de GanadoConecta responden a los
drivers identificados y establecen una base para implementar
la primera versión del sistema.

Su estado es propuesto para implementación. El cumplimiento
se verificará mediante revisión del código y pruebas.

## 2. Decisiones

| ID | Decisión arquitectónica | Drivers relacionados | Justificación | Resultado esperado |
|---|---|---|---|---|
| ADR-01 | Backend principal como monolito modular. | DA05, DA06, DA08 | Mantener las funciones principales en una aplicación desplegable reduce la complejidad operativa. La separación modular facilita su mantenimiento y evolución. | Backend con módulos de usuarios, animales, seguimiento, calendario, alertas, reportes y anuncios. |
| ADR-02 | Aplicación web responsiva bajo un esquema cliente-servidor. | DA06, DA07 | Permite adaptar la interfaz a diferentes dispositivos y separar la presentación del procesamiento de las operaciones. | Frontend React conectado al backend Spring Boot mediante una API REST sobre HTTPS y JSON. |
| ADR-03 | Clean Architecture para las dependencias internas. | DA06 | Las reglas de negocio y los casos de uso deben poder probarse sin depender directamente de frameworks, bases de datos o proveedores. | Dominio, aplicación, adaptadores e infraestructura, con dependencias dirigidas hacia el núcleo. |
| ADR-04 | PostgreSQL con operaciones transaccionales. | DA02 | Los historiales y los cambios relacionados de estado requieren integridad referencial y actualizaciones consistentes. | Persistencia relacional; actualización del animal y cierre de sus anuncios activos dentro de una misma transacción. |
| ADR-05 | Servicio propio de IA ejecutado de manera independiente. | DA03 | Permite utilizar herramientas de Python y asignar recursos al análisis por separado. La ejecución en segundo plano evita bloquear las operaciones principales. | Servicio Python/FastAPI que recibe los registros necesarios y devuelve resultados al backend. |
| ADR-06 | Integraciones mediante interfaces y adaptadores. | DA03, DA04, DA06 | Los casos de uso deben depender de contratos y no de clientes HTTP o proveedores concretos. | Contratos para análisis, notificaciones y almacenamiento, implementados mediante adaptadores de infraestructura. |
| ADR-07 | Autenticación y autorización por rol y pertenencia. | DA01, DA08 | Es necesario comprobar tanto las funciones permitidas como la relación del usuario con cada registro. | Spring Security y JWT, con verificaciones de permisos y pertenencia en las operaciones protegidas. |
| ADR-08 | Módulo de anuncios con información pública y estados controlados. | DA01, DA02, DA08 | Permite gestionar las publicaciones y su moderación, protegiendo el historial privado de los animales. | Respuestas públicas específicas; estados de borrador, publicado, pausado, retirado y vendido, con un control de moderación diferenciado. |
| ADR-09 | Tareas persistentes para análisis y notificaciones. | DA03, DA04 | Las solicitudes deben recuperarse y reintentarse después de una falla sin perder las tareas pendientes. | Registro de tareas, estados e intentos en PostgreSQL; ejecución en segundo plano y reintentos limitados con control de duplicados. |
| ADR-10 | Consultas optimizadas y almacenamiento separado de fotografías. | DA05 | Los listados requieren consultas eficientes y las fotografías necesitan almacenamiento adecuado para archivos. | Paginación e índices en PostgreSQL; fotografías en almacenamiento de archivos u objetos, con referencias y metadatos en la base de datos. |
| ADR-11 | Redis como caché del catálogo público. | DA01, DA02, DA05, DA06 | Reduce consultas repetidas a PostgreSQL y permite mantener el acceso a la caché detrás de un contrato. | Caché de resultados públicos con caducidad, invalidación tras cambios confirmados y consulta a PostgreSQL cuando no exista una entrada o Redis falle. |

## 3. Consecuencias y consideraciones

### Monolito modular

- Los módulos principales compartirán una unidad de despliegue.
- Se deberán respetar sus límites para evitar dependencias
  innecesarias entre funcionalidades.
- El servicio propio de IA tendrá un despliegue independiente.

### Clean Architecture

- Se separarán las reglas de negocio de los detalles tecnológicos.
- Se crearán interfaces principalmente para persistencia
  e integraciones cuando sean necesarias.
- No se requiere una interfaz para cada clase.
- La separación introduce clases y transformaciones adicionales,
  por lo que deberá mantenerse proporcional al alcance del proyecto.

### Seguridad

- Un JWT válido no sustituye la verificación de permisos
  y pertenencia de los registros.
- La expiración, renovación y revocación de credenciales
  se detallarán en la especificación de autenticación.
- Las respuestas públicas no incluirán historiales completos
  ni datos de contacto no autorizados.

### Estados y moderación

- El estado de publicación y la condición de moderación
  se tratarán como conceptos diferenciados.
- El catálogo mostrará únicamente anuncios publicados
  y habilitados por moderación.
- Las transiciones permitidas y el procedimiento de revisión
  se definirán en las especificaciones.

### Tareas persistentes

- Las tareas deberán registrar su estado y los intentos realizados.
- La ejecución deberá controlar reclamaciones concurrentes
  de una misma tarea y recuperarse ante interrupciones.
- Se evitará aplicar dos veces un mismo resultado de análisis.
- La prevención de notificaciones duplicadas dependerá también
  de las capacidades del proveedor.
- Persistir una tarea no garantiza por sí solo su ejecución correcta.

### Caché del catálogo público

- PostgreSQL conservará los datos permanentes y será
  la fuente principal de información.
- Redis almacenará temporalmente resultados de consultas
  del catálogo público.
- El backend consultará primero la caché. Si no encuentra
  la información, la obtendrá de PostgreSQL y podrá guardarla
  en Redis con un tiempo de caducidad.
- Las claves distinguirán filtros, paginación y orden.
- Después de confirmar cambios de publicación, edición,
  pausa, retirada, moderación o venta, se invalidarán
  las entradas afectadas.
- Las invalidaciones fallidas se registrarán para reintentarlas.
- Si Redis no está disponible, las consultas continuarán
  mediante PostgreSQL, aunque podrían ser más lentas.
- No se almacenarán historiales privados, credenciales
  ni archivos de fotografías en esta caché.
- La caducidad y la estrategia de invalidación se definirán
  en la especificación del catálogo.
- La caché puede conservar temporalmente datos anteriores
  si falla una invalidación. Las operaciones de modificación
  verificarán el estado vigente en PostgreSQL.

## 4. Verificación

| Tipo de prueba prevista | Alcance previsto |
|---|---|
| Integración | API, autenticación, permisos, pertenencia, persistencia, transacciones, tareas y adaptadores; consultas con y sin caché, caducidad, invalidación y fallos de Redis. |
| Carga | Rendimiento de consultas de animales y anuncios conforme a AC01, diferenciando caché vacía, caché poblada y Redis no disponible. |

Las pruebas se vincularán con los criterios de aceptación
de las especificaciones en specs/.

## 5. Decisiones pendientes

- Proveedor y canales de notificaciones.
- Solución concreta de almacenamiento de fotografías.
- Herramientas de pruebas unitarias, de integración y de carga.
- Método de análisis de IA y datos mínimos necesarios.
- Configuración del entorno de despliegue.

Redis forma parte de la arquitectura seleccionada para la primera
versión. Su configuración y beneficio se evaluarán mediante pruebas.

La separación de nuevos servicios se evaluará según las mediciones
y las necesidades de crecimiento del sistema.