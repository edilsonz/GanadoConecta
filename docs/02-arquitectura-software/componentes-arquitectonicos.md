# Componentes arquitectónicos

## 1. Descripción

Este documento identifica las responsabilidades principales
de GanadoConecta y sus relaciones.

La aplicación web, el backend, el servicio de IA, PostgreSQL
y Redis son unidades diferenciadas de la solución.

Los módulos del backend forman parte de una misma aplicación
desplegable y se organizan mediante Clean Architecture.

## 2. Elementos de la solución

| Elemento | Responsabilidad | Tecnología |
|---|---|---|
| Aplicación web | Presentar formularios, historiales, calendario, reportes y catálogo de anuncios. | React |
| Backend principal | Exponer la API REST, comprobar permisos y ejecutar las operaciones del sistema. | Java y Spring Boot |
| Base de datos | Conservar permanentemente los registros y sus relaciones. | PostgreSQL |
| Caché | Almacenar temporalmente resultados de consultas frecuentes del catálogo público. | Redis |
| Servicio propio de IA | Analizar registros históricos e identificar patrones o variaciones productivas. | Python y FastAPI |
| Almacenamiento de fotografías | Conservar imágenes de los anuncios. | Solución pendiente de selección |
| Proveedor de notificaciones | Enviar recordatorios y alertas fuera de la plataforma. | Proveedor pendiente de selección |

El servicio de IA pertenece a GanadoConecta, aunque se ejecuta
de manera independiente. El proveedor de notificaciones es externo.

## 3. Componentes lógicos del backend

| Componente | Responsabilidad | Requisitos relacionados |
|---|---|---|
| Usuarios y acceso | Gestionar cuentas, perfiles, autenticación y permisos. | RF01, RF11 |
| Animales | Registrar, editar y consultar animales, su pertenencia y su estado. | RF02, RF15 |
| Seguimiento ganadero | Gestionar historiales y registros sanitarios, reproductivos y productivos. | RF03–RF06 |
| Calendario y alertas | Gestionar actividades programadas, recordatorios y alertas de seguimiento. | RF07, RF09 |
| Análisis inteligente | Preparar solicitudes de análisis y gestionar los resultados del servicio de IA. | RF08, RF09 |
| Reportes | Consultar reportes de seguimiento y estadísticas autorizadas. | RF10, RF11 |
| Anuncios y moderación | Gestionar publicaciones, búsquedas, contacto autorizado, estados, reportes y moderación. | RF12–RF16 |

Los módulos colaborarán mediante contratos definidos.
La operación de venta coordinará los módulos de animales y anuncios.

## 4. Componentes técnicos de apoyo

| Componente | Responsabilidad |
|---|---|
| API REST y seguridad | Recibir solicitudes, validar su formato, autenticar e invocar casos de uso. |
| Casos de uso | Coordinar las operaciones y comprobar permisos, pertenencia y reglas de negocio. |
| Adaptadores de persistencia | Implementar los repositorios y el acceso transaccional a PostgreSQL mediante Spring Data JPA. |
| Adaptador de caché | Implementar el contrato de acceso a Redis para consultar, guardar e invalidar resultados públicos con caducidad. |
| Adaptador de IA | Transformar datos y comunicarse con el servicio Python/FastAPI. |
| Adaptador de notificaciones | Implementar el contrato de envío e interpretar las respuestas del proveedor. |
| Adaptador de fotografías | Implementar el contrato para guardar y recuperar archivos de imagen. |
| Ejecutor de tareas | Procesar tareas persistentes en segundo plano, controlar estados y aplicar reintentos limitados. |

Estos componentes técnicos pertenecen al backend principal.
Su separación lógica no implica desplegarlos como servicios independientes.

### Diagrama de componentes del backend

![Componentes del backend de GanadoConecta](../../img/GanadoConecta_C4_Nivel_3_Componentes.png)

*Figura. Modelo C4 de nivel 3: componentes del backend de GanadoConecta.*

