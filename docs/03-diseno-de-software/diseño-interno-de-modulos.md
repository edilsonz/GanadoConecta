# Diseño interno de módulos de GanadoConecta

## 1. Propósito

Describir las responsabilidades y la organización interna
de los módulos del backend de GanadoConecta.

El backend será un monolito modular desarrollado con Java
y Spring Boot, organizado según Clean Architecture.

Este documento presenta el diseño propuesto.
Las clases, tablas y contratos definitivos se concretarán
mediante las especificaciones SDD y el diseño detallado.

## 2. Organización interna

Cada módulo separa las siguientes responsabilidades:

| Parte | Responsabilidad |
|---|---|
| Dominio | Entidades, objetos de valor y reglas de negocio. |
| Aplicación | Casos de uso y contratos que necesitan ejecutar. |
| Adaptadores de entrada | Controladores REST y conversión de solicitudes. |
| Adaptadores de salida | Persistencia y comunicación con servicios externos. |

Las dependencias del código se orientan hacia el dominio
y la aplicación.

Los módulos colaboran mediante contratos explícitos.
Un módulo no debe modificar directamente los datos internos
de otro sin pasar por la operación que aplica sus reglas.

## 2.1. Estructura propuesta del backend

La siguiente estructura representa la organización prevista del backend de GanadoConecta, desarrollado en Java y Spring Boot bajo un monolito modular y Clean Architecture. No representa código implementado.

backend/
└── src/main/java/
    └── ganadoconecta/
        ├── modulos/
        │   ├── identidad/
        │   ├── ganado/
        │   │   ├── dominio/
        │   │   ├── aplicacion/
        │   │   ├── adaptadores/
        │   │   │   ├── entrada/
        │   │   │   └── salida/
        │   │   └── configuracion/
        │   ├── calendario/
        │   ├── analisis/
        │   ├── anuncios/
        │   ├── administracion/
        │   └── reportes/
        └── compartido/

El módulo Ganado se utiliza como ejemplo de organización interna. Los demás módulos seguirán el mismo criterio de separación según sus necesidades.
El módulo Análisis coordinará la comunicación con el servicio independiente de IA desarrollado en Python y FastAPI.
Las carpetas, clases e interfaces definitivas se establecerán durante el diseño detallado y la implementación.

## 3. Identidad y acceso

### Responsabilidad

Gestionar la identidad de los usuarios y controlar el acceso
a las funcionalidades protegidas.

### Elementos del dominio propuestos

- Usuario.
- Rol.
- Estado de la cuenta.

### Casos de uso

- Registrar productor.
- Iniciar sesión.
- Consultar y actualizar el perfil.
- Verificar permisos para operaciones protegidas.

### Colaboraciones

- Persistencia de usuarios en PostgreSQL.
- Infraestructura de seguridad para hash y JWT.
- Administración para habilitar o deshabilitar cuentas.

### Reglas principales

- El registro público asigna el rol de productor.
- El correo normalizado es único.
- La contraseña se conserva mediante hash.
- El backend comprueba rol, estado de cuenta y propiedad
  de los registros cuando corresponda.

## 4. Gestión de ganado e historiales

### Responsabilidad

Gestionar la identificación de los animales y sus registros
sanitarios, reproductivos y productivos.

### Elementos del dominio propuestos

- Animal.
- Registro sanitario.
- Registro reproductivo.
- Registro productivo.
- Estado del animal.

### Casos de uso

- Registrar y actualizar un animal.
- Consultar su historial.
- Registrar eventos sanitarios.
- Registrar eventos reproductivos.
- Registrar peso y otros datos productivos definidos.
- Consultar la evolución productiva.
- Registrar la venta de un animal.

### Colaboraciones

- Identidad y acceso para verificar propiedad.
- Calendario para programar actividades relacionadas.
- Anuncios para cerrar publicaciones cuando se registre una venta.
- Análisis para proporcionar los datos autorizados.

### Reglas principales

- Cada animal está asociado a un productor.
- Los registros privados solo se consultan con autorización.
- La venta conserva los historiales.
- El cambio de estado por venta y el cierre de sus anuncios
  deben confirmarse en una misma transacción.
