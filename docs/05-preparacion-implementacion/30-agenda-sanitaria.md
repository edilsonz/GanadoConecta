# Agenda sanitaria — SPEC-02 S06

V8 crea actividades sanitarias futuras por animal. La API de ganadería permite
programar, listar/filtrar y completar. Los estados visibles `PROXIMA`, `VENCIDA`
y `COMPLETADA` usan `America/Lima`. Completar cierra la actividad con auditoría y
no crea un evento clínico, dosis ni recomendación.

React muestra la agenda de la ganadería, enlaza el animal abierto en la ficha y
permite consultar por animal. Propietario/editor modifican; lector solo consulta.
La navegación habilita «Agenda sanitaria».

La validación dirigida aprobó 10/10 pruebas backend y 45/45 frontend, además de
TypeScript. Cubre V8, reintento idempotente, estados, cierre, roles y aislamiento.
Usa PostgreSQL aislado y fixtures transaccionales. No se han creado
actividades en la base real ni se ha ejecutado una prueba manual en Chrome.
