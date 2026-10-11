# Propuesta tecnológica de GanadoConecta

## 1. Objetivo

Definir las tecnologías que se utilizarán para implementar
GanadoConecta, considerando su arquitectura, funcionalidades
y pruebas obligatorias.

## 2. Tecnologías seleccionadas

| Parte | Tecnología | Justificación |
|---|---|---|
| Interfaz web | React | Permite organizar la interfaz mediante componentes reutilizables y consumir la API REST. |
| Backend | Java y Spring Boot | Proporciona herramientas para implementar la API, seguridad, persistencia y ejecución de tareas. |
| Base de datos | PostgreSQL | Permite conservar registros relacionados y realizar operaciones transaccionales. |
| Caché | Redis | Permite almacenar temporalmente consultas frecuentes del catálogo público. |
| Servicio de IA | Python y FastAPI | Permite desarrollar el análisis de registros y exponerlo mediante una API independiente. |
| Control de versiones | Git y GitHub | Permite registrar cambios y mantener el código y la documentación del proyecto. |
| Editor de desarrollo | Visual Studio Code | Permite editar documentación y código, utilizar Git y trabajar con Codex. |
| Diagramación | Draw.io | Permite crear y mantener diagramas editables de arquitectura y del modelo C4. |

Las versiones concretas se fijarán al crear los proyectos
y se registrarán en sus archivos de dependencias.

## 3. Aplicación web

React presentará las funcionalidades del productor,
el visitante y el administrador.

La interfaz incluirá formularios, historiales, calendario,
reportes y catálogo de anuncios. Se adaptará a computadoras,
tabletas y teléfonos móviles.

Se comunicará con el backend mediante HTTPS y JSON.
La biblioteca de componentes visuales se seleccionará
durante el diseño de la interfaz.

## 4. Backend principal

El backend se desarrollará con Java y Spring Boot
como una aplicación monolítica modular.

Se utilizarán:

- Spring MVC para los controladores REST.
- Spring Security para autenticación y controles de acceso.
- JWT para las credenciales de acceso a la API,
  según la especificación de autenticación.
- Spring Data JPA para los adaptadores de persistencia.

Los módulos se organizarán mediante Clean Architecture.
El dominio permanecerá independiente de los frameworks
y de las implementaciones de infraestructura.

Un JWT válido no sustituirá la comprobación del rol
y la pertenencia de cada registro.

## 5. Persistencia

PostgreSQL conservará usuarios, animales, historiales,
actividades, anuncios y tareas persistentes.

Se aplicarán relaciones, restricciones e índices
según las necesidades de los datos y las consultas.

La venta actualizará el estado del animal y cerrará
sus anuncios activos dentro de una misma transacción,
conservando el historial.

## 6. Caché del catálogo

Redis almacenará temporalmente resultados públicos
del catálogo de anuncios.

- Las entradas tendrán un tiempo de caducidad.
- Las claves distinguirán filtros, paginación y orden.
- Los cambios confirmados en anuncios invalidarán
  las entradas afectadas.
- Las invalidaciones fallidas se registrarán y reintentarán.
- Ante ausencia de una entrada o fallo de Redis,
  se consultará PostgreSQL.
- No se almacenarán historiales privados, credenciales
  ni archivos de fotografías.

El cliente de Redis se integrará mediante un adaptador
que implemente el contrato CacheCatalogo.

Las especificaciones definirán la caducidad, la invalidación
y el control de información desactualizada.

## 7. Servicio propio de IA

El servicio se desarrollará con Python y FastAPI
y se ejecutará de manera independiente del backend.

Recibirá los registros necesarios y devolverá resultados
sobre patrones o variaciones productivas.

El método de análisis, las bibliotecas y los datos mínimos
se definirán antes de implementar esta funcionalidad.

No realizará diagnósticos veterinarios.
Sus solicitudes se gestionarán mediante tareas persistentes.

## 8. Notificaciones y fotografías

El proveedor de notificaciones y los canales de envío
están pendientes de selección.

Las fotografías se conservarán en almacenamiento
de archivos u objetos. PostgreSQL guardará sus referencias
y metadatos.

La solución concreta de almacenamiento también
está pendiente de selección.

Ambas integraciones utilizarán contratos y adaptadores
para mantener sus detalles fuera de los casos de uso.

## 9. Pruebas obligatorias

| Tipo | Alcance |
|---|---|
| Unitarias | Reglas del dominio y casos de uso con implementaciones simuladas de los contratos. |
| Integración | API, seguridad, persistencia, transacciones, caché, tareas y adaptadores. |
| Carga | Consultas de animales y anuncios con 100 usuarios concurrentes, diferenciando caché vacía y poblada. |

Se comprobará también la continuidad de las consultas
cuando Redis no esté disponible y el comportamiento
ante fallos de IA y notificaciones.

Las herramientas concretas de pruebas se seleccionarán
antes de implementar los proyectos.

El objetivo de rendimiento será un percentil 95
de hasta 2 segundos bajo condiciones documentadas,
excluyendo el análisis de IA y la transferencia de fotografías.

## 10. Desarrollo guiado por especificaciones

Se aplicará SDD para definir las funcionalidades
antes de implementar el código.

- docs/ contendrá la documentación general y arquitectónica.
- specs/ contendrá las especificaciones de funcionalidades.
- AGENTS.md orientará el trabajo de Codex en el repositorio.

Cada especificación incluirá reglas, permisos, entradas,
resultados, errores, criterios de aceptación y pruebas.

Codex apoyará la documentación y la implementación.
Sus cambios deberán revisarse y verificarse.

## 11. Organización futura del código

El repositorio incorporará:

- frontend/: aplicación React.
- backend/: aplicación Spring Boot.
- servicio-ia/: servicio Python/FastAPI.

Estas carpetas se crearán al iniciar la implementación.

## 12. Estado de la propuesta

Las tecnologías principales están seleccionadas.
La implementación y sus resultados de pruebas
todavía están pendientes.

El entorno de despliegue, las versiones, las herramientas
de pruebas y los proveedores pendientes se definirán
en las siguientes etapas.