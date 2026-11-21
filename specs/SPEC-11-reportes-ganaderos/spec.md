# SPEC-11 — Reportes y estadísticas ganaderas

ACT01 · HU10 · RF10 con RF03–RF06. Propietario, editor y lector con membresía activa consultan `GET /api/v1/ganaderias/{id}/reportes`. Parámetros `desde` y `hasta` (inclusivos, ISO y fecha de negocio America/Lima), y `animalId` opcional. La interfaz ofrece períodos y un selector paginado de animales. El reporte de peso requiere elegir un animal para comparar pesajes de la misma ficha.

- Sanidad: cuenta cada fila de evento sanitario una vez y agrupa por tipo. Una atención de campaña registrada en el historial cuenta como un evento, incluso si la campaña tiene varios participantes.
- Reproducción: separa montas, inseminaciones y evaluaciones por resultado registrado. Partos y crías se agrupan por fecha de parto. El filtro por animal aplica a la madre en partos y a la ficha del evento reproductivo. Cada cría vinculada al parto se cuenta una vez.
- Peso: total en el período, último y hasta 50 mediciones recientes en orden cronológico para gráfico y tabla. Delta en kg respecto a la medición anterior dentro del período por fecha, instante de registro e ID. La primera medición sin anterior tiene delta nulo.
- Las vistas usan solo registros confirmados y conservan historiales de animales vendidos. Rango inválido devuelve 400. Ganadería o animal sin acceso devuelve 403 sin distinguir pertenencia o existencia.

| Criterio | Verificación |
|---|---|
| CA11-01 | Los agregados corresponden a los registros confirmados. Período inclusivo, ausencia de datos explícita y campañas sin duplicación. |
| CA11-02 | Los tres roles de una ganadería pueden consultar. Terceros y membresías revocadas quedan excluidos. |
| CA11-03 | Evolución y variaciones de peso con datos reales, unidades y alternativa tabular. |

Exportación CSV y nuevos indicadores quedan fuera de esta entrega. Las estadísticas no interpretan observaciones como diagnósticos ni emiten recomendaciones.
