# Contratos OpenAPI del Sprint 2

El monolito sirve Swagger UI bajo el perfil explícito `openapi` y conserva la
seguridad habitual de las operaciones. Con `postgres,openapi` aparecen los
módulos de persistencia y la comprobación JDBC de salud. En perfiles
`prod` y `production` las rutas de documentación quedan protegidas aunque
se solicite `openapi`. La evidencia usa un proceso local y datos sintéticos.

| Recurso | URL o archivo |
|---|---|
| Swagger UI local del entorno de prueba | [http://127.0.0.1:18082/swagger-ui/index.html](http://127.0.0.1:18082/swagger-ui/index.html) |
| OpenAPI JSON generado por Spring | [http://127.0.0.1:18082/v3/api-docs](http://127.0.0.1:18082/v3/api-docs) |
| OpenAPI YAML generado por Spring | [http://127.0.0.1:18082/v3/api-docs.yaml](http://127.0.0.1:18082/v3/api-docs.yaml) |
| Contratos registrados | [Spring Boot](spring.json) · [FastAPI interno](fastapi-interno.json) |
| Navegador real | [Swagger por módulos](swagger-modulos.png) · [Operación CSRF](swagger-csrf.png) |

Springdoc 2.10.1 genera la estructura de parámetros y DTO a partir de Spring
MVC. El catálogo de metadatos en `configuracion/openapi` describe operaciones,
respuestas, permisos, paginación y casos de cuerpo procesado manualmente.
`OpenApiPostgresIntegrationTest` compara rutas y métodos con los mappings
efectivos y comprueba referencias, Swagger, autenticación y CSRF. En el
entorno local se verificaron 53 operaciones explícitas en 44 rutas y 13 módulos.
[Matriz oficial de compatibilidad Springdoc](https://springdoc.org/v2/faq.html#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot)

## Reproducir

Con PostgreSQL aislado y claves de prueba, sigue la [guía HTTPS](../../../tests/e2e/README.md)
para preparar base, archivos, llaves y puertos. Agrega el perfil de documentos
al comando de Spring. Desde `backend/`:

```powershell
mvn.cmd -B -ntp spring-boot:run "-Dspring-boot.run.profiles=postgres,openapi"
```

Si se usa el puerto predeterminado, sustituye 18082 por 8080 en las URL. Para
obtener una copia local desde la raíz con el backend arriba:

```powershell
curl.exe --fail --silent --show-error http://127.0.0.1:18082/v3/api-docs -o .cache/openapi-spring.json
```

Las operaciones privadas exigen JWT asociado a una sesión vigente. Las
escrituras usan la cookie y `X-CSRF-TOKEN` obtenidos en
`GET /api/v1/auth/csrf`, además de un origen admitido. Las operaciones de
ganadería aplican el rol correspondiente en el servidor. Para probar escrituras
con el origen HTTPS configurado usa los recorridos de la PWA. La captura de
Swagger no registra credenciales ni resultados privados.

FastAPI documenta la ruta `POST /interno/v1/inferir` mediante exportación
sin publicar `/docs`, `/redoc` ni `/openapi.json`. Desde `servicio-ia/`,
con las [dependencias de pruebas](../../../servicio-ia/requirements-test.txt):

```powershell
python -m ganadoconecta_ia.exportar_openapi | Set-Content -Encoding ascii ../.cache/openapi-fastapi.json
python -m openapi_spec_validator ../.cache/openapi-fastapi.json
```

El exportador usa una credencial efímera interna para inicializar el contrato.
El esquema contiene el nombre del encabezado y sus requisitos sin incluir
valores de autenticación. La validación del contrato y los errores controlados
se ejecutan sin necesidad de GPU, modelo ni servicios externos.
