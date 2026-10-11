# Patrones de diseño de GanadoConecta

## 1. Propósito

Definir los patrones que se utilizarán para organizar las
responsabilidades del sistema y conectar sus reglas de negocio
con la infraestructura.

Los patrones descritos son propuestas de diseño.
Su implementación está pendiente.

## 2. Repository

### Propósito

Separar las operaciones de persistencia de los casos de uso.

### Aplicación

Los casos de uso utilizarán contratos de repositorio para consultar
y guardar usuarios, animales, registros ganaderos y anuncios.

Los adaptadores de persistencia implementarán estos contratos
mediante PostgreSQL.

### Responsabilidades

- El contrato expresa las operaciones que necesita la aplicación.
- El adaptador resuelve consultas y almacenamiento.
- El dominio conserva las reglas de negocio.
- El caso de uso coordina la operación y su transacción.

Los contratos no deben exponer detalles de SQL, conexiones
ni entidades propias del framework de persistencia.

### Beneficio

Permite modificar la persistencia sin incorporar sus detalles
en las reglas de negocio.

## 3. Adapter

### Propósito

Adaptar una tecnología o servicio externo a un contrato
utilizado por la aplicación.

### Aplicación

| Contrato o responsabilidad | Adaptador propuesto |
|---|---|
| Persistencia | Adaptador PostgreSQL |
| CacheCatalogo | Adaptador Redis |
| ServicioAnalisis | Cliente del servicio Python/FastAPI |
| Envío de notificaciones | Adaptador del proveedor seleccionado |
| Almacenamiento de fotografías | Adaptador de archivos u objetos |

Los proveedores de notificaciones y fotografías todavía
deben definirse.

### Beneficio

Permite mantener los casos de uso independientes de los
protocolos y proveedores externos.

## 4. Application Service

### Propósito

Organizar una operación del sistema mediante un caso de uso.

### Aplicación

Se definirán servicios de aplicación para operaciones como:

- Registrar un animal.
- Registrar un evento sanitario.
- Consultar la evolución productiva.
- Publicar o pausar un anuncio.
- Registrar la venta de un animal.
- Solicitar un análisis de tendencias.

### Responsabilidades

1. Recibir los datos de la operación.
2. Verificar la autorización correspondiente.
3. Consultar los datos necesarios.
4. Aplicar las reglas del dominio.
5. Coordinar la persistencia y las integraciones.
6. Devolver el resultado.

Las reglas de negocio no deben concentrarse en los controladores.
Los servicios de aplicación tampoco deben sustituir
las responsabilidades de las entidades del dominio.

## 5. DTO y Mapper

### Propósito

Definir los datos que atraviesan la API y convertirlos
a las representaciones internas correspondientes.

### Aplicación

Se utilizarán DTO para solicitudes y respuestas de la API REST.

Los mappers convertirán los datos entre:

- Solicitudes y entradas de los casos de uso.
- Resultados internos y respuestas de la API.
- Representaciones de persistencia y objetos del dominio,
  cuando sean diferentes.

### Reglas

- Las respuestas públicas contienen únicamente datos autorizados.
- Las contraseñas y sus hashes no se devuelven.
- Los historiales privados no se incluyen automáticamente
  en los anuncios.
- Las entidades de persistencia no se exponen directamente
  como contrato de la API.

### Beneficio

Permite controlar la información intercambiada y reducir
el acoplamiento entre API, dominio y persistencia.

## 6. Cache-Aside

### Propósito

Reducir consultas repetidas al catálogo mediante una caché
que pueda reconstruirse desde PostgreSQL.

### Flujo de consulta

1. El caso de uso recibe y valida los filtros.
2. Construye una clave que incluye filtros, página y orden.
3. Consulta Redis mediante el contrato CacheCatalogo.
4. Si existe una entrada válida, utiliza el resultado conforme
   a las reglas de visibilidad del catálogo.
5. Si la entrada no existe, expiró o Redis falla,
   consulta PostgreSQL.
6. Intenta guardar el resultado público en Redis con un TTL.
7. Devuelve la respuesta.

Un fallo al guardar la caché no debe impedir devolver
una consulta obtenida correctamente desde PostgreSQL.

### Flujo de actualización

1. El caso de uso modifica los datos en PostgreSQL.
2. Confirma la transacción.
3. Invalida las entradas de catálogo afectadas.
4. Si la invalidación falla, registra el trabajo pendiente
   y permite su reintento.

### Restricciones

- PostgreSQL conserva los datos permanentes.
- Redis no almacena historiales privados, credenciales
  ni archivos de fotografías.
- Las claves y el TTL deben definirse en el diseño detallado.
- La expiración por sí sola no garantiza la retirada inmediata
  de un anuncio.
- Antes de implementar, debe definirse cómo impedir que una
  entrada antigua exponga un anuncio cuya visibilidad fue revocada.

## 7. Transactional Outbox

### Propósito

Registrar de forma duradera trabajos posteriores a un cambio
sin perderlos cuando falle una integración.

### Aplicación propuesta

Se utilizará para trabajos que deban sobrevivir a reinicios,
como solicitudes de notificación e invalidaciones de caché.

### Flujo

1. Una misma transacción guarda el cambio de negocio y
   una entrada de trabajo pendiente en PostgreSQL.
2. Después de confirmar la transacción, un ejecutor consulta
   los trabajos disponibles.
3. El ejecutor realiza la acción correspondiente.
4. Registra el resultado o programa un reintento.

### Consideraciones

- El procesamiento puede repetirse; debe contemplar idempotencia.
- Se definirán límites de reintento y tratamiento de fallos persistentes.
- El procesamiento concurrente debe evitar ejecuciones
  incompatibles sobre un mismo trabajo.
- El envío de una notificación no se considera exitoso
  solo porque se haya creado su entrada pendiente.
- El mecanismo de captura y ejecución debe detallarse
  antes de implementarlo.

### Beneficio

Permite recuperar tareas pendientes sin incorporar llamadas
externas dentro de la transacción de negocio.

## 8. Estados y transiciones explícitas

### Propósito

Controlar qué operaciones están permitidas según el estado
de un animal o anuncio.

### Aplicación

Los cambios de publicación, pausa, retirada, moderación y venta
deben pasar por operaciones del dominio que validen
sus precondiciones.

La venta de un animal debe coordinar el cambio de su estado
y el cierre de sus anuncios dentro de una transacción,
conservando el historial.

### Consideraciones

Los estados y sus transiciones se definirán en las
especificaciones correspondientes.

Inicialmente pueden representarse mediante enumeraciones
y métodos del dominio. El patrón State con clases separadas
solo se incorporará si la complejidad del comportamiento
lo justifica.

## 9. Resumen

| Patrón o mecanismo | Aplicación |
|---|---|
| Repository | Acceso a datos mediante contratos |
| Adapter | Integración con tecnologías y proveedores |
| Application Service | Coordinación de casos de uso |
| DTO y Mapper | Intercambio y transformación de datos |
| Cache-Aside | Consultas del catálogo con Redis |
| Transactional Outbox | Trabajos duraderos posteriores a cambios |
| Estados explícitos | Validación de transiciones del dominio |

## 10. Estado y decisiones pendientes

Los patrones todavía no están implementados.

Antes de implementarlos se deben concretar:

- Contratos y responsabilidades de cada módulo.
- Límites de las transacciones.
- Claves, TTL y reglas de invalidación de la caché.
- Protección de la visibilidad ante resultados antiguos.
- Estructura y procesamiento de los trabajos pendientes.
- Estados y transiciones de animales y anuncios.
- Proveedores de notificaciones y fotografías.