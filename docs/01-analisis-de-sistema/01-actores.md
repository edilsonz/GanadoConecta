# Actores del sistema

## 1. Descripción

GanadoConecta permite a los productores gestionar información de su
ganado y publicar anuncios de venta. Los visitantes pueden consultar
el catálogo público, mientras que el administrador gestiona usuarios
y modera publicaciones.

## 2. Actores humanos

| ID | Actor | Descripción | Funciones principales |
|---|---|---|---|
| ACT01 | Productor | Usuario registrado que gestiona animales bajo su responsabilidad. | Registrar y actualizar animales; registrar controles sanitarios, reproductivos y productivos; consultar historiales, calendario, alertas, análisis y reportes; gestionar anuncios y registrar ventas. |
| ACT02 | Visitante o interesado | Persona que consulta información pública de animales en venta. | Consultar y filtrar anuncios; visualizar características, fotografías, precio y contacto autorizado del vendedor. |
| ACT03 | Administrador | Usuario autorizado para administrar la plataforma. | Gestionar usuarios; consultar información general y estadísticas básicas; revisar reportes de anuncios inadecuados y retirar publicaciones cuando corresponda. |

Un interesado podrá contactar al vendedor mediante el medio publicado.
La negociación, el pago y el transporte se realizarán fuera del sistema.

El reporte de publicaciones estará disponible para usuarios autenticados.
Un visitante deberá registrarse o iniciar sesión para realizarlo.

## 3. Sistema externo

| ID | Sistema externo | Responsabilidad | Interacción con GanadoConecta |
|---|---|---|---|
| EXT01 | Proveedor de notificaciones | Enviar recordatorios y alertas fuera de la plataforma. | Recibir solicitudes de envío y comunicar su resultado al backend mediante la integración disponible. |

El proveedor concreto y los canales de envío están pendientes de selección.

## 4. Servicio interno de apoyo

El servicio de inteligencia artificial desarrollado con Python y FastAPI
forma parte de GanadoConecta, aunque se ejecuta de manera independiente
del backend principal.

Su responsabilidad será analizar registros históricos e identificar
patrones o variaciones productivas relevantes. No se considera un actor
externo en el diagrama de contexto del sistema.

## 5. Reglas de acceso

- El productor solo podrá gestionar animales y registros bajo su
  responsabilidad.
- El visitante accederá únicamente a información pública autorizada.
- El administrador accederá a las funciones correspondientes a su rol.
  Su rol no implicará acceso irrestricto a todos los historiales privados.
- Los servicios integrados recibirán únicamente la información
  necesaria para cumplir su función.

## 6. Consideración sobre el veterinario

En el alcance inicial, el veterinario no tendrá un rol independiente
ni funciones específicas dentro de la plataforma.

El productor podrá registrar información de las atenciones veterinarias.
La incorporación de un rol veterinario requerirá definir previamente
sus permisos, funcionalidades y requisitos.