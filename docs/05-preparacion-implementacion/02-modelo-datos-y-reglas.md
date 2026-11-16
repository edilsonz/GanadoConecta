# Modelo de datos conceptual y reglas — propuesta, no migraciones

> **Estado de consolidación:** las 16 decisiones aprobadas por el responsable
> del proyecto se integran en la [base consolidada](05-consolidacion-arquitectonica.md).
> Los detalles de este documento que no figuren como aprobados allí siguen
> siendo propuestas pendientes de SPEC. No hay implementación ni pruebas funcionales.

## Entidades centrales

| Entidad | Campos esenciales propuestos | Reglas |
|---|---|---|
| Usuario | id UUID, nombre, credenciales, estado | Cuenta individual, no compartir contraseñas |
| Ganaderia | id UUID, nombreVisible, propietarioUsuarioId, estado | Espacio de trabajo compartido |
| Membresia | id, ganaderiaId, usuarioId, rol, estado | `propietario`, `editor`, `lector`; revocación comprobada en backend |
| Animal | id UUID, ganaderiaId, sexo, nombreOpcional, areteTextoOpcional, razaOpcional, fechaNacimientoOpcional, categoria, estado, version | UUID es identificador; arete puede repetirse si no se valida, advertir duplicados dentro de ganadería |
| Parentesco | animalId, tipo (`madre`/`padre`), progenitorAnimalId o referenciaExterna, fuente, verificado | Referencia opcional; evitar ciclos, sexo incompatible y autocontención |
| Nacimiento | id, madreId, fecha, observaciones | Una madre y varias crías relacionadas; padre opcional |
| RegistroPeso | id UUID, animalId, fecha, valorKg, autorId, origen, estado | Fecha, unidad y rangos; advertencias de valores anómalos |
| CampanaSanitaria | id UUID, ganaderiaId, tipoActividad, fecha, descripción, responsable, estado | La campaña no implica que todos fueron atendidos |
| AplicacionSanitaria | id UUID, campanaId opcional, animalId, fecha, resultado, productoOpcional, dosisOpcional, viaOpcional, proximaFechaOpcional, observacionesOpcional | Registrar por animal efectivamente tratado, no asumir aplicación masiva automática |
| ActividadCalendario | id, ganaderiaId, alcance, fecha, tipo, estado | Alcance: individual, selección de animales o todos; registrar destinatarios |
| Anuncio | id, animalId, título, descripción, precioSolicitado, ubicacionGeneral, contactoAutorizado, mostrarGenealogia, estadoPublicacion, estadoModeracion | Proyección pública con campos permitidos; `mostrarGenealogia=false` por defecto |
| Foto | id, animalId o anuncioId, referenciaObjeto, propietario, permisoVisualizacion, tamaño, formato | Binario fuera de PostgreSQL, metadata adentro |
| OperacionSincronizacion | operationId UUID, actor, ganaderia, entidad, tipo, recibidoEn, resultado, hashPayload opcional | Dedupe server-side, manejo de conflictos |
| TareaPersistente | id, tipo, estado, intentos, reintentoEn | Ya prevista en ADR-09; análisis/notificaciones, no confundir con sync |
| FuenteConocimiento | id, título, URL/documento, organismo, fecha, revisadoEn, estado | Solo fuentes aprobadas para explicación general IA |

### Relación de genealogía

- Padre/madre pueden ser registros `Animal` de la misma ganadería o referencias externas controladas.
- La ascendencia puede estar incompleta. Indicar al usuario «No registrado».
- No representar como pedigrí certificado aquello declarado sin evidencia.
- El parentesco público puede requerir ocultar nombres/identificadores de terceros o información no autorizada; publicar solo una proyección explícita.

### Ciclo de vida de vacunos

- Estados principales: `ACTIVO`, `VENDIDO`, `FALLECIDO` (se pueden ampliar mediante especificación).
- `sexo`: `HEMBRA` o `MACHO` (otras necesidades de registro veterinario deberán discutirse si surgen).
- Categorías iniciales: `TERNERA`, `VAQUILLA`, `VACA`, `TERNERO`, `TORETE`, `TORO` (respetar las denominaciones de la interfaz).
- Evitar avance automático basado solo en edad: `VACA` exige registro de primer parto o confirmación con respaldo según regla definida; `TORO` requiere condición de reproductor reconocida.
- Animal sin fecha de nacimiento podrá tener categoría declarada o quedar `SIN_CLASIFICAR` hasta confirmación.
- Venta actualiza estado y cierra anuncios activos en **una transacción**, conserva registros históricos (ADR-04/08).

## Reglas de acceso iniciales

| Operación | Propietario | Editor familiar | Lector familiar |
|---|---|---|---|
| Ver animales e historiales privados de la ganadería | Sí | Sí | Sí |
| Registrar peso, vacunación, nacimiento | Sí | Sí | No |
| Gestionar miembros | Sí | No | No |
| Publicar anuncio/cerrar venta | Sí | No | No |

## Decisiones para implementación SDD

1. Prevenir códigos de arete duplicados detectables dentro de una ganadería, sin rechazar animales sin arete; un solo código familiar como «H.P.» puede representar marca de propiedad y no identificación individual.
2. Elegir cómo modelar el nombre frontal y marca posterior del arete sin confundirlos con `Animal.nombre` ni la `Ganaderia`: campos opcionales `areteFrente`, `areteReverso`, `codigoArete` solo si son relevantes. No imponer número de arete obligatorio.
3. En carga CSV definir máximo de filas, previsualización, errores por fila, idempotencia y confirmación antes de guardar.
4. En campañas colectivas confirmar animales aplicados, no aplicados y pendientes; el editor puede registrar los hechos, pero no definir tratamientos médicos mediante IA.
5. En venta offline advertir la demora pública; limitar publicación de anuncios a conexión en primera entrega si es necesario.