- Las fechas estimadas se distinguen de los eventos confirmados.

La distribución en submódulos sanitarios, reproductivos
y productivos podrá concretarse durante la implementación.

## 5. Calendario y recordatorios

### Responsabilidad

Organizar actividades ganaderas y solicitar recordatorios.

### Elementos del dominio propuestos

- Actividad programada.
- Recordatorio.
- Estado de la actividad.

### Casos de uso

- Programar una actividad.
- Consultar el calendario.
- Reprogramar o cancelar una actividad.
- Registrar su realización.
- Generar solicitudes de recordatorio.

### Colaboraciones

- Gestión de ganado para relacionar actividades con animales.
- Identidad y acceso para verificar propiedad.
- Notificaciones para entregar los recordatorios.

### Reglas principales

- Las actividades pertenecen al productor correspondiente.
- Reprogramar o cancelar debe actualizar los recordatorios afectados.
- El procesamiento repetido no debe crear recordatorios
  duplicados para el mismo evento.
- Las reglas de anticipación y zona horaria deben definirse
  en la especificación.

## 6. Análisis de tendencias

### Responsabilidad

Coordinar solicitudes de análisis y presentar resultados
del servicio propio de IA.

### Elementos propuestos

- Solicitud de análisis.
- Resultado de análisis.
- Referencia a los registros utilizados.

### Casos de uso

- Preparar los datos autorizados para análisis.
- Solicitar el análisis.
- Consultar el resultado.
- Presentar tendencias y variaciones detectadas.

### Colaboraciones

- Gestión de ganado para obtener registros.
- Adaptador del servicio Python/FastAPI.
- Notificaciones cuando corresponda comunicar una alerta.

### Reglas principales

- El productor solo puede analizar datos que tenga permiso
  para consultar.
- Se envían únicamente los datos necesarios.
- Los resultados deben identificar el periodo analizado.
- La IA no realiza diagnósticos veterinarios.
- Si el servicio falla o los datos son insuficientes,
  no se presenta un análisis como completado.

Los contratos, tiempos de espera y forma de ejecución
se definirán antes de implementar la integración.

## 7. Anuncios y catálogo

### Responsabilidad

Gestionar los anuncios de venta y sus consultas públicas.

### Elementos del dominio propuestos

- Anuncio.
- Estado del anuncio.
- Datos públicos del animal.
- Referencia a fotografías.
- Datos de contacto autorizados.

### Casos de uso

- Crear, editar y publicar un anuncio.
- Pausar o retirar un anuncio.
- Consultar el catálogo y el detalle público.
- Filtrar por categoría, raza, sexo, ubicación y precio.
- Cerrar los anuncios de un animal vendido.

### Colaboraciones

- Gestión de ganado para verificar animal y propietario.
- Identidad y acceso para autorizar modificaciones.
- Moderación para aplicar restricciones de visibilidad.
- Almacenamiento de fotografías.
- CacheCatalogo para consultas mediante Redis.

### Reglas principales

- Solo el propietario puede gestionar sus anuncios,
  salvo las acciones administrativas autorizadas.
- Los historiales privados no se publican automáticamente.
- Solo se muestran anuncios permitidos por su estado
  y por las reglas de moderación.
- La venta cierra los anuncios correspondientes.
- La invalidación de caché ocurre después de confirmar
  el cambio persistente.
- Un fallo de Redis permite consultar PostgreSQL.
- Debe impedirse que una entrada antigua exponga anuncios
  cuya visibilidad haya sido revocada.

El sistema facilita el contacto; no procesa pagos,
comisiones, chat interno ni transporte.

## 8. Administración y moderación

### Responsabilidad

Gestionar cuentas y revisar reportes sobre anuncios.

### Elementos del dominio propuestos

- Reporte de anuncio.
- Decisión de moderación.
- Registro de acción administrativa.

### Casos de uso

- Consultar usuarios y estadísticas autorizadas.
- Habilitar o deshabilitar cuentas.
- Registrar un reporte de anuncio.
- Revisar reportes.
- Aplicar una decisión de moderación.

