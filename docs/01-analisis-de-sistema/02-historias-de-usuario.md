# Historias de usuario

## 1. Descripción

Las historias de usuario expresan las necesidades de los actores
de GanadoConecta. Se relacionan con los 16 requisitos funcionales
acordados y servirán como base para las especificaciones en specs/.

## 2. Historias de usuario

| ID | Actor | Historia de usuario | Requisito |
|---|---|---|---|
| HU01 | Usuario | Como usuario, quiero registrarme, iniciar sesión y actualizar mis datos personales y de contacto para acceder al sistema y mantener mi información vigente. | RF01 |
| HU02 | Productor | Como productor, quiero registrar, editar y consultar mis animales para mantener organizada su información individual. | RF02 |
| HU03 | Productor | Como productor, quiero consultar el historial sanitario, reproductivo y productivo de cada animal para conocer sus antecedentes. | RF03 |
| HU04 | Productor | Como productor, quiero registrar vacunas, desparasitaciones, baños sanitarios y tratamientos para llevar el seguimiento sanitario de mis animales. | RF04 |
| HU05 | Productor | Como productor, quiero registrar montas o inseminaciones, gestaciones, partos y crías, y consultar una fecha probable de parto para organizar el seguimiento reproductivo. | RF05 |
| HU06 | Productor | Como productor, quiero registrar el peso y otros datos productivos y consultar su evolución para evaluar el desarrollo de mis animales. | RF06 |
| HU07 | Productor | Como productor, quiero consultar un calendario y recibir recordatorios para realizar oportunamente las actividades programadas. | RF07 |
| HU08 | Productor | Como productor, quiero obtener análisis de mis registros históricos mediante IA para identificar patrones y variaciones productivas relevantes. | RF08 |
| HU09 | Productor | Como productor, quiero recibir alertas sobre registros o tendencias que requieran seguimiento para revisar las situaciones detectadas. | RF09 |
| HU10 | Productor | Como productor, quiero consultar reportes sanitarios, reproductivos y productivos para apoyar la gestión de mi ganado. | RF10 |
| HU11 | Administrador | Como administrador, quiero gestionar usuarios y consultar información general y estadísticas básicas para administrar la plataforma. | RF11 |
| HU12 | Productor | Como productor, quiero crear y editar anuncios de mis animales con información y contacto autorizados para ofrecerlos sin exponer su historial completo. | RF12 |
| HU13 | Visitante o interesado | Como interesado, quiero consultar y filtrar anuncios activos para encontrar animales que coincidan con mis necesidades. | RF13 |
| HU14 | Productor | Como productor, quiero publicar, pausar o retirar mis anuncios para controlar su disponibilidad en el catálogo. | RF14 |
| HU15 | Productor | Como productor, quiero registrar un animal como vendido para actualizar su estado, cerrar sus anuncios activos y conservar su historial. | RF15 |
| HU16 | Usuario autenticado | Como usuario autenticado, quiero reportar anuncios inadecuados para solicitar su revisión. | RF16 |
| HU17 | Administrador | Como administrador, quiero revisar los reportes y retirar anuncios inadecuados cuando corresponda para moderar el catálogo. | RF16 |

## 3. Criterios de aceptación iniciales

Estos criterios se ampliarán en las especificaciones de cada
funcionalidad antes de implementar el código.

| Historia | Criterios de aceptación |
|---|---|
| HU01 | El registro valida los datos obligatorios y evita cuentas duplicadas según el identificador definido. El inicio de sesión rechaza credenciales incorrectas. Cada usuario puede actualizar únicamente su propio perfil. |
| HU02 | El productor puede registrar, editar y consultar animales bajo su responsabilidad. Se rechazan las operaciones sobre animales de otros productores. |
| HU03 | El historial muestra los registros asociados al animal seleccionado y respeta los permisos de acceso. |
| HU04 | Cada control se asocia a un animal existente e incluye el tipo de actividad, la fecha y los demás datos exigidos. Permite registrar observaciones y próximas actividades cuando corresponda. |
| HU05 | Los eventos reproductivos se asocian a los animales correspondientes. La fecha probable de parto se calcula cuando existen datos suficientes y se presenta como una estimación. |
| HU06 | Se rechazan pesos menores o iguales a cero. Los registros incluyen fecha y permiten consultar su evolución histórica. Los otros indicadores productivos se definirán en la especificación. |
| HU07 | El calendario muestra las actividades del productor. Los recordatorios corresponden a actividades programadas. Un fallo del proveedor externo no impide registrar o consultar información. |
| HU08 | El análisis utiliza registros autorizados y presenta sus resultados o el estado de la solicitud. Cuando los datos son insuficientes o el servicio falla, se informa la situación sin inventar resultados. |
| HU09 | Las alertas identifican el registro o tendencia que las origina. Los criterios que las generan se documentan antes de implementar y no se presentan como diagnósticos veterinarios. |
| HU10 | Los reportes corresponden a los registros autorizados del productor y reflejan la información disponible. |
| HU11 | Solo el administrador puede ejecutar funciones administrativas. El alcance de la gestión de usuarios y de las estadísticas se detalla en su especificación. |
| HU12 | Solo el productor responsable del animal puede crear o editar su anuncio. El anuncio valida sus datos obligatorios y muestra únicamente información pública autorizada. |
| HU13 | El catálogo muestra anuncios publicados y habilitados por moderación. Los filtros disponibles son categoría, raza, sexo, ubicación y rango de precio. |
| HU14 | El propietario puede cambiar el estado del anuncio mediante las transiciones permitidas. Los anuncios pausados o retirados dejan de aparecer en el catálogo público. |
| HU15 | El estado del animal y el cierre de sus anuncios activos se actualizan dentro de una misma transacción. Se conserva el historial y no se procesa ningún pago. |
| HU16 | El usuario autenticado puede reportar un anuncio indicando el motivo. El reporte queda disponible para revisión administrativa. |
| HU17 | El administrador puede revisar el reporte y retirar la publicación cuando corresponda. Un anuncio retirado deja de mostrarse en el catálogo público. |

## 4. Decisiones pendientes de especificación

- Datos obligatorios e identificador de las cuentas.
- Datos obligatorios de animales y anuncios.
- Indicadores productivos adicionales al peso.
- Método y datos necesarios para estimar la fecha probable de parto.
- Condiciones que generan alertas y análisis de IA.
- Canales y anticipación de los recordatorios.
- Transiciones de estados y procedimiento de moderación.
- Operaciones administrativas y estadísticas disponibles.

La autenticación para reportar anuncios es una regla propuesta
que deberá confirmarse en la especificación correspondiente.

## 5. Relación con las pruebas

Las especificaciones deberán vincular sus criterios de aceptación con:

- Pruebas unitarias de reglas y casos de uso.
- Pruebas de integración de API, persistencia, permisos y servicios.
- Pruebas de carga de consultas de animales y anuncios.

Las pruebas de carga se realizarán cuando los flujos evaluados
dispongan de una implementación ejecutable.