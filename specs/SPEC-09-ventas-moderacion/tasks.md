# Tareas — SPEC-09

- [x] CA09-01: venta, recibo, cierre y ficha.
- [x] CA09-02/03: permiso independiente, revisión y auditoría.
- [x] CA09-04: modo de pruebas autenticado y recepción deshabilitada por defecto.
- [x] Pruebas y evidencia.

El 21/11/2026 pasaron 12 pruebas Java dirigidas, incluyendo 4 escenarios HTTP
de catálogo, venta y moderación en PostgreSQL aislado. La regresión final
aprobó 164 pruebas Java, 227 pruebas React, TypeScript y build PWA. Chrome
headless renderizó las vistas de SPEC-07–09 con datos sintéticos y API
simulada a 360 y 1424 px sin desbordamiento horizontal. El flujo integrado
manual sigue pendiente.

Para un ensayo aislado, el operador habilita únicamente una cuenta
sintética en `identidad.permiso_moderacion` y puede usar
`GANADOCONECTA_REPORTES_PRUEBA_AUTENTICADA=true` en ese entorno. En los demás
entornos mantener la recepción desactivada hasta resolver la política de RF16.
