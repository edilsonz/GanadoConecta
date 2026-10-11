# Atributos de calidad

## 1. Descripción

Los atributos de calidad definen cómo deberá comportarse GanadoConecta.
Complementan los requisitos funcionales y orientan las decisiones
arquitectónicas y las pruebas.

## 2. Atributos de calidad

| ID | Atributo | Escenario de calidad | Prioridad |
|---|---|---|---|
| AC01 | Rendimiento | El 95 % de las consultas de animales y anuncios deberá responder en un máximo de 2 segundos durante una prueba con 100 usuarios concurrentes y un conjunto de datos definido. Se excluyen el procesamiento de IA y la transferencia de fotografías. | Alta |
| AC02 | Disponibilidad | Ante una falla temporal de IA, notificaciones o Redis, el sistema deberá permitir continuar con el registro de animales y la consulta de historiales y anuncios. Si Redis falla, las consultas del catálogo utilizarán PostgreSQL. | Alta |
| AC03 | Escalabilidad | El sistema deberá permitir aumentar su capacidad conforme crezcan los usuarios, animales, registros, fotografías y solicitudes, sin requerir un rediseño completo. | Media |
| AC04 | Seguridad | Cada productor deberá acceder únicamente a sus registros privados. Las publicaciones mostrarán solo información autorizada y las funciones administrativas estarán restringidas según los permisos del usuario. | Alta |
| AC05 | Integridad de datos | Los controles conservarán su asociación con el animal correspondiente. Al registrar una venta, el estado del animal y el cierre de sus anuncios activos deberán actualizarse de manera consistente, conservando el historial. | Alta |
| AC06 | Mantenibilidad | Los módulos deberán poder modificarse y probarse sin afectar innecesariamente otras funcionalidades. Las dependencias del código respetarán los límites definidos por Clean Architecture. | Alta |
| AC07 | Interoperabilidad | El sistema deberá intercambiar información con IA y notificaciones mediante interfaces definidas, detectando respuestas inválidas y fallos de comunicación. | Alta |
| AC08 | Usabilidad | El productor deberá registrar controles y publicar animales mediante formularios claros. Los interesados podrán consultar y filtrar anuncios desde computadoras y dispositivos móviles. | Alta |

## 3. Verificación de los atributos

| Atributo | Forma de verificación |
|---|---|
| AC01 | Pruebas de carga con caché vacía y poblada, registrando el percentil 95 de los tiempos de respuesta, la concurrencia y la tasa de errores. |
| AC02 | Pruebas de integración con fallos simulados de IA, notificaciones y Redis. Comprobar que las operaciones principales continúen y que el catálogo consulte PostgreSQL cuando Redis no esté disponible. |
| AC03 | Pruebas con volúmenes crecientes de datos y solicitudes, documentando los límites observados y los recursos utilizados. |
| AC04 | Pruebas de integración que intenten acceder sin autenticación, con un rol insuficiente o a registros de otro productor. Revisión de los campos expuestos en respuestas públicas. |
| AC05 | Pruebas unitarias de validaciones y pruebas de integración sobre relaciones y transacciones, incluyendo un fallo durante el registro de una venta. |
| AC06 | Revisión de dependencias entre módulos y capas, junto con pruebas unitarias del dominio y los casos de uso sin servicios reales. |
| AC07 | Pruebas de integración de adaptadores ante respuestas válidas, inválidas, errores y tiempos de espera. |
| AC08 | Revisión de los flujos principales en distintos tamaños de pantalla y pruebas de tareas con usuarios representativos. |

## 4. Condiciones de evaluación del rendimiento

Antes de ejecutar las pruebas de carga se deberán definir y registrar:

- Entorno de ejecución y recursos disponibles.
- Cantidad de animales, historiales y anuncios.
- Consultas evaluadas y distribución de solicitudes.
- Duración de la prueba y periodo de incremento de carga.
- Configuración de los 100 usuarios concurrentes.
- Punto de medición de los tiempos de respuesta.
- Tasa de errores y criterio de aceptación correspondiente.
- Estado inicial de la caché: vacía o poblada.
- Tiempo de caducidad configurado y proporción de consultas
  resueltas mediante Redis.
- Comportamiento cuando Redis no esté disponible.

El objetivo de 2 segundos corresponde al percentil 95, no al promedio.
La tasa de errores se evaluará por separado para evitar considerar
exitosa una prueba con respuestas rápidas pero fallidas.
Las pruebas con Redis disponible evaluarán el objetivo de AC01
tanto con caché vacía como poblada.

La prueba con Redis no disponible comprobará la continuidad
del servicio mediante PostgreSQL y documentará la degradación
del rendimiento. No se ha definido un tiempo máximo específico
para este escenario de fallo.

También se comprobará que los cambios en anuncios invaliden
las entradas afectadas y que las invalidaciones fallidas
puedan reintentarse.

## 5. Pruebas obligatorias

El proyecto incluirá:

- Pruebas unitarias de reglas de negocio y casos de uso.
- Pruebas de integración de API, persistencia, permisos y adaptadores.
- Pruebas de carga de consultas de animales y anuncios.

Los resultados se documentarán cuando exista una implementación
ejecutable. Las metas descritas no se consideran cumplidas hasta
que las pruebas aporten evidencia.

## 6. Precisiones pendientes

Los atributos AC03, AC06 y AC08 deberán concretarse con escenarios
y criterios adicionales en las especificaciones correspondientes.

No se ha definido todavía un porcentaje de disponibilidad mensual
ni una capacidad máxima garantizada de usuarios o registros.