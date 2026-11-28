# Redis y vigencia del catálogo público

Redis 8.4.6 se integra con Spring Boot a través de
`CatalogoAnuncios.CachePublica` y `CacheCatalogoRedis`. Está desactivado
por defecto. El diagnóstico de arquitectura y las condiciones de
reproducción están en el [arnés de rendimiento](../../../tests/rendimiento/README.md).

## Configuración y flujo

| Parámetro | Valor local predeterminado | Función |
|---|---|---|
| `GANADOCONECTA_CACHE_CATALOGO` | `false` | Opt-in de la caché en Spring. |
| `GANADOCONECTA_CACHE_TTL_SEGUNDOS` | 30 | Retención de 1 a 300 s en cada clave. |
| `REDIS_HOST`, `REDIS_PORT` | `127.0.0.1`, 6379 | Destino del cliente. Compose publica en loopback. |
| `REDIS_CONNECT_TIMEOUT`, `REDIS_COMMAND_TIMEOUT` | 150 ms, 100 ms | Plazos separados para conectar y ejecutar comandos. |
| `REDIS_USER`, `REDIS_PASSWORD`, `REDIS_TLS` | Por entorno | Acceso y transporte cuando se configure un Redis externo. |

1. Spring valida los filtros y lee la revisión UUID actual en PostgreSQL.
2. Busca en Redis una clave de esa revisión, proyección, orden, filtros y página.
3. Ante ausencia o vencimiento, consulta PostgreSQL. Guarda la proyección
   pública por el TTL indicado. Los binarios de fotos tienen su propia
   autorización de lectura en PostgreSQL.
4. Ante fallo de conexión, formato o plazo, vuelve a PostgreSQL. Registra
   el tipo de fallo y omite Redis durante dos segundos. Una solicitud
   posterior reintenta. Micrometer cuenta aciertos, ausencias, fallos y
   omisiones. La prueba de carga observa también los contadores de Redis.

La proyección incluye títulos, características autorizadas y referencias
de fotos. Excluye identificadores privados de ganadería y animal, sesiones,
secretos y historiales. El acceso usa un Redis de confianza y la configuración
local restringe red y memoria. Producción requiere autenticación y TLS según
la topología que se apruebe.

## Invalidación y concurrencia

V18 incorpora una revisión por base. V19 difiere su actualización hasta
la confirmación y evita repetirla dentro de una misma transacción.
La venta puede tomar primero el bloqueo del animal, mientras que una
edición toma el del anuncio. La revisión diferida evita tomar el bloqueo
compartido en medio de esas escrituras.

Los lectores que comienzan después de la confirmación usan la revisión nueva.
Una lectura que se solapa con el cambio puede terminar con el estado que
observó antes. Aunque esa lectura escriba después su clave anterior,
la siguiente consulta comprueba la revisión vigente. Esto también protege
al reiniciar Redis o al recuperar una clave histórica. Una reversión no
publica una nueva revisión. No se depende de un borrado remoto para retirar
un anuncio. Las entradas inaccesibles quedan hasta que expira su TTL.

Esta garantía añade una consulta ligera a PostgreSQL por búsqueda y un
bloqueo común al confirmar cambios públicos. Redis reduce el trabajo
de la proyección repetida. El PostgreSQL primario sigue en la ruta crítica.
[Contrato SDD](../../../specs/SPEC-08-catalogo-anuncios/plan.md)

## Verificación en PostgreSQL y Redis reales

| Verificación | Resultado |
|---|---|
| Consulta fría, acierto, filtros, páginas, proyección y TTL de 2 s | Aprobada. El contador de aciertos aumentó, las claves expiraron y la lectura siguiente volvió a consultar PostgreSQL. |
| Edición, pausa, retirada, venta HTTP y reversión SQL | Aprobada. Después de confirmar, se vio el título editado o desapareció el anuncio. La reversión conservó revisión y proyección. |
| Lector iniciado antes de una pausa | Aprobada. Pudo completar su lectura anterior, y su escritura tardía no volvió a exponer el anuncio a una consulta posterior. |
| Edición concurrente con venta | Aprobada. La prueba coordinó ambos bloqueos y ambas transacciones terminaron. El anuncio quedó fuera de la página. |
| Redis detenido, pausa y reinicio | Aprobada. Spring devolvió la misma página desde PostgreSQL. Una pausa confirmada durante la caída siguió vigente tras el reinicio, incluso al reinyectar la clave sintética anterior. Se observaron nuevos aciertos tras la recuperación. |

[Pruebas Java](../../../backend/src/test/java/pe/ganadoconecta/CatalogoRedisIntegrationTest.java) ·
[Guion de caída y reinicio](../../../tests/rendimiento/redis-ciclo.ps1) ·
[Resultado del ciclo real](redis-ciclo.json)