## 5. Relaciones principales

- React consume la API REST mediante HTTPS y JSON.
- Los controladores invocan los casos de uso.
- Los casos de uso utilizan las reglas del dominio y los contratos
  necesarios para acceder a recursos.
- Los adaptadores implementan los contratos del núcleo.
- PostgreSQL conserva los registros, relaciones y tareas persistentes.
- Redis conserva temporalmente resultados públicos del catálogo.
- El servicio de IA recibe únicamente los registros necesarios
  para realizar cada análisis.
- El almacenamiento de fotografías conserva los archivos;
  PostgreSQL mantiene sus referencias y metadatos.
- El proveedor de notificaciones recibe solicitudes de envío
  mediante su adaptador.

Las dependencias del código apuntarán hacia el núcleo.
El dominio no dependerá de PostgreSQL, Redis, Spring Boot
ni de clientes de servicios externos.

## 6. Consulta del catálogo con caché

1. El backend recibe la consulta y valida sus parámetros.
2. Consulta Redis mediante el contrato de caché.
3. Si existe una entrada vigente, utiliza el resultado almacenado.
4. Si la entrada no existe, ha caducado o Redis falla,
   consulta PostgreSQL.
5. Cuando Redis esté disponible, podrá almacenar el resultado
   con un tiempo de caducidad.
6. Devuelve únicamente información pública autorizada.

Las claves distinguirán los filtros, la paginación y el orden.

Después de confirmar cambios en los anuncios, se invalidarán
las entradas afectadas. Las invalidaciones fallidas se registrarán
y reintentarán.

La especificación definirá la caducidad, la invalidación y las medidas
para impedir que se muestre información que haya dejado de estar
autorizada.

La caché no almacenará historiales privados, credenciales
ni archivos de fotografías.

## 7. Registro de una venta

La operación coordinará las siguientes acciones:

1. Verificar la identidad y los permisos del productor.
2. Comprobar la pertenencia y el estado vigente del animal.
3. Actualizar el animal como vendido.
4. Cerrar sus anuncios activos.
5. Conservar su historial.
6. Confirmar la transacción en PostgreSQL.
7. Invalidar las entradas de caché afectadas.

La actualización del animal y el cierre de sus anuncios
se ejecutarán dentro de una misma transacción.

Redis no participará en esa transacción. Si la invalidación
falla, se registrará para reintentarla sin deshacer la venta
confirmada.

## 8. Análisis y notificaciones en segundo plano

Las solicitudes de análisis y notificaciones se registrarán
como tareas persistentes.

El ejecutor procesará las tareas pendientes, utilizará los
adaptadores correspondientes y registrará el resultado
de cada intento.

Las tareas deberán poder recuperarse después de una interrupción,
con controles para evitar aplicar resultados duplicados.

Una falla temporal de IA o notificaciones no deberá impedir
registrar animales ni consultar historiales y anuncios.

## 9. Verificación

- Pruebas unitarias de reglas y casos de uso.
- Pruebas de integración de API, persistencia, seguridad y adaptadores.
- Pruebas de integración de transacciones y tareas persistentes.
- Pruebas de consultas con y sin caché, caducidad e invalidación.
- Pruebas de continuidad de las consultas cuando Redis falle.
- Pruebas de carga de animales y anuncios, diferenciando caché
  vacía y poblada.

Las especificaciones en specs/ definirán los contratos
y criterios de aceptación antes de implementar cada funcionalidad.

## 10. Relación con el modelo C4

El nivel 2 representa las unidades de ejecución y almacenamiento
de GanadoConecta.

El nivel 3 detalla los componentes internos del backend y sus
colaboraciones con PostgreSQL, Redis, el servicio de IA,
las notificaciones y el almacenamiento de fotografías.

Las flechas del diagrama de componentes representan colaboraciones
durante la ejecución. Las dependencias del código respetarán
la orientación hacia el núcleo de Clean Architecture.