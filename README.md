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
| img/ | Imágenes y diagramas editables |
| tecnologia/ | Propuesta tecnológica |

## Estado del proyecto

En fase de documentación y diseño arquitectónico.
La implementación se desarrollará posteriormente.