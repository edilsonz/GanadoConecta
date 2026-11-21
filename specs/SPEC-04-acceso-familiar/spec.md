# SPEC-04 — Acceso familiar

ACT01 → HU22 → RF01/RF21/RF17 → AC04/AC05, ADR-07/ADR-13. Complementa AU06–AU07 de SPEC-01.

El propietario vigente consulta integrantes e invitaciones, emite invitaciones y cambia o revoca accesos de colaboradores. Se mantiene un único titular protegido por el esquema existente. La transferencia de titularidad queda pendiente de aprobación.

El propietario indica correo y rol EDITOR o LECTOR. Comparte directamente un código generado por el sistema. El invitado inicia sesión con su cuenta individual y pega el código. La aceptación exige coincidencia con el correo de la cuenta activa. El servidor consume la invitación y concede la membresía en una transacción. Si ya existe una membresía activa, se exige gestionar su rol por separado. Una membresía revocada requiere una nueva invitación emitida después de la revocación.

Decisiones técnicas del incremento. Código aleatorio de 256 bits, entregado una vez al propietario, con vigencia de 48 horas. Emitir otro para el mismo destinatario cancela el anterior. El propietario puede cancelar un código pendiente y revocar una membresía activa mediante AU07. La expiración se calcula en servidor. Se excluyen proveedores de mensajería.

| Criterio | Resultado |
|---|---|
| AF01 | Solo el titular administra. No se puede otorgar ni quitar PROPIETARIO. |
| AF02 | Solo el destinatario autenticado acepta un código pendiente y vigente una vez. Rechazo uniforme para códigos inválidos, cancelados, vencidos o consumidos. |
| AF03 | El cambio o revocación rige en la siguiente solicitud remota y al sincronizar. Los pendientes locales se conservan. |
| AF04 | Los cambios de membresía conservan autor, fecha y estados/roles anterior y nuevo. |
