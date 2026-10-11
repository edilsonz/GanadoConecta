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
| RF02 | El sistema deberá permitir registrar, editar y consultar la información individual de los animales. |
| RF03 | El sistema deberá mantener y permitir consultar el historial sanitario, reproductivo y productivo de cada animal. |
| RF04 | El sistema deberá permitir registrar vacunas, desparasitaciones, baños sanitarios y tratamientos, incluyendo fechas, observaciones y próximas actividades cuando corresponda. |
| RF05 | El sistema deberá permitir registrar montas o inseminaciones, gestaciones, partos y crías, y calcular una fecha probable de parto. |
| RF06 | El sistema deberá permitir registrar el peso y otros datos productivos y consultar su evolución histórica. |
| RF07 | El sistema deberá mostrar un calendario y generar recordatorios de las próximas actividades sanitarias, reproductivas y productivas. |
| RF08 | El sistema deberá analizar registros históricos mediante inteligencia artificial para identificar patrones y variaciones productivas relevantes. |
| RF09 | El sistema deberá generar alertas sobre registros o tendencias que requieran mayor seguimiento. |
| RF10 | El sistema deberá permitir consultar reportes sanitarios, reproductivos y productivos del ganado. |
| RF11 | El sistema deberá permitir al administrador gestionar usuarios y consultar información general y estadísticas básicas. |
| RF12 | El sistema deberá permitir al productor crear y editar anuncios de sus animales con fotografías, características, ubicación general, precio solicitado y contacto autorizado, manteniendo privado el historial completo. |
| RF13 | El sistema deberá permitir consultar anuncios activos y filtrarlos por categoría, raza, sexo, ubicación o rango de precio. |
| RF14 | El sistema deberá permitir al propietario publicar, pausar o retirar sus anuncios. |
| RF15 | El sistema deberá permitir registrar un animal como vendido, cerrar sus anuncios activos y conservar su historial. |
| RF16 | El sistema deberá permitir reportar anuncios inadecuados y al administrador revisarlos y retirarlos cuando corresponda. |

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
- Los análisis de IA no constituirán diagnósticos veterinarios.

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