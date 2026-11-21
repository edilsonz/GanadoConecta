# SPEC-05 — Control de peso

ACT01 → HU03/HU06 → RF03/RF06 (parciales) → AC04/AC05, ADR-04/ADR-13.

La ficha privada admite mediciones de peso en kg con fecha y observaciones opcionales. Propietario/editor vigentes registran y los tres roles activos consultan. La medición se asocia a un animal de la misma ganadería. El control solo registra datos informados por el usuario.

Decisiones técnicas. UUIDv4 como identidad de la medición e idempotencia por coincidencia de actor, ámbito y datos. El peso decimal es positivo, con hasta tres decimales y nueve cifras enteras. Fecha obligatoria hasta el día actual en Lima. Observaciones de hasta 2000 caracteres. En orden cronológico, las mediciones del mismo día se ordenan por recepción y UUID.

| Criterio | Resultado |
|---|---|
| PE01 | Alta válida, reenvío sin duplicado y conflicto si el UUID ya representa otro contenido. |
| PE02 | Peso no positivo, escala no admitida y fecha inválida se rechazan. |
| PE03 | Lectura y escritura respetan membresía vigente, rol y aislamiento. |
| PE04 | Historial, último peso y diferencia en kg frente a la medición anterior. El gráfico sitúa las mediciones según fecha. Se distinguen vacío y fallo. |

Sin indicadores adicionales, reglas clínicas, edición de mediciones ni protocolo offline nuevo.
