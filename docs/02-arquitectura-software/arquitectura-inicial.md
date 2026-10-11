# Arquitectura inicial de GanadoConecta

## 1. Objetivo

Establecer la organización inicial del Sistema Inteligente para
la Gestión Integral de la Actividad Ganadera, denominado GanadoConecta.

La propuesta integra la gestión del ganado, los anuncios de venta
y el análisis inteligente, considerando los requisitos, atributos
de calidad y restricciones definidos.

## 2. Organización general

GanadoConecta utilizará una arquitectura cliente-servidor con
un backend monolítico modular organizado mediante Clean Architecture.

El análisis inteligente se ejecutará en un servicio propio independiente.
La solución incluirá una aplicación web, un backend principal,
una base de datos y las integraciones necesarias.

La solución incorporará Redis como caché del catálogo público.
PostgreSQL seguirá siendo la fuente principal de información
y conservará los datos permanentes.

## 3. Elementos principales

| Elemento | Tecnología | Responsabilidad |
|---|---|---|
| Aplicación web | React | Presentar formularios, historiales, calendario, reportes y catálogo de anuncios. |
| Backend principal | Java y Spring Boot | Exponer la API REST, verificar permisos, ejecutar casos de uso y coordinar las operaciones. |
| Base de datos | PostgreSQL | Conservar usuarios, animales, historiales, actividades, anuncios y tareas persistentes. |
| Caché | Redis | Almacenar temporalmente resultados de consultas frecuentes del catálogo público para reducir accesos repetidos a PostgreSQL. |
| Servicio propio de IA | Python y FastAPI | Analizar registros históricos e identificar patrones o variaciones productivas. |
| Almacenamiento de fotografías | Solución pendiente de selección | Conservar archivos de imagen; sus referencias y metadatos se almacenarán en PostgreSQL. |
| Proveedor externo de notificaciones | Proveedor pendiente de selección | Enviar recordatorios y alertas mediante los canales que se definan. |

El servicio de IA forma parte de GanadoConecta, aunque se despliegue
de manera independiente. El proveedor de notificaciones es externo.

### Diagrama general de la arquitectura

![Arquitectura general de GanadoConecta](../../img/Figura_1_Estilo_GanadoConecta.png)

*Figura 1. Arquitectura cliente-servidor de GanadoConecta,
con backend monolítico modular y servicio propio de IA independiente.*

## 4. Módulos del backend

| Módulo | Responsabilidad | Requisitos relacionados |
|---|---|---|
| Usuarios y acceso | Registro, inicio de sesión, perfil, permisos y gestión administrativa de usuarios. | RF01, RF11 |
| Animales | Registro, edición y consulta individual de animales. | RF02 |
| Seguimiento ganadero | Historial y registros sanitarios, reproductivos y productivos. | RF03–RF06 |
| Calendario y alertas | Actividades programadas, recordatorios y alertas de seguimiento. | RF07, RF09 |
| Análisis inteligente | Solicitud de análisis y gestión de resultados del servicio de IA. | RF08, RF09 |
| Reportes | Consulta de reportes sanitarios, reproductivos y productivos y estadísticas básicas autorizadas. | RF10, RF11 |
| Anuncios | Publicación, búsqueda, estados, contacto autorizado, reportes y moderación. | RF12–RF16 |

El registro de una venta coordinará el estado del animal
y el cierre de sus anuncios activos dentro de una transacción.

Los módulos compartirán una aplicación desplegable y una base de datos,
manteniendo límites explícitos en el código. El acceso a funciones
de otro módulo se realizará mediante contratos definidos.

## 5. Organización interna

Las responsabilidades del backend se distribuirán en:

- **Dominio:** entidades y reglas de negocio.
- **Aplicación:** casos de uso y contratos necesarios.
- **Adaptadores de interfaz:** controladores REST, DTO y transformaciones.
- **Infraestructura:** persistencia, clientes de servicios,
  almacenamiento y configuración tecnológica.

Las dependencias del código apuntarán hacia el núcleo.
El dominio no dependerá de Spring Boot, PostgreSQL ni FastAPI.

## 6. Comunicación y ejecución

1. El usuario realiza una operación desde la aplicación web.
2. React envía la solicitud a la API REST mediante HTTPS y JSON.
3. El backend valida el formato, la identidad y los permisos
   cuando la operación está protegida.
4. El caso de uso aplica las reglas y utiliza los contratos necesarios.
5. Los adaptadores ejecutan el acceso a datos o las integraciones.
6. El backend devuelve la respuesta a la aplicación web.

Las solicitudes de análisis y notificaciones se registrarán como
tareas persistentes y se procesarán en segundo plano.

La creación de una tarea asociada a un cambio de negocio deberá
ser consistente con ese cambio. Cuando ambos se almacenen
en PostgreSQL, se utilizará la misma transacción.

Una falla temporal de IA o notificaciones no deberá impedir
el registro de animales ni la consulta de historiales y anuncios.

### Consulta del catálogo con caché

1. La aplicación web solicita una consulta del catálogo.
2. El backend consulta Redis mediante el contrato de caché.
3. Si existe una entrada vigente, utiliza el resultado almacenado.
4. Si la entrada no existe, ha caducado o Redis no está disponible,
   consulta PostgreSQL.
5. Cuando Redis está disponible, puede almacenar el resultado
   obtenido con un tiempo de caducidad.
6. El backend devuelve únicamente información pública autorizada.

Las claves de caché distinguirán los filtros, la paginación
y el orden de los resultados.

Después de confirmar en PostgreSQL cambios de publicación,
edición, pausa, retirada, moderación o venta, se invalidarán
las entradas afectadas. Las invalidaciones fallidas se registrarán
y reintentarán.

La especificación del catálogo definirá el tiempo de caducidad,
la estrategia de invalidación y las medidas para impedir
que se exponga información que haya dejado de estar autorizada.

## 7. Seguridad e integridad

- Se verificará la pertenencia de los registros en las operaciones
  protegidas, además de comprobar el rol del usuario.
- El catálogo público utilizará respuestas específicas con
  información autorizada.
- Los historiales completos permanecerán privados.
- La venta conservará el historial del animal.
- Las tareas incorporarán estados, intentos y controles
  para su recuperación y para reducir duplicados.
- Redis almacenará únicamente resultados públicos del catálogo.
  No contendrá historiales privados, credenciales ni archivos
  de fotografías.
- Las operaciones de modificación comprobarán el estado vigente
  en PostgreSQL.
- Redis no formará parte de la transacción de venta.
  La invalidación se realizará después de confirmar
  la actualización del animal y el cierre de sus anuncios.

## 8. Desarrollo y verificación

El desarrollo se guiará mediante SDD. Las especificaciones
de cada funcionalidad se documentarán en specs/ antes
de implementar su código.

Se realizarán pruebas unitarias, de integración y de carga.
El objetivo inicial de rendimiento será un percentil 95
de hasta 2 segundos para consultas de animales y anuncios
con 100 usuarios concurrentes, bajo condiciones documentadas.

Las pruebas de integración comprobarán aciertos y ausencias
de caché, caducidad, invalidación y recuperación ante fallos.

Las pruebas de carga distinguirán caché vacía y poblada.
También se evaluará la continuidad de las consultas cuando
Redis no esté disponible, registrando la degradación observada.

## 9. Límites y evolución

La primera versión no procesará pagos, cobrará comisiones
ni gestionará transporte o chat interno.

La IA no realizará diagnósticos veterinarios.

Redis forma parte de la arquitectura de la primera versión.
Su configuración se ajustará según las mediciones de rendimiento.

La separación de nuevos servicios se evaluará según
las necesidades de crecimiento del sistema.