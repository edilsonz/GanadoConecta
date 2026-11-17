# BASE-01 — Primera estructura ejecutable

Estado: alcance técnico autorizado por el responsable el 16 de noviembre de 2026;
base técnica y conexión PostgreSQL/JDBC verificadas el 17 de noviembre de 2026.
No completa SPEC-01.

## Objetivo y trazabilidad

Preparar proyectos ejecutables React/TypeScript/Vite y Spring Boot, configuración
local PostgreSQL y pruebas básicas. Relacionado con T03 de SPEC-01, RC01/RC02/RC04,
AC06 y ADR-01/ADR-03/ADR-04/ADR-07. No se crean requisitos funcionales nuevos.
Actores: desarrollador local; visitante de pantalla inicial y salud pública.

## Entradas, resultados y reglas

Configuración mediante entorno; sin datos privados. Una pantalla responsiva verde
blanca muestra identidad, navegación preparada y estado de API consultado realmente.
Sin animales ficticios, acceso simulado, PWA/offline ni formularios de negocio.
Backend monolítico modular; núcleo de futuras funciones sin dependencias de framework.
Salud pública solo devuelve status; todas las demás rutas se deniegan por defecto.
PostgreSQL es independiente del arranque básico; perfil postgres opcional requiere
configuración explícita. Redis/IA/notificaciones no se incorporan en esta base.

## Flujo y errores

Instalar dependencias, iniciar API, iniciar interfaz y consultar salud. Si falla
la API se informa que no se pudo comprobar conexión, nunca un éxito inventado.
Sin Docker se conserva Compose documentado y no se declara probada la base de datos.
Sin variables obligatorias no se inicia el perfil de persistencia.

## Criterios y pruebas

| ID | Criterio | Verificación |
|---|---|---|
| BASE-CA01 | Frontend compila con TypeScript y muestra inicio responsivo sin registros inventados. | Build, pruebas de renderizado y HTTP del servidor. |
| BASE-CA02 | API arranca sin BD y GET /api/health devuelve solo status. | Prueba HTTP de integración y arranque real local. |
| BASE-CA03 | Otras rutas se deniegan sin ofrecer autenticación falsa. | Pruebas de seguridad de API. |
| BASE-CA04 | Conexión fallida o válida se representa según respuesta real. | Pruebas de componente con red controlada. |
| BASE-CA05 | PostgreSQL tiene configuración reproducible, volumen, healthcheck y secretos externos. | Compose healthy, consulta SQL y prueba JDBC real por el puerto publicado. |
| BASE-CA06 | README permite instalar, ejecutar y probar; variables privadas ignoradas. | Revisión de comandos, gitignore y evidencias. |

No aplican pruebas de carga de animales/anuncios ni CA de negocio de SPEC-01:
esos endpoints aún no existen. Pruebas de BD y rendimiento se ejecutarán cuando
esté disponible el entorno/flujo correspondiente, sin falsos resultados.