### Colaboraciones

- Identidad y acceso para controlar permisos administrativos.
- Anuncios para modificar la visibilidad.
- Notificaciones cuando corresponda comunicar una decisión.

### Reglas principales

- Las operaciones administrativas requieren autorización.
- Las decisiones de moderación deben conservar motivo,
  responsable y fecha.
- Los cambios que afecten el catálogo deben invalidar
  las entradas de caché correspondientes.
- Los permisos para reportar anuncios deben concretarse
  en la especificación.

## 9. Reportes de gestión

### Responsabilidad

Construir consultas y reportes sobre la actividad ganadera
del productor.

### Elementos propuestos

- Filtros de reporte.
- Proyección de datos.
- Resultado del reporte.

### Casos de uso

- Consultar información sanitaria, reproductiva y productiva
  de un periodo.
- Generar los reportes definidos por los requisitos.

### Colaboraciones

- Gestión de ganado para obtener datos autorizados.
- Identidad y acceso para verificar permisos.

### Reglas principales

- Los resultados respetan la propiedad de los registros.
- Los filtros y periodos deben validarse.
- Los reportes no modifican los registros originales.
- Los formatos de presentación o exportación se definirán
  en las especificaciones.

## 10. Capacidades de infraestructura compartidas

### Notificaciones

Implementa el contrato de envío mediante el proveedor seleccionado.

El ejecutor procesa solicitudes pendientes y registra
resultados, reintentos y fallos.

### Fotografías

Implementa el almacenamiento mediante archivos u objetos.

La solución debe definir permisos, validaciones, límites
y tratamiento de archivos sin referencia.

### Persistencia

Implementa los contratos de repositorio mediante PostgreSQL.

### Caché

Implementa CacheCatalogo mediante Redis y aplica las reglas
de claves, TTL e invalidación.

### Seguridad

Proporciona validación de tokens y mecanismos de autenticación,
sin sustituir las reglas de autorización de los casos de uso.

### Ejecutor de trabajos

Procesa los trabajos duraderos registrados en PostgreSQL.

Estos adaptadores y mecanismos no constituyen, por sí solos,
módulos de negocio ni servicios desplegados independientemente.

## 11. Flujos que requieren coordinación

### Registro de venta

1. Verificar identidad y propiedad.
2. Validar el estado del animal.
3. Cambiar su estado y cerrar sus anuncios.
4. Registrar el trabajo de invalidación pendiente.
5. Confirmar los cambios en una misma transacción.
6. Procesar la invalidación después de la confirmación.

El historial del animal se conserva.

### Publicación o moderación de un anuncio

1. Verificar permisos y reglas de publicación.
2. Guardar el cambio y el trabajo de invalidación.
3. Confirmar la transacción.
4. Invalidar las consultas afectadas.
5. Registrar y reintentar una invalidación fallida.

### Recordatorio

1. Identificar una actividad que requiere recordatorio.
2. Registrar la solicitud pendiente evitando duplicados.
3. Procesar el envío mediante el adaptador de notificaciones.
4. Guardar el resultado o programar un reintento.

## 12. Relación con C4

El nivel 3 de C4 muestra la organización general del backend.
Este documento desarrolla sus responsabilidades internas.

Las agrupaciones de este documento pueden desglosar
componentes del diagrama. Cuando se definan los límites finales,
los nombres y responsabilidades deberán mantenerse coherentes
entre C4, especificaciones y código.

El servicio de IA permanece como un contenedor independiente,
implementado con Python y FastAPI.

## 13. Decisiones pendientes

- Modelo de datos y restricciones.
- Clases y objetos de valor definitivos.
- Contratos entre módulos y rutas de la API.
- Estados y transiciones de animales y anuncios.
- Reglas de concurrencia.
- Claves, TTL y protección de visibilidad en caché.
- Proveedores de notificaciones y fotografías.
- Contrato y ejecución del servicio de IA.
- Procesamiento y recuperación de trabajos pendientes.

## 14. Estado

Diseño interno propuesto.
La implementación y validación de los módulos están pendientes.