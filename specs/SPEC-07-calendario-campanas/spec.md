# SPEC-07 — Calendario, recordatorios y campañas

RF04/RF07/RF22 → HU04/HU07/HU23 → AC04/AC05. ACT01 con membresía activa consulta; PROPIETARIO y EDITOR gestionan.

- El calendario conserva las actividades individuales de SPEC-02 y agrega actividades sanitarias, reproductivas y productivas con uno o varios animales. Al consultar se muestran las programadas (futuras), pendientes (hoy), vencidas y realizadas.
- Una campaña es una actividad explícita de vacunación, desparasitación o baño antiparasitario. La confirmación selecciona los animales realmente atendidos, registra la fecha realizada hasta hoy y exige nombre de vacuna si corresponde. Se cierra la campaña una vez y se conserva la lista de participantes y atendidos. Una actividad común puede marcarse realizada sin crear hechos clínicos.
- La interfaz muestra avisos internos para hoy y atrasos. Las próximas fechas quedan visibles en el calendario. Anticipación configurable, entrega externa y campañas offline quedan pendientes.

| Criterio | Verificación |
|---|---|
| CA07-01 | Calendario cronológico, roles vigentes, filtro por participante y estados. |
| CA07-02 | Solo el subconjunto seleccionado recibe hechos sanitarios, con cierre atómico. |
| CA07-03 | Reintento idéntico conserva el resultado y un cambio de selección tras cerrar entra en conflicto. |
| CA07-04 | Avisos internos disponibles sin IA. EXT01 queda como puerto sin proveedor. |

Límites técnicos del incremento. Hasta 100 participantes por actividad. La fecha de negocio usa America/Lima. No se infieren dosis, tratamientos ni fechas.
