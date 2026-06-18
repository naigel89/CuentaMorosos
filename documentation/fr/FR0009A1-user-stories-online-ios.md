# FR0009A1: User Stories — Online y Compatibilidad iOS

## Épica 1: Autenticación y Cuenta de Usuario

### US-01 - Registro de nuevo usuario
**Como** persona que nunca ha usado la app,  
**quiero** poder crear una cuenta con mi email y contraseña,  
**para** tener mis datos guardados y accesibles desde cualquier dispositivo.

**Criterios de aceptación:**
- El usuario puede introducir email y contraseña para registrarse.
- Se valida que el email tenga formato correcto y la contraseña sea segura (mínimo 8 caracteres).
- Si el email ya existe, se muestra un mensaje de error claro.
- Al registrarse correctamente, se crea un documento en `users/{uid}` en Firestore.
- El usuario es redirigido automáticamente a la pantalla principal.

---

### US-02 - Inicio de sesión
**Como** usuario registrado,  
**quiero** poder iniciar sesión con mi email y contraseña,  
**para** acceder a mis eventos y datos.

**Criterios de aceptación:**
- El usuario puede introducir sus credenciales para iniciar sesión.
- Si las credenciales son incorrectas, se muestra un error descriptivo.
- Al iniciar sesión, la sesión persiste aunque se cierre la app.

---

### US-03 - Recuperación de contraseña
**Como** usuario que ha olvidado su contraseña,  
**quiero** recibir un email para restablecerla,  
**para** poder recuperar el acceso a mi cuenta.

**Criterios de aceptación:**
- Existe una opción "¿Olvidaste tu contraseña?" en la pantalla de login.
- Al introducir el email, se envía un correo de restablecimiento mediante Firebase Auth.
- Se muestra confirmación de que el email ha sido enviado.

---

### US-04 - Cerrar sesión
**Como** usuario autenticado,  
**quiero** poder cerrar mi sesión,  
**para** proteger mis datos si comparto el dispositivo.

**Criterios de aceptación:**
- Existe una opción de cerrar sesión en los ajustes.
- Al cerrar sesión, se borran los datos cacheados localmente.
- El usuario es redirigido a la pantalla de login.

---

## Épica 2: Gestión de Eventos Online

### US-05 - Crear un evento online
**Como** usuario autenticado,  
**quiero** crear un nuevo evento que se guarde en la nube,  
**para** poder acceder a él desde cualquier dispositivo y compartirlo.

**Criterios de aceptación:**
- Al crear un evento, se guarda en Firestore con el `ownerId` del usuario actual.
- El creador queda automáticamente añadido a `memberIds`.
- El evento aparece en tiempo real en la lista de eventos del usuario.

---

### US-06 - Ver eventos en tiempo real
**Como** miembro de un evento,  
**quiero** que los cambios de otros participantes se reflejen automáticamente,  
**para** tener siempre la información actualizada sin refrescar manualmente.

**Criterios de aceptación:**
- La lista de eventos y los gastos/deudas de un evento se actualizan en tiempo real.
- Si otro miembro añade un gasto, el cambio es visible en menos de 5 segundos.

---

## Épica 3: Colaboración e Invitaciones

### US-07 - Invitar a alguien a un evento por email
**Como** creador de un evento,  
**quiero** invitar a otras personas mediante su email,  
**para** que puedan ver y editar los gastos y deudas del evento.

**Criterios de aceptación:**
- Existe una opción "Invitar participante" dentro de la pantalla de un evento.
- Al introducir el email, se crea un documento en la colección `invitations`.
- El invitado recibe una notificación push o email con la invitación.
- La invitación expira a los 7 días si no es aceptada.

---

### US-08 - Aceptar o rechazar una invitación
**Como** usuario invitado a un evento,  
**quiero** poder aceptar o rechazar la invitación,  
**para** decidir si quiero participar en ese evento.

**Criterios de aceptación:**
- El usuario ve las invitaciones pendientes en una sección de notificaciones o ajustes.
- Al aceptar, el `uid` del usuario se añade a `memberIds` del evento.
- Al rechazar, la invitación pasa a estado `rejected` y desaparece de la lista.

---

### US-09 - Ver qué usuarios son miembros de un evento
**Como** miembro de un evento,  
**quiero** ver la lista de participantes del evento,  
**para** saber quién tiene acceso a la información.

**Criterios de aceptación:**
- En la pantalla del evento hay una sección que muestra el nombre y foto de cada miembro.
- El creador está marcado visualmente como propietario.

---

### US-10 - Expulsar a un miembro de un evento
**Como** creador de un evento,  
**quiero** poder eliminar a un participante,  
**para** revocarle el acceso si ya no debe tener visibilidad de los datos.

**Criterios de aceptación:**
- Solo el creador puede eliminar miembros.
- Al eliminar un miembro, su `uid` se borra de `memberIds`.
- El miembro eliminado deja de ver el evento en su lista inmediatamente.

---

## Épica 4: Funcionamiento Offline

### US-11 - Usar la app sin conexión a internet
**Como** usuario,  
**quiero** poder consultar y modificar los datos aunque no tenga internet,  
**para** que la app sea útil en cualquier situación.

**Criterios de aceptación:**
- Los datos del último acceso están disponibles offline.
- Los cambios realizados offline se sincronizan automáticamente al recuperar la conexión.
- Se muestra un indicador visual cuando la app está en modo offline.

---

## Épica 5: Compatibilidad iOS

### US-12 - Usar la app en un iPhone
**Como** usuario de iPhone,  
**quiero** poder descargar e instalar CuentaMorosos desde la App Store,  
**para** usar las mismas funcionalidades que en Android.

**Criterios de aceptación:**
- La app es funcional en iOS 16 o superior.
- La interfaz está adaptada a las convenciones de diseño de iOS.
- La cuenta y los datos son los mismos que en la versión Android.

---

### US-13 - Sincronización entre Android e iOS
**Como** usuario que tiene un Android y su pareja un iPhone,  
**quiero** que ambos vean los mismos datos del evento en tiempo real,  
**para** que no haya discrepancias entre plataformas.

**Criterios de aceptación:**
- Un cambio realizado desde Android es visible en iOS en menos de 5 segundos y viceversa.
- No hay pérdida de datos al sincronizar entre plataformas distintas.

---

## Changelog
| Fecha | Versión | Revisión | Tipo de cambio | Descripción |
|---|---|---|---|---|
| 2026-04-05 | A | 1 | Alta | User stories para autenticación, eventos online, colaboración, modo offline y compatibilidad iOS. |
| 2026-06-18 | A | 2 | Actualización | Código reasignado de FR0006A1 a FR0009A1 para resolver colisión con `FR0006A1-perfiles-fantasma.md`. |
