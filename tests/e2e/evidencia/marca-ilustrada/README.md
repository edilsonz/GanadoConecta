# Portada con la ilustración de GanadoConecta

Chrome headless abrió la PWA compilada en loopback. Se revisaron el encuadre
del ganado y la ausencia de desplazamiento horizontal. Las capturas del
acceso muestran el formulario inicial con una respuesta de renovación
controlada de sesión ausente (HTTP 401). La revisión fue visual y se hizo
sin autenticar cuentas ni usar PostgreSQL.

| Ancho | Portada | Acceso |
|---|---|---|
| 360 px | [Portada](portada-360.png) | [Acceso](acceso-360.png) |
| 768 px | [Portada](portada-768.png) | [Acceso](acceso-768.png) |
| 1440 px | [Portada](portada-1440.png) | [Acceso](acceso-1440.png) |

El Service Worker guardó ambas variantes WebP y mostró la portada en una
reapertura sin red. La comprobación usó un contexto de navegador separado.
[Procedencia de las imágenes](../../../../img/marca/README.md)
