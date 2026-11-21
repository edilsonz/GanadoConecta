# Fuentes y vistas de la arquitectura

Las dos fuentes Draw.io corresponden al estado del código en `c961a0b`.
Cada página tiene una vista SVG exportada para su lectura desde Markdown.
Las figuras propuestas de la etapa inicial están en el
[archivo histórico](../../../img/README.md#archivo-histórico).

| Fuente editable | Índice de página para exportar | Vista SVG | Referencia |
|---|---:|---|---|
| [ganadoconecta-c4.drawio](../../04-modelo-c4/diagramas/ganadoconecta-c4.drawio) | 0 | [Contexto](../../04-modelo-c4/diagramas/nivel-1-contexto.svg) | [C4 nivel 1](../../04-modelo-c4/Nivel1-DiagramaContexto.md) |
| C4 | 1 | [Contenedores](../../04-modelo-c4/diagramas/nivel-2-contenedores.svg) | [C4 nivel 2](../../04-modelo-c4/Nivel2-DiagramaContenedores.md) |
| C4 | 2 | [Componentes](../../04-modelo-c4/diagramas/nivel-3-componentes.svg) | [C4 nivel 3](../../04-modelo-c4/Nivel3-DiagramaComponentes.md) |
| [ganadoconecta-tecnico.drawio](ganadoconecta-tecnico.drawio) | 0 | [Clean Architecture](clean-architecture.svg) | [Alta individual, SPEC-01](../../../specs/SPEC-01-registro-ganado-offline/spec.md) |
| Técnica | 1 | [Modelo de datos](modelo-de-datos.svg) | [Flyway V1–V16](../../../backend/src/main/resources/db/migration/) |
| Técnica | 2 | [Despliegue local](despliegue-local.svg) | [Configuración local](../../../README.md) y [entorno E2E](../../../tests/e2e/README.md) |
| Técnica | 3 | [Sincronización offline](sincronizacion-offline.svg) | [Ganado, SPEC-01](../../../specs/SPEC-01-registro-ganado-offline/spec.md) y [sanidad, SPEC-02](../../05-preparacion-implementacion/32-sincronizacion-sanitaria-offline.md) |

Las cajas verdes representan actores y componentes del alcance vigente. El
azul identifica persistencia. Las cajas ámbar discontinuas indican elementos
pendientes. El modelo lógico de datos selecciona relaciones representativas.
Las migraciones contienen el esquema físico completo.

## Actualizar una figura

1. Abre la fuente Draw.io indicada. Edita sus elementos o conectores y guarda
   en la misma fuente.
2. Exporta solo la página modificada al SVG asociado. Este ejemplo se ejecuta
   desde la raíz del repositorio con Draw.io de escritorio.

```powershell
& 'C:\Program Files\draw.io\draw.io.exe' --export --format svg --svg-theme light --embed-svg-fonts false --border 20 --page-index 0 --output 'docs/04-modelo-c4/diagramas/nivel-1-contexto.svg' 'docs/04-modelo-c4/diagramas/ganadoconecta-c4.drawio'
```

3. Abre el SVG y verifica títulos, conectores y estados. Actualiza los enlaces
   y la trazabilidad si cambió el comportamiento.
