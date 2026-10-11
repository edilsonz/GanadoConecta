# Síntesis de la propuesta arquitectónica

GanadoConecta, desarrollado bajo el proyecto “Sistema Inteligente
para la Gestión Integral de la Actividad Ganadera”, utilizará
una arquitectura cliente-servidor con un backend monolítico modular.

La aplicación web se desarrollará con React y se comunicará
mediante una API REST con el backend de Java y Spring Boot.
Este integrará usuarios, animales, seguimiento sanitario,
reproductivo y productivo, calendario, alertas, análisis,
reportes y anuncios de venta.

Clean Architecture organizará las responsabilidades internas
del backend. El dominio y los casos de uso permanecerán
separados de la persistencia, la caché y las integraciones.
Los adaptadores implementarán contratos del núcleo y
las dependencias del código apuntarán hacia este.

PostgreSQL conservará los datos permanentes y será la fuente
principal de información. Redis almacenará temporalmente
resultados de consultas frecuentes del catálogo público.
Las entradas tendrán caducidad y se invalidarán después
de confirmar cambios que afecten a los anuncios.

Cuando una entrada no exista, haya caducado o Redis falle,
el backend consultará PostgreSQL. Las invalidaciones fallidas
se registrarán y reintentarán. Las especificaciones definirán
cómo impedir que información desactualizada exponga datos
que hayan dejado de estar autorizados.

El análisis inteligente se ejecutará en un servicio propio
independiente con Python y FastAPI. El envío de notificaciones
utilizará un proveedor externo, y las fotografías se conservarán
en almacenamiento de archivos u objetos, con sus referencias
en PostgreSQL. El proveedor de notificaciones y la solución
concreta de almacenamiento siguen pendientes de selección.

Las solicitudes de análisis y notificaciones se procesarán
mediante tareas persistentes en segundo plano. Se controlarán
sus estados, intentos y recuperación ante fallos.

La seguridad verificará identidad, permisos y pertenencia
de los registros. Los historiales completos permanecerán
privados y los anuncios mostrarán únicamente información
autorizada. Al registrar una venta, el estado del animal
y el cierre de sus anuncios se actualizarán dentro de una
misma transacción, conservando el historial.

El desarrollo seguirá SDD: las especificaciones se definirán
antes de implementar cada funcionalidad y se mantendrán
coherentes con el código y las pruebas.

Se realizarán pruebas unitarias, de integración y de carga.
El objetivo inicial de rendimiento será que el percentil 95
de las consultas de animales y anuncios no supere los
2 segundos con 100 usuarios concurrentes, bajo condiciones
documentadas. Se evaluarán escenarios con caché vacía,
poblada y no disponible.

La primera versión estará orientada al ganado vacuno.
La negociación, los pagos y el transporte se realizarán
fuera de la plataforma. La IA analizará registros históricos
sin realizar diagnósticos ni sustituir la evaluación veterinaria.

La propuesta establece la base para implementar y verificar
el sistema. Su comportamiento y sus metas de calidad deberán
demostrarse mediante las pruebas correspondientes.