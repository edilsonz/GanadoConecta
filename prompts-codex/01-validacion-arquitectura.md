# Prompt 01 — Validar y actualizar arquitectura (PRIMERA TAREA)

Actúa como arquitecto de software del proyecto GanadoConecta. Estás trabajando en el repositorio proporcionado.

**Primero lee:** `AGENTS.md`, `README.md`, `docs/01-analisis-de-sistema/`, `docs/02-arquitectura-software/`, `docs/03-diseno-de-software/`, `docs/04-modelo-c4/`, `tecnologia/propuesta-tecnologica.md`, `specs/plantilla-spec.md`, y TODOS los documentos de `docs/05-preparacion-implementacion/`.

**Contexto confirmado:** GanadoConecta es una aplicación web responsiva/PWA para celular y computadora. Prioriza gestión general de ganado vacuno, registro flexible, acceso familiar, historial, genealogía, controles y campañas sanitarias, calendario individual y colectivo, análisis de IA y asistente con fuentes fiables. El catálogo es NO transaccional y solo publica genealogía autorizada. Una venta cierra anuncios y conserva historial; no transfiere la ficha entre cuentas. Soporta operaciones offline mediante IndexedDB y sincronización segura posterior.

**Tu tarea ahora:**
1. Hacer una matriz de coherencia de las decisiones nuevas con RF01–RF16, AC01–AC08, ADR-01–ADR-11, restricciones y C4 existentes.
2. Proponer e integrar, tras identificar conflictos, RF17–RF22, AC09 y ADR-12–ADR-15 (numeraciones sugeridas en el documento v2), preservando redacción y estilo de los documentos originales.
3. Actualizar actores, historias de usuario, requisitos, atributos, restricciones, decisiones, componentes y modelo C4 **de forma consistente**. Mantener fuentes editables de diagramas donde sea viable. No presentar PNG originales como actualizados sin regenerarlos.
4. Definir los límites explícitos de módulos y la protección de genealogía pública versus historiales privados.
5. Crear las especificaciones SDD mínimas para empezar con usuarios/familias, animales y sincronización; usar la plantilla existente.
6. Listar decisiones todavía pendientes y recomendar valores por defecto justificados, sin modificar por tu cuenta el alcance de pagos, diagnóstico o roles veterinarios.

**Restricciones:** Mantener Spring Boot monolito modular/Clean Architecture, React, PostgreSQL, Redis para caché pública, FastAPI independiente y tareas persistentes existentes. No inventar funcionalidades ya desarrolladas. NO implementar todavía todo el frontend o backend. No incluir secretos ni cambiar el ZIP original.

**Resultado esperado:** Resumen de archivos modificados, tabla antes/después, decisiones incorporadas, riesgos, fuentes consultadas y pruebas de consistencia documental realmente realizadas. Si algo contradice los originales, señalarlo explícitamente.
