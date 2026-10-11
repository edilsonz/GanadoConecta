# Principios de diseño de GanadoConecta

## 1. Propósito

Establecer los principios que guiarán el diseño interno de
GanadoConecta para mantener sus módulos comprensibles,
modificables y verificables.

Estos principios corresponden al diseño propuesto.
Su cumplimiento deberá comprobarse durante la implementación.

## 2. Separación de responsabilidades

Cada elemento del sistema debe tener una responsabilidad definida:

- React presenta la información y permite la interacción del usuario.
- Los controladores reciben solicitudes y construyen respuestas.
- Los casos de uso coordinan las operaciones de la aplicación.
- El dominio contiene las reglas de negocio.
- Los adaptadores conectan la aplicación con bases de datos
  y servicios externos.

Las validaciones de la interfaz ayudan al usuario, pero las reglas
y los permisos también deben verificarse en el backend.

## 3. Dependencias hacia el dominio

Se aplicará Clean Architecture: las dependencias del código
se orientan hacia las políticas internas del sistema.

El dominio no debe depender de React, Spring Boot, PostgreSQL,
Redis ni FastAPI.

Los casos de uso utilizan contratos para acceder a la persistencia
y a los servicios externos. Los adaptadores implementan esos
contratos mediante las tecnologías seleccionadas.

## 4. Alta cohesión y bajo acoplamiento

Las funcionalidades relacionadas se agrupan en módulos del
monolito, con responsabilidades y contratos definidos.

Los módulos deben evitar acceder directamente a los detalles
internos de otros módulos. La colaboración se realiza mediante
operaciones explícitas.

Por ejemplo, registrar la venta de un animal debe coordinar
su estado y el cierre de sus anuncios, preservando su historial.

## 5. Principios SOLID

| Principio | Aplicación propuesta |
|---|---|
| Responsabilidad única | Separar reglas de negocio, coordinación, persistencia y presentación. |
| Abierto/cerrado | Incorporar nuevas implementaciones de servicios mediante contratos cuando exista una necesidad concreta. |
| Sustitución de Liskov | Las implementaciones deben respetar el comportamiento definido por sus contratos. |
| Segregación de interfaces | Definir contratos específicos para las operaciones que necesita cada consumidor. |
| Inversión de dependencias | Los casos de uso dependen de contratos; la infraestructura proporciona sus implementaciones. |

Las interfaces y abstracciones se incorporarán cuando ayuden
a separar responsabilidades o dependencias externas.

### 5.1. Aplicación de SOLID en GanadoConecta

| Principio | Ejemplo de aplicación |
|---|---|
| Responsabilidad única (SRP) | En Gestión de Ganado, el controlador recibe solicitudes, el caso de uso coordina el registro de animales y el repositorio gestiona su persistencia. |
| Abierto/cerrado (OCP) | El proveedor de notificaciones podrá reemplazarse mediante un nuevo adaptador sin modificar los casos de uso. |
| Sustitución de Liskov (LSP) | Los adaptadores que implementen el contrato de almacenamiento de fotografías deberán respetar las mismas condiciones de entrada, salida y manejo de errores. |
| Segregación de interfaces (ISP) | Los casos de uso utilizarán contratos específicos para consultar animales, almacenar registros o enviar notificaciones, evitando interfaces innecesariamente extensas. |
| Inversión de dependencias (DIP) | El caso de uso RegistrarAnimal dependerá de un contrato de repositorio, no directamente de PostgreSQL o de una implementación de Spring Data JPA. |

Estos ejemplos representan decisiones propuestas de diseño.
Su cumplimiento deberá verificarse durante la implementación.

## 6. Simplicidad y reutilización

Se aplicarán los siguientes criterios:

- KISS: mantener soluciones comprensibles y acordes con el alcance.
- DRY: evitar duplicar reglas de negocio.
- YAGNI: implementar las capacidades necesarias para los
  requisitos definidos.

Se mantiene el backend como monolito modular. La separación
del servicio de IA responde a la arquitectura acordada.

## 7. Integridad de los datos

PostgreSQL conserva los datos permanentes.

Las operaciones que requieren consistencia conjunta se ejecutan
dentro de una transacción. Las restricciones de la base de datos
complementan las validaciones de la aplicación.

Los cambios de estado deben respetar las reglas del dominio.
La venta de un animal conserva sus registros históricos.

Las integraciones externas deben definir cómo recuperarse
de fallos sin confirmar resultados que no se hayan obtenido.

## 8. Seguridad y privacidad

La autorización se verifica en el backend según el rol y,
cuando corresponda, la propiedad de los registros.

Se distingue entre:

- Información privada de la gestión ganadera.
- Información autorizada para anuncios públicos.
- Operaciones administrativas.

Los datos privados no deben aparecer en respuestas públicas,
logs ni entradas de la caché del catálogo.

Las contraseñas se almacenan mediante hash y las credenciales
de infraestructura se mantienen fuera del repositorio.

## 9. Uso de la caché

Redis mejora las consultas del catálogo público.

El diseño debe contemplar:

- Claves que identifiquen filtros, paginación y orden.
- Tiempo de expiración de las entradas.
- Invalidación después de confirmar cambios en PostgreSQL.
- Registro y reintento de invalidaciones fallidas.
- Consulta a PostgreSQL ante ausencia o fallo de caché.
- Protección frente a la exposición de anuncios cuya
  visibilidad haya sido revocada.

Redis no reemplaza la persistencia permanente ni forma parte
de la transacción de venta.

## 10. Contratos y manejo de errores

Las operaciones deben definir entradas, salidas, validaciones
y respuestas ante errores.

Los mensajes para el usuario deben ser comprensibles.
Las respuestas no deben exponer credenciales, consultas internas
ni detalles sensibles de infraestructura.

Las llamadas al servicio de IA y al proveedor de notificaciones
deben contemplar tiempos de espera y fallos.

El análisis de IA comunica tendencias y variaciones;
no constituye un diagnóstico veterinario.

## 11. Diseño guiado por especificaciones

Las especificaciones SDD concretan el comportamiento esperado
antes de implementar cada funcionalidad.

El diseño debe mantener trazabilidad con los requisitos,
las reglas de negocio y los criterios de aceptación.

Los cambios que afecten el comportamiento o la arquitectura
deben reflejarse en la documentación correspondiente.

## 12. Estado

Este documento define los principios del diseño propuesto.
La implementación y la verificación de su cumplimiento
están pendientes.