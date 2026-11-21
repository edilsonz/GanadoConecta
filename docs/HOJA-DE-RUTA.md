# Hoja de ruta de GanadoConecta

Los [requisitos originales](01-analisis-de-sistema/03-requisitos-funcionales.md), las [historias](01-analisis-de-sistema/02-historias-de-usuario.md) y los [actores](01-analisis-de-sistema/01-actores.md) definen el alcance. La [matriz](MATRIZ-DE-TRAZABILIDAD.md) sigue los 22 RF y 23 HU; la [rúbrica](RUBRICA-DE-EVALUACION.md) recoge las evidencias docentes.

| Módulo | Incrementos disponibles | Trabajo pendiente |
|---|---|---|
| 1. Autenticación y perfiles | Registro, sesión y logout online | Actualizar datos personales y de contacto; decidir verificación del correo. |
| 2. Ganaderías y acceso familiar | Creación, membresías, invitaciones de uso único, roles y revocación | Transferencia de titularidad si se aprueba. |
| 3. Registro individual y masivo de ganado | Alta individual, ficha y listado; lote online con resultados por fila | Edición de animales y política de conflictos de edición; formatos de importación si se aprueban. |
| 4. Sanidad, calendario y recordatorios | Vacunas, historial, agenda, recordatorios internos y campañas confirmadas por animal | Captura de baños individuales en la interfaz; anticipación configurable y entrega externa EXT01. |
| 5. Reproducción, partos y genealogía | Eventos, gestaciones informadas, partos, parentescos y auditoría | Aprobar la regla de cálculo de fecha probable de parto antes de implementarla. La fecha manual sigue disponible. |
| 6. Peso e historial productivo | Peso histórico en kg, variación y gráfico | Definir los demás indicadores productivos con el responsable. |
| 7. Catálogo, anuncios y ventas | Catálogo público, anuncios, fotos autorizadas, proyección acotada de genealogía, venta y moderación | Aprobar quién puede reportar, proveedor de fotos, caché e invalidación. |
| 8. Administración de usuarios | Permiso administrativo específico de moderación | Gestión de cuentas, información general y estadísticas administrativas. |
| 9. Reportes y estadísticas | Historiales de ficha como fuente | Reportes sanitarios, reproductivos y productivos. |
| 10. Inteligencia artificial y alertas | Alcance y arquitectura definidos | Análisis histórico, alertas por tendencias e IA conversacional sustentada en fuentes. |

## Aspectos transversales

- **Offline.** La cola ya cubre alta individual y eventos sanitarios seleccionados. Quedan por precisar consulta privada offline, edición/versiones, campañas, reproducción, peso, lotes y demás ampliaciones, con pruebas de expiración y sincronización.
- **Seguridad.** Mantener autorización por ganadería, revisión de exposición pública, permisos administrativos separados y operación de credenciales/secretos fuera de Git.
- **Infraestructura y despliegue.** Flyway y Docker para reproducción local. Pendientes proveedor de fotos, Redis con invalidación, entrega EXT01, CI/CD, hosting, dominio, DNS y CDN.
- **Interfaz.** Espacio del productor con navegación por módulo y ficha por pestañas. [Evidencia visual y de regresión](05-preparacion-implementacion/36-consolidacion-ux.md). Pendiente validación integrada con usuarios.
- **Pruebas.** Conservar evidencia unitaria e integración. Los [E2E existentes](../tests/e2e/README.md) cubren SPEC-07 a SPEC-09 y navegación. Quedan pendientes los E2E offline, los planes de consulta y k6 sobre datos sintéticos reproducibles.
- **Arquitectura.** Consultar las [vistas vigentes y fuentes Draw.io](02-arquitectura-software/diagramas/README.md) para el informe final.
