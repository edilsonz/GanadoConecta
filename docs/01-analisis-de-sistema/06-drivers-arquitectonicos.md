# Drivers arquitectónicos

## 1. Descripción

Los drivers arquitectónicos identifican los requisitos, atributos
de calidad y restricciones que influyen directamente en el diseño
de GanadoConecta.

Las referencias corresponden a los 16 requisitos funcionales,
los ocho atributos de calidad y las nueve restricciones acordadas.

## 2. Drivers arquitectónicos

| ID | Driver arquitectónico | Origen | ¿Por qué influye en la arquitectura? | Prioridad |
|---|---|---|---|---|
| DA01 | Seguridad y separación entre información pública y privada. | RF01, RF11, RF12, RF16, AC04 | Requiere autenticación, autorización por roles, verificación de pertenencia de los registros y respuestas específicas para el catálogo público. | Alta |
| DA02 | Integridad del historial y de los estados de animales y anuncios. | RF02–RF06, RF14, RF15, AC05 | Exige validaciones, relaciones consistentes y transacciones para actualizar el estado del animal y cerrar sus anuncios sin perder el historial. | Alta |
| DA03 | Análisis inteligente independiente de las operaciones habituales. | RF08, RF09, AC01, AC02, RC06 | Justifica separar el procesamiento de IA y ejecutar los análisis sin bloquear las operaciones de registro y consulta. | Alta |
| DA04 | Programación de recordatorios y envío de notificaciones. | RF07, RF09, AC02, AC07, RC07 | Requiere tareas programadas, control del estado de envío y reintentos con mecanismos para reducir avisos duplicados. | Alta |
| DA05 | Rendimiento y crecimiento de historiales y anuncios. | RF03, RF06, RF10, RF12, RF13, AC01, AC03 | Influye en la paginación, los índices, la optimización de consultas, el almacenamiento de fotografías y la capacidad de despliegue. | Alta |
| DA06 | Mantenibilidad y control de dependencias internas. | AC06, AC07, RC03, RC04 | Orienta la separación modular, la aplicación de Clean Architecture y la definición de contratos para persistencia e integraciones. | Alta |
| DA07 | Acceso desde diferentes dispositivos. | RC01, AC08 | Condiciona el diseño responsivo, la navegación y la presentación de formularios, historiales y anuncios. | Media |
| DA08 | Publicación y moderación de anuncios de ganado. | RF12–RF16, RC08, RC09 | Requiere un módulo con estados, permisos y moderación que permita el contacto entre interesados dentro del alcance comercial definido. | Alta |

## 3. Relación con las decisiones arquitectónicas

Los drivers orientarán las decisiones sobre:

- Organización del backend como monolito modular.
- Comunicación mediante API REST.
- Aplicación de Clean Architecture.
- Persistencia relacional y operaciones transaccionales.
- Ejecución independiente del servicio de IA.
- Integraciones mediante interfaces y adaptadores.
- Autenticación, permisos y pertenencia de los registros.
- Estados y moderación de anuncios.
- Tareas persistentes de análisis y notificaciones.
- Optimización de consultas y almacenamiento de fotografías.

La justificación y trazabilidad de estas decisiones se detallarán
en 07-decisiones-arquitectonicas.md.

## 4. Relación con SDD y las pruebas

Las especificaciones en specs/ deberán referenciar los drivers
que condicionen cada funcionalidad.

La verificación incluirá:

- Pruebas unitarias de reglas, validaciones y casos de uso.
- Pruebas de integración de permisos, transacciones y adaptadores.
- Pruebas de carga relacionadas con AC01 y DA05.

SDD organiza el proceso de desarrollo y la trazabilidad.
No constituye por sí mismo un estilo arquitectónico.

## 5. Consideración sobre notificaciones

Los reintentos no garantizan por sí solos que un aviso se entregue
una única vez. El control de duplicados deberá considerar
las capacidades del proveedor y los identificadores de las tareas.