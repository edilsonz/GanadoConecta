# Prompt 02 — Generar proyecto base ejecutable (solo tras aprobar etapa 0)

Lee `AGENTS.md`, docs originales y `docs/05-preparacion-implementacion`, más las especificaciones SDD aprobadas por la etapa anterior. No asumas que los documentos v2 reemplazan automáticamente los originales; usa los revisados.

Crea un **esqueleto mínimo ejecutable** de GanadoConecta, evitando desarrollar todo a la vez:
- `frontend/`: React, TypeScript, rutas, diseño responsivo mobile-first, estructura de componentes, PWA básica con mensaje offline, manejo de sesión y errores. Pantallas iniciales de Ingresar, Inicio y Mi ganado con estados vacíos honestos.
- `backend/`: Java/Spring Boot, monolito modular y separación dominio/aplicación/adaptadores/infraestructura proporcional a la complejidad. API de salud y estructura inicial de módulos. Seguridad preparada sin rutas privadas expuestas.
- `servicio-ia/`: FastAPI con `/health`, sin simular análisis reales ni conectar a proveedor inventado.
- PostgreSQL vía Docker Compose con migraciones (Flyway o alternativa justificada); Redis opcional en compose pero SIN hacerlo necesario para gestionar animales.
- `README` con requisitos, `.env.example`, comandos para levantar, compilar, probar y advertencias de seguridad.
- Pruebas smoke y lint disponibles.

**Criterios de aceptación:** Los tres servicios pueden iniciarse con instrucciones reproducibles; salud backend/IA responde; frontend carga en móvil/escritorio; el backend no depende de Redis/IA para servir el flujo básico. Registra dependencias y versiones exactas elegidas. Ejecuta las pruebas disponibles e informa resultados verdaderos.

No implementes todavía catálogo, IA conversacional, importación masiva ni una sincronización incompleta presentada como terminada. Entrega archivos, comandos, evidencias y limitaciones.
