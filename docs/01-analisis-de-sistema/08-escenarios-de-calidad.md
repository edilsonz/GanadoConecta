# Escenarios de calidad

## 1. Descripción

Los escenarios describen situaciones concretas para evaluar
los atributos de calidad de GanadoConecta.

Cada escenario identifica el estímulo, el entorno, la parte afectada,
la respuesta esperada y la medida para comprobarla.

## 2. Escenarios

### EC01. Rendimiento de consultas

- **Atributo:** AC01.
- **Fuente:** Productores e interesados.
- **Estímulo:** Consultas concurrentes de animales y anuncios.
- **Entorno:** Entorno de prueba documentado, con 100 usuarios
  concurrentes y un conjunto de datos previamente definido.
- **Parte afectada:** API de consultas y persistencia.
- **Respuesta esperada:** El sistema procesa las consultas
  dentro del objetivo de tiempo acordado.
- **Medida:** El percentil 95 de los tiempos de respuesta
  no supera los 2 segundos.
- **Verificación:** Pruebas de carga.

Se excluyen el procesamiento de IA y la transferencia de fotografías.
La duración, distribución de solicitudes y tasa máxima aceptable
de errores se definirán antes de ejecutar la prueba.

### EC02. Falla de un servicio integrado

- **Atributo:** AC02.
- **Fuente:** Servicio de IA o proveedor de notificaciones.
- **Estímulo:** Error de comunicación o tiempo de espera agotado.
- **Entorno:** Sistema operativo con una integración temporalmente
  no disponible.
- **Parte afectada:** Adaptadores y tareas de análisis o notificación.
- **Respuesta esperada:** Se registra el fallo y se aplica
  la política de reintentos. Las operaciones principales continúan.
- **Medida:** Los casos de prueba de registro de animales
  y consulta de historiales y anuncios finalizan correctamente
  durante la falla simulada.
- **Verificación:** Pruebas de integración con fallos controlados.

Este escenario no establece un porcentaje de disponibilidad mensual.

### EC03. Crecimiento de datos y solicitudes

- **Atributo:** AC03.
- **Fuente:** Aumento de usuarios y registros.
- **Estímulo:** Crecimiento del volumen de animales, historiales,
  anuncios y consultas.
- **Entorno:** Prueba con conjuntos de datos y niveles de carga
  progresivamente mayores.
- **Parte afectada:** Backend, base de datos y recursos de ejecución.
- **Respuesta esperada:** Se identifican los límites de capacidad
  y se evalúan ajustes de recursos, consultas e índices.
- **Medida:** Comparación de tiempos, errores y consumo de recursos
  antes y después de los ajustes.
- **Verificación:** Pruebas de carga y revisión de consultas.

Los volúmenes y umbrales adicionales se definirán antes de la prueba.
No se considera demostrada la escalabilidad únicamente por el diseño.

### EC04. Acceso a registros de otro productor

- **Atributo:** AC04.
- **Fuente:** Usuario autenticado.
- **Estímulo:** Intento de consultar o modificar un registro privado
  perteneciente a otro productor.
- **Entorno:** Sistema con al menos dos productores y sus registros.
- **Parte afectada:** Operaciones protegidas de la API.
- **Respuesta esperada:** Se rechaza la operación sin exponer
  información privada ni modificar registros.
- **Medida:** Todos los intentos no autorizados del conjunto
  de pruebas son rechazados y los datos permanecen sin cambios.
- **Verificación:** Pruebas de integración de permisos y pertenencia.

También se comprobará que las respuestas del catálogo público
incluyan únicamente los campos autorizados.

### EC05. Falla durante el registro de una venta

- **Atributo:** AC05.
- **Fuente:** Productor autorizado y fallo de persistencia simulado.
- **Estímulo:** Interrupción entre la actualización del animal
  y el cierre de sus anuncios.
- **Entorno:** Animal con historial y anuncios activos.
- **Parte afectada:** Caso de uso de venta y transacción de persistencia.
- **Respuesta esperada:** Se revierte la operación completa.
  En una ejecución exitosa se actualizan ambos elementos.
- **Medida:** No existen actualizaciones parciales y el historial
  se conserva tanto en el caso exitoso como en el fallido.
- **Verificación:** Pruebas de integración transaccionales.

### EC06. Modificación de un caso de uso

- **Atributo:** AC06.
- **Fuente:** Equipo de desarrollo.
- **Estímulo:** Cambio de una regla de publicación de anuncios.
- **Entorno:** Desarrollo guiado por una especificación actualizada.
- **Parte afectada:** Dominio y aplicación del módulo de anuncios.
- **Respuesta esperada:** La regla puede probarse sin ejecutar
  PostgreSQL, el servicio de IA ni el proveedor de notificaciones.
- **Medida:** Las pruebas unitarias se ejecutan con implementaciones
  simuladas y el núcleo no importa tecnologías de infraestructura.
- **Verificación:** Pruebas unitarias y revisión de dependencias.

El esfuerzo de modificación y los límites permitidos entre módulos
se concretarán cuando exista una estructura de código.

### EC07. Respuesta inválida de una integración

- **Atributo:** AC07.
- **Fuente:** Servicio de IA o proveedor de notificaciones.
- **Estímulo:** Respuesta incompleta, mal formada o incompatible
  con el contrato esperado.
- **Entorno:** Integración en ejecución.
- **Parte afectada:** Adaptador correspondiente.
- **Respuesta esperada:** Se detecta la respuesta inválida,
  se registra el fallo y no se almacena como un resultado válido.
- **Medida:** Todas las respuestas inválidas previstas en las pruebas
  son detectadas sin corromper los registros.
- **Verificación:** Pruebas de integración de contratos y errores.

### EC08. Uso desde computadora y teléfono

- **Atributo:** AC08.
- **Fuente:** Productor o interesado.
- **Estímulo:** Registro de un control, publicación de un anuncio
  o búsqueda en el catálogo.
- **Entorno:** Navegador en computadora y teléfono móvil.
- **Parte afectada:** Interfaz web.
- **Respuesta esperada:** Los formularios, mensajes y filtros
  permiten completar las tareas sin elementos inaccesibles
  ni desplazamiento horizontal que impida su uso.
- **Medida:** Las tareas definidas se completan en los dispositivos
  y tamaños de pantalla seleccionados.
- **Verificación:** Revisión responsiva y pruebas con usuarios.

La muestra de usuarios, los dispositivos y las metas de tiempo
y éxito se definirán antes de la evaluación de usabilidad.

## 3. Evidencia de evaluación

Cada ejecución deberá registrar:

- Escenario y especificación relacionados.
- Versión del código evaluado.
- Entorno y datos utilizados.
- Procedimiento o comando de ejecución.
- Resultado esperado y resultado obtenido.
- Evidencias y limitaciones.
- Estado: aprobado, fallido o pendiente.

Estos escenarios son objetivos de evaluación.
No representan pruebas ya ejecutadas.