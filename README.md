# GanadoConecta

Sistema Inteligente para la Gestión Integral de la Actividad Ganadera.

## Datos del proyecto

- **Autor:** Carlos Edilson Huarcaya Palomino
- **Universidad:** Universidad Nacional de San Cristóbal de Huamanga
- **Curso:** Arquitectura de Software

## Descripción

GanadoConecta es una aplicación web orientada a productores de ganado
vacuno. Permitirá gestionar animales, mantener sus historiales sanitarios,
reproductivos y productivos, programar actividades, consultar reportes
y analizar registros históricos mediante inteligencia artificial.

También contará con un catálogo de anuncios de animales en venta,
donde los interesados podrán consultar la información publicada y
contactar al vendedor mediante el medio que este autorice.

## Alcance inicial

- Gestión individual de animales e historiales.
- Registro de actividades sanitarias, reproductivas y productivas.
- Calendario, recordatorios y alertas.
- Análisis de registros históricos mediante inteligencia artificial.
- Consulta de reportes.
- Publicación, búsqueda y moderación de anuncios de venta.
- Gestión de usuarios y permisos.

Los historiales completos serán privados. Los anuncios mostrarán
únicamente información autorizada por el productor.

La negociación, los pagos y el transporte se realizarán fuera de la
plataforma. La IA no realizará diagnósticos ni sustituirá la evaluación
de un profesional veterinario.

## Arquitectura propuesta

Arquitectura cliente-servidor con un backend monolítico modular
organizado en capas y un servicio propio de IA independiente.

El backend aplicará Clean Architecture para separar las reglas del
negocio, los casos de uso y los detalles tecnológicos.

## Tecnologías seleccionadas

| Parte | Tecnología |
|---|---|
| Interfaz web | React |
| Backend | Java y Spring Boot |
| Base de datos | PostgreSQL |
| Caché | Redis |
| Servicio de IA | Python y FastAPI |
| Control de versiones | Git y GitHub |

El proveedor de notificaciones y la solución de almacenamiento de
fotografías están pendientes de selección.

## Organización del repositorio

| Carpeta | Contenido |
|---|---|
| docs/01-analisis-de-sistema/ | Análisis y requisitos del sistema |
| docs/02-arquitectura-software/ | Propuesta arquitectónica |
| docs/03-diseno-de-software/ | Diseño interno del software |
| docs/04-modelo-c4/ | Documentación de los modelos C4 |
| docs/05-preparacion-implementacion/ | Propuestas de preparación; pendientes de revisión |
| docs/06-evaluacion-sdd/ | Ruta SDD y criterios de evaluación |
| img/ | Imágenes de los diagramas arquitectónicos y modelos C4 |
| tecnologia/ | Propuesta tecnológica |
| specs/ | Una carpeta por funcionalidad con spec.md, plan.md y tasks.md; plantilla original conservada |
| prompts-codex/ | Instrucciones reutilizables para tareas de Codex |

Consultar la [guía de preparación](docs/05-preparacion-implementacion/README.md),
la [ruta SDD](docs/06-evaluacion-sdd/ruta-sdd-rubrica.md) y las
[convenciones de especificaciones](specs/README.md).

Organización prevista para la implementación (estas carpetas aún no existen):

| Ruta futura | Responsabilidad |
|---|---|
| frontend/ | Aplicación React y sus pruebas unitarias y de integración |
| backend/ | Monolito modular Java/Spring Boot, migraciones y pruebas unitarias y de integración |
| servicio-ia/ | Servicio Python/FastAPI independiente y sus pruebas |
| tests/e2e/ | Pruebas de flujos completos entre servicios |
| tests/carga/ | Escenarios, datos sintéticos y resultados de carga documentados |
| infra/ | Configuración de entornos, servicios locales y despliegue sin secretos |
| .github/workflows/ | Automatización de CI/CD cuando se defina |

Las herramientas de pruebas se revisarán y documentarán antes de implementarlas.
Las carpetas se crearán cuando tengan contenido real; la infraestructura
de despliegue de infra/ se distingue de los adaptadores internos del backend.

## Estado del proyecto

En fase de documentación y diseño arquitectónico.
La implementación se desarrollará posteriormente.

Se utilizará Redis como caché para consultas frecuentes del catálogo
público de anuncios. PostgreSQL conservará los datos permanentes
y será la fuente principal de información.

Los resultados almacenados en caché tendrán un tiempo de caducidad.
Al publicar, editar, pausar, retirar, moderar o marcar como vendido
un anuncio, se invalidará la caché afectada después de confirmar
el cambio en PostgreSQL.

Si la información no está en caché o Redis no está disponible,
el backend consultará PostgreSQL. La caché no almacenará
historiales privados ni archivos de fotografías.


## Sprint 01: Documentación técnica del proyecto

### Objetivo del Sprint

Organizar, revisar y consolidar la documentación técnica de GanadoConecta, estableciendo las bases para el desarrollo del Sistema Inteligente para la Gestión Integral de la Actividad Ganadera.

### Entregables desarrollados

- Análisis del sistema: actores, historias de usuario y requisitos.
- Identificación de atributos de calidad, restricciones y drivers arquitectónicos.
- Definición de la arquitectura de software y propuesta tecnológica.
- Diagramas C4 de contexto, contenedores y componentes (niveles 1, 2 y 3).
- Diseño interno de los módulos del sistema.
- Aplicación propuesta de principios SOLID y patrones de diseño.
- Revisión de la documentación y coherencia arquitectónica.

### Gestión del Sprint

Las actividades del Sprint 01 se organizan mediante un tablero Kanban en Trello, utilizando las columnas:

BACKLOG → SPRINT ACTUAL → EN PROCESO → EN REVISIÓN → TERMINADO.

El tablero permite realizar el seguimiento de actividades, verificar avances y registrar observaciones durante la revisión.

### Estado del Sprint

**Estado:** Documentación técnica preparada para entrega y pendiente de evaluación docente.

La implementación del sistema se desarrollará en etapas posteriores.
