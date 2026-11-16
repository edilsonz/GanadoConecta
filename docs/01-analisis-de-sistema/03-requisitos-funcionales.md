# Requisitos funcionales

## 1. Descripción

Los requisitos funcionales definen las operaciones que ofrecerá
GanadoConecta en su primera versión, orientada al ganado vacuno.

Estos códigos se utilizarán en las historias de usuario, los drivers
arquitectónicos, las especificaciones y las pruebas.

## 2. Requisitos funcionales

| ID | Requisito funcional |
|---|---|
| RF01 | El sistema deberá permitir el registro, inicio de sesión y actualización de los datos personales y de contacto del usuario. |
| RF02 | El sistema deberá permitir registrar, editar y consultar ganado vacuno con identificador interno único; nombre, arete y datos adicionales opcionales. Admitirá las categorías ternera, vaquilla, vaca, ternero, torete y toro. |
| RF03 | El sistema deberá mantener y permitir consultar el historial sanitario, reproductivo y productivo de cada animal. |
| RF04 | El sistema deberá permitir registrar vacunas, desparasitaciones, baños sanitarios y tratamientos, incluyendo fechas, observaciones y próximas actividades cuando corresponda. |
| RF05 | El sistema deberá permitir registrar montas o inseminaciones, gestaciones, partos y crías, y calcular una fecha probable de parto. |
| RF06 | El sistema deberá permitir registrar el peso y otros datos productivos y consultar su evolución histórica. |
| RF07 | El sistema deberá mostrar un calendario y generar recordatorios individuales y colectivos de las próximas actividades sanitarias, reproductivas y productivas, sin depender de la IA. |
| RF08 | El sistema deberá analizar registros históricos mediante inteligencia artificial para identificar patrones y variaciones productivas relevantes. |
| RF09 | El sistema deberá generar alertas sobre registros o tendencias que requieran mayor seguimiento. |
| RF10 | El sistema deberá permitir consultar reportes sanitarios, reproductivos y productivos del ganado. |
| RF11 | El sistema deberá permitir al administrador gestionar usuarios y consultar información general y estadísticas básicas. |
| RF12 | El sistema deberá permitir al productor crear y editar anuncios de sus animales con fotografías, características, ubicación general, precio solicitado y contacto autorizado, manteniendo privado el historial completo. |
| RF13 | El sistema deberá permitir consultar anuncios activos y filtrarlos por categoría, raza, sexo, ubicación o rango de precio. |
| RF14 | El sistema deberá permitir al propietario publicar, pausar o retirar sus anuncios. |
| RF15 | El sistema deberá permitir registrar un animal como vendido, cerrar sus anuncios activos y conservar su historial. |
| RF16 | El sistema deberá permitir reportar anuncios inadecuados y al administrador revisarlos y retirarlos cuando corresponda. |
| RF17 | El sistema deberá permitir consultar datos autorizados previamente disponibles y guardar registros sin conexión mediante IndexedDB, sincronizándolos posteriormente con validación de permisos, versiones y prevención de duplicados. El alcance offline de cada flujo se detallará en su SPEC. |
| RF18 | El sistema deberá registrar nacimientos, parentescos, genealogía y antecedentes, conservando su privacidad y permitiendo publicar únicamente la genealogía autorizada en anuncios. |
| RF19 | El sistema deberá ofrecer IA conversacional sobre datos autorizados y orientación general sustentada en fuentes confiables, sin diagnosticar ni prescribir; comunicará insuficiencia de información o de fuentes. |
| RF20 | El sistema deberá permitir el registro individual flexible y masivo de ganado vacuno. El formato de importación, límites y tratamiento de errores se definirán en su SPEC. |
| RF21 | El sistema deberá permitir cuentas individuales y acceso familiar autorizado a una misma ganadería, comprobando permisos y pertenencia tanto en operaciones online como al sincronizar. |
| RF22 | El sistema deberá permitir seguimiento sanitario individual y campañas colectivas de vacunación, desparasitación y baños antiparasitarios, conservando la asociación de las atenciones con cada animal. |

## 3. Condiciones generales

- Las operaciones sobre registros privados deberán verificar
  la identidad, los permisos y la responsabilidad del productor
  sobre el animal.
- El catálogo público mostrará únicamente anuncios publicados
  y habilitados por moderación.
- La información pública del anuncio permanecerá separada
  del historial privado del animal.
- Registrar una venta actualizará el estado del animal y cerrará
  sus anuncios activos de forma consistente, conservando el historial.
- La negociación, el pago, la entrega y el transporte se realizarán
  fuera de la plataforma.
- Los análisis y la conversación con IA no diagnosticarán ni prescribirán.

## 4. Desarrollo mediante SDD

Cada funcionalidad se detallará en specs/ antes de su implementación.

Las especificaciones definirán los datos obligatorios, las reglas,
los permisos, los casos de error y los criterios de aceptación,
manteniendo referencias a los requisitos de este documento.

La autenticación para reportar anuncios se mantiene como una regla
propuesta pendiente de confirmación; no modifica todavía RF16.

## 5. Verificación

El cumplimiento se comprobará mediante pruebas unitarias
y de integración relacionadas con los criterios de aceptación.

Las pruebas de carga evaluarán el rendimiento de las consultas
de animales y anuncios según el atributo de calidad AC01.

## 6. Consolidación aprobada

RF01–RF16 se conservan, con precisiones en RF02 y RF07. RF17–RF22 incorporan
las decisiones aprobadas por el responsable del proyecto el 15 de noviembre
de 2026. Esto no acredita implementación ni evaluación docente.
La pertenencia privada se establece por ganadería y autorización individual;
no requiere que todos sus integrantes sean propietarios de cada animal.
La genealogía pública requiere autorización expresa. RF15 no transfiere
animales ni historiales entre usuarios; sigue cerrando anuncios en la misma
transacción. Los formatos, roles concretos y validaciones no aprobados se
mantienen pendientes en las SPEC.
