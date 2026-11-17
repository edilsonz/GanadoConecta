# BASE-01 — Plan técnico

Se concreta T03 para esta iteración. Herramientas observadas: JDK 22.0.1,
Maven 3.9.16, Node 22.19.0, npm 10.9.3. Docker/PostgreSQL no detectados.
No se instalan herramientas del sistema ni se cambia PATH/JAVA_HOME global.

Decisiones antes del código: Maven por estar instalado; Java release 21 como
base LTS, compilable con JDK 22 actual; Spring Boot 3.5.16 por compatibilidad
con JUnit 5 del plan aprobado, sin migrar en esta tarea a Spring Boot 4/JUnit 6.
React 19.3.1, TypeScript 5.9.3, Vite 8.3.8 y plugin-react 6.3.0; Vitest 5.1.0,
Testing Library React 16.3.3 y jest-dom 7.0.1, happy-dom 21.0.4.
Node 22.19.0 satisface los motores consultados. jsdom actual requiere un parche
más nuevo de Node y no se instala; happy-dom soporta el instalado. Versiones
exactas en manifests y package-lock. No son versiones para siempre: actualizar
mediante revisión de compatibilidad y pruebas.

Backend: Spring MVC, Actuator health y Spring Security; seguridad deny-all salvo
GET /api/health, sin login/JWT aún. Adaptador PostgreSQL opcional por perfil,
sin tablas ni casos de negocio. package-info documenta límites futuros de
Identidad y Ganado sin crear servicios, entidades o interfaces vacíos.
Pruebas Spring Boot Test/JUnit 5 con servidor real aleatorio, sin BD para la base.

Frontend: una pantalla y cliente de salud; Vite proxy /api a API local. No se
habilita CORS arbitrario. Configuración Vite lee root .env solo para opciones
públicas explícitas: jamás expone POSTGRES_PASSWORD ni otras variables privadas.
API remota lenta/fallida se representa como no verificable; controles de negocio
no disponibles no se hacen pasar por navegación funcional.

PostgreSQL 17.12 con Compose, puerto ligado a loopback, volumen nombrado y zona UTC.
Archivo .env.example con contraseña vacía, exige elegirla fuera del repositorio.
Perfil postgres conecta por JDBC; sin Docker o servidor local no se afirma conexión.
No se borra volumen ni se crean credenciales reales en esta tarea.

Fuentes consultadas: requisitos oficiales de Spring Boot 3.5, guía Vite y
metadatos de Maven Central/npm para versiones y motores. Referencias y resultados
concretos se registrarán en docs/05-preparacion-implementacion/06-base-ejecutable.md.
