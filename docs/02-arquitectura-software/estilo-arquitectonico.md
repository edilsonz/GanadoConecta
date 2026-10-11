# Estilo arquitectónico

## 1. Estilo seleccionado

GanadoConecta utilizará una arquitectura cliente-servidor
con un backend monolítico modular organizado en capas,
acompañado de un servicio propio de inteligencia artificial
ejecutado de manera independiente.

La solución incluirá PostgreSQL para los datos permanentes
y Redis para almacenar temporalmente consultas del catálogo público.

## 2. Aplicación al sistema

| Característica | Aplicación en GanadoConecta |
|---|---|
| Cliente-servidor | La aplicación web React consume las operaciones del backend mediante una API REST sobre HTTPS y JSON. |
| Monolito modular | Spring Boot integra los módulos principales dentro de una misma aplicación desplegable. |
| Organización en capas | Se separan la recepción de solicitudes, los casos de uso, las reglas del negocio y los detalles de infraestructura. |
| Servicio de IA independiente | Python y FastAPI ejecutan el análisis de registros históricos en un proceso separado. |
| Persistencia relacional | PostgreSQL conserva los registros y permite realizar operaciones transaccionales. |
| Caché de consultas | Redis conserva temporalmente resultados públicos del catálogo para reducir consultas repetidas a PostgreSQL. |

PostgreSQL y Redis son elementos de infraestructura que apoyan
el estilo seleccionado; no constituyen estilos arquitectónicos.

## 3. Justificación

### Cliente-servidor

Permite separar la presentación de la información del procesamiento
de las operaciones.

La aplicación web ofrece formularios, historiales, calendario,
reportes y anuncios. El backend verifica permisos, aplica reglas
y coordina el acceso a los datos.

### Monolito modular

Permite desarrollar y desplegar las funciones principales
de forma conjunta, manteniendo límites entre responsabilidades.

Los módulos de usuarios, animales, seguimiento, calendario,
análisis, reportes y anuncios compartirán el backend principal.

Esta organización reduce la complejidad operativa inicial.
Los cambios deberán respetar los contratos entre módulos
para evitar un acoplamiento innecesario.

### Organización interna

Clean Architecture orientará las dependencias del código
hacia el dominio y los casos de uso.

La persistencia, la caché y las integraciones se conectarán
mediante contratos y adaptadores.

### Servicio propio de IA

Su ejecución independiente permite utilizar herramientas de Python
y asignar recursos al análisis por separado.

El backend gestionará solicitudes mediante tareas persistentes
para evitar que el procesamiento bloquee las operaciones habituales.

### Caché con Redis

Redis almacenará resultados frecuentes del catálogo público,
mientras PostgreSQL conservará la información permanente.

Se utilizarán caducidad e invalidación después de cambios confirmados.
Si Redis no tiene el resultado o no está disponible, el backend
consultará PostgreSQL.

El beneficio de la caché se comprobará mediante pruebas de carga.

## 4. Relación entre los elementos

1. El usuario accede a la aplicación web.
2. React envía solicitudes a la API REST.
3. El backend valida las entradas y comprueba los permisos
   de las operaciones protegidas.
4. Los casos de uso ejecutan las reglas correspondientes.
5. Los adaptadores realizan las consultas o integraciones necesarias.
6. La API devuelve la respuesta a la aplicación web.

En las consultas del catálogo, el backend podrá utilizar Redis.
Los cambios de negocio se confirmarán en PostgreSQL antes
de invalidar las entradas de caché afectadas.

El análisis inteligente y el envío de notificaciones se procesarán
mediante tareas persistentes en segundo plano.

## 5. Límites de la propuesta

La expresión monolito modular se refiere al backend principal.
La solución completa también incluye la aplicación web,
PostgreSQL, Redis, el servicio de IA y el almacenamiento de imágenes.

El proveedor de notificaciones es un sistema externo.

La negociación, los pagos, las comisiones, el transporte
y el chat interno quedan fuera del alcance inicial.

## 6. Evaluación

La propuesta se verificará mediante:

- Pruebas unitarias de reglas y casos de uso.
- Pruebas de integración de persistencia, caché, permisos
  y servicios.
- Pruebas de carga con caché vacía y poblada.
- Pruebas de continuidad ante fallos de Redis,
  IA y notificaciones.

La separación de nuevos servicios se evaluará según
las mediciones y necesidades de crecimiento.