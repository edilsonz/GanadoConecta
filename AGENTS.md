# Instrucciones para trabajar en GanadoConecta

## Contexto del proyecto

GanadoConecta es una aplicación web para gestionar ganado vacuno,
mantener historiales sanitarios, reproductivos y productivos,
programar actividades, analizar registros y publicar anuncios de venta.

## Arquitectura acordada

- Arquitectura cliente-servidor.
- Backend principal como monolito modular.
- Clean Architecture aplicada de forma sencilla.
- Servicio propio de inteligencia artificial independiente.
- Dependencias del código dirigidas hacia el dominio y los casos de uso.
- Interfaces para persistencia e integraciones cuando sean necesarias.

## Tecnologías seleccionadas

- Frontend: React.
- Backend: Java con Spring Boot.
- Base de datos: PostgreSQL.
- Servicio de IA: Python con FastAPI.
- Control de versiones: Git y GitHub.
- Proveedor de notificaciones y almacenamiento de fotografías:
  pendientes de selección.

## Desarrollo guiado por especificaciones (SDD)

- Definir y revisar las especificaciones antes de implementar.
- Guardar las especificaciones de funcionalidades en specs/.
- Mantener la documentación general y arquitectónica en docs/.
- Cada especificación deberá incluir:
  - Objetivo y requisitos relacionados.
  - Actores y permisos.
  - Entradas y resultados esperados.
  - Reglas de negocio y validaciones.
  - Flujo principal y casos de error.
  - Criterios de aceptación.
  - Pruebas necesarias.
- Mantener coherentes las especificaciones, el código y las pruebas.
- Actualizar la documentación cuando cambie el comportamiento.
- No inventar requisitos ni presentar funciones pendientes
  como implementadas.

## Reglas del sistema

- Verificar identidad, permisos y pertenencia de los registros.
- Mantener privados los historiales completos de los animales.
- Publicar únicamente la información autorizada por el productor.
- Conservar el historial al registrar la venta de un animal.
- Actualizar el estado del animal y cerrar sus anuncios activos
  dentro de una misma transacción.
- No incorporar pagos, comisiones, chat interno ni logística
  en el alcance inicial.
- La IA analizará registros históricos; no realizará diagnósticos
  ni sustituirá la evaluación de un profesional veterinario.

## Pruebas obligatorias

### Pruebas unitarias

- Comprobar reglas de negocio y casos de uso.
- Cubrir condiciones válidas, inválidas y casos límite relevantes.
- Probar el núcleo sin depender de bases de datos o servicios reales.

### Pruebas de integración

- Comprobar la interacción entre API, casos de uso y persistencia.
- Verificar autenticación, autorización y aislamiento entre productores.
- Verificar transacciones, conservación del historial y cierre de anuncios.
- Comprobar los adaptadores de IA y notificaciones mediante entornos
  de prueba o servicios controlados, incluyendo fallos de comunicación.

### Pruebas de carga

- Evaluar las consultas de animales y anuncios.
- Usar como objetivo inicial que el 95 % de las consultas responda
  en un máximo de 2 segundos con 100 usuarios concurrentes.
- Excluir de ese objetivo el procesamiento de IA y la transferencia
  de fotografías.
- Documentar el entorno, volumen de datos, duración, errores
  y tiempos de respuesta de cada prueba.

## Verificación y evidencia

- Relacionar las pruebas con los criterios de aceptación.
- Ejecutar las pruebas correspondientes a cada cambio.
- Ejecutar pruebas de carga cuando exista una versión ejecutable
  de los flujos evaluados y ante cambios que afecten su rendimiento.
- Registrar los resultados y las limitaciones encontradas.
- No afirmar que una prueba pasó si no se ejecutó.
- Las herramientas de pruebas se elegirán y documentarán
  antes de implementarlas.

## Forma de trabajo

- Revisar la documentación existente antes de modificar el sistema.
- Escribir la documentación en español.
- Mantener coherentes los códigos de requisitos, atributos,
  restricciones, drivers y decisiones.
- No guardar credenciales ni secretos en el repositorio.
- Agrupar los commits por bloques de trabajo relacionados.