# Restricciones del sistema

## 1. Descripción

Las restricciones establecen las condiciones que deberán respetarse
durante el diseño y desarrollo de GanadoConecta.

## 2. Restricciones

| ID | Restricción | Descripción |
|---|---|---|
| RC01 | Aplicación web | El sistema deberá ser accesible mediante un navegador web y contar con una interfaz adaptable a computadoras, tabletas y teléfonos móviles. |
| RC02 | Control de versiones | El código fuente y la documentación deberán gestionarse mediante Git y mantenerse en un repositorio de GitHub. |
| RC03 | API REST | La comunicación entre la aplicación web y el backend deberá realizarse mediante una API REST. |
| RC04 | Organización arquitectónica | La solución deberá separar sus responsabilidades y aplicar Clean Architecture para orientar las dependencias internas hacia el núcleo del negocio. |
| RC05 | Alcance inicial | La primera versión estará orientada al ganado vacuno e incluirá gestión, seguimiento, análisis de información y anuncios de venta. |
| RC06 | Alcance de la inteligencia artificial | La IA analizará registros históricos del sistema. No realizará diagnósticos automáticos ni sustituirá la evaluación de un profesional veterinario. |
| RC07 | Notificaciones externas | El envío de recordatorios y alertas fuera de la plataforma dependerá de un proveedor externo. |
| RC08 | Operaciones comerciales | La primera versión no procesará pagos, cobrará comisiones por venta ni gestionará la entrega o el transporte. Estas condiciones serán acordadas entre los interesados. |
| RC09 | Contacto entre interesados | La comunicación se realizará mediante el contacto autorizado por el vendedor. No se implementará inicialmente un chat interno. |

## 3. Condiciones del proceso de desarrollo

Además de las restricciones del sistema, se han acordado
las siguientes condiciones de trabajo:

- Aplicar SDD: definir las especificaciones antes de implementar
  las funcionalidades.
- Guardar las especificaciones en specs/ y la documentación
  general y arquitectónica en docs/.
- Mantener coherentes los requisitos, las especificaciones,
  el código y las pruebas.
- Incorporar pruebas unitarias, de integración y de carga.
- Documentar los resultados de las pruebas y sus limitaciones.
- Agrupar los commits por bloques de trabajo relacionados.
- No almacenar credenciales ni secretos en el repositorio.

Estas condiciones complementan RC02 y RC04 sin modificar
la numeración de las restricciones existentes.

## 4. Decisiones tecnológicas relacionadas

Las tecnologías seleccionadas para la propuesta son:

- React para la interfaz web.
- Java con Spring Boot para el backend.
- PostgreSQL para la persistencia.
- Python con FastAPI para el servicio propio de IA.

Su justificación se documentará en
tecnologia/propuesta-tecnologica.md.

El proveedor de notificaciones y la solución de almacenamiento
de fotografías están pendientes de selección.