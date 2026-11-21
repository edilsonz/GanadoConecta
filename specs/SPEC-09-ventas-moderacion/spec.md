# SPEC-09 — Ventas y moderación

ACT01/ACT03, HU15/HU16/HU17, RF15/RF16, AC04/AC05 y ADR-04/ADR-08. ACT02 participa en la propuesta de reportes públicos.

- El propietario autorizado registra la venta online. La venta conserva UUID e historiales, marca el animal como vendido y cierra anuncios publicados o pausados en una transacción. Un anuncio no puede publicarse si el animal está vendido. No se registra pago, comprador ni transferencia.
- Recibo de venta de una sola operación por animal. El mismo actor y UUID de operación recuperan el resultado original; cualquier otra operación para la venta ya registrada obtiene conflicto.
- Un administrador activo se habilita mediante un permiso de plataforma independiente de las membresías. Consulta reportes con motivo y el título o estado del anuncio, sin acceder a historiales privados. Puede desestimar o retirar. Se registra actor, momento y decisión.
- Los reportes admiten motivo de 5 a 1000 caracteres y se asocian a un anuncio publicado. **Pendiente de aprobación:** autenticación obligatoria para reportar. La recepción se mantiene deshabilitada en la configuración predeterminada. El modo autenticado queda como propuesta verificable en entorno aislado. Si se aprueba el modo público, requiere definir controles de abuso y privacidad antes de activarse.

| Criterio | Verificación |
|---|---|
| CA09-01 | La venta es transaccional, conserva ficha e historiales, y no se duplica. |
| CA09-02 | Roles de la ganadería no otorgan acceso a moderación ni el permiso administrativo da acceso a fichas. |
| CA09-03 | Retiro administrativo excluye anuncio y fotos, conserva decisión auditada y rechaza cambios incompatibles. |
| CA09-04 | El reporte requiere motivo y la política pendiente mantiene bloqueada su recepción predeterminada. |
