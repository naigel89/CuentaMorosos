# iOS Notifications Specification

## Purpose

Define la implementación iOS de los tres puertos de `notifications/NotificationPorts.kt`.

La premisa de diseño es que **las reglas ya son comunes**: `NotificationContentFactory` decide
textos, canal, huella y deep link, y `NotificationCoordinator` decide cuándo emitir. iOS aporta
únicamente el "cómo se pinta" y el "dónde se recuerda". Cualquier requisito que reimplemente una
regla ya compartida está mal planteado.

## Requirements

### R101: `NotificationPresenter` sobre UNUserNotificationCenter

`iosMain` SHALL implementar `NotificationPresenter` usando `UNUserNotificationCenter`. Los textos
SHALL tomarse de `NotificationContent`, nunca construirse en iOS.

#### Scenario: se emite una invitación

- GIVEN el usuario concedió permiso de notificaciones
- WHEN `NotificationCoordinator.dispatch(InvitationReceived(...))` se ejecuta
- THEN SHALL aparecer una notificación con título "Invitación recibida"
- AND el cuerpo SHALL ser exactamente `content.body`, idéntico al que muestra Android

#### Scenario: permiso denegado

- GIVEN el usuario denegó el permiso
- WHEN se despacha cualquier evento
- THEN `areNotificationsEnabled()` SHALL devolver `false`
- AND NO SHALL registrarse la huella, de modo que si reactiva el permiso la reciba

### R102: Las acciones se derivan del contenido compartido

`ensureChannels()` SHALL registrar una `UNNotificationCategory` por cada `NotificationType`, con
las acciones que devuelva `NotificationContentFactory.actionsForType`. Los identificadores SHALL
ser los de `NotificationContentFactory` (`ACTION_ACCEPT_INVITATION`, etc.).

Se agrupa por tipo y **no** por canal porque las acciones dependen del tipo: una invitación
recibida ofrece aceptar/rechazar y una aceptada solo ver detalles, aunque ambas caigan en el
canal `ch_invitations`. Agrupar por canal daría a una de las dos las acciones de la otra.

#### Scenario: aceptar y rechazar en una invitación

- WHEN se emite una `InvitationReceived`
- THEN la notificación SHALL ofrecer "Aceptar" y "Rechazar"
- AND los identificadores SHALL coincidir con los que usa Android

#### Scenario: el resto solo ofrece ver detalles

- WHEN se emite un cálculo completado o un recordatorio
- THEN la única acción SHALL ser "Ver detalles"

### R103: `NotificationDedupStore` sobre NSUserDefaults

`iosMain` SHALL implementar `NotificationDedupStore` persistiendo las huellas en `NSUserDefaults`.

#### Scenario: no se repite una notificación

- GIVEN se emitió `CALCULATION_COMPLETED:ev-1`
- WHEN llega otra push del mismo evento
- THEN NO SHALL emitirse una segunda notificación

#### Scenario: sobrevive al reinicio

- GIVEN una huella registrada
- WHEN se cierra y reabre la app
- THEN `hasBeenSent()` SHALL seguir devolviendo `true`

### R104: Deep link al pulsar

Al abrir una notificación, el host SHALL emitir el `DeepLinkTarget` de `content.deepLink` en el
`SharedFlow` que recibe `CuentaMorososApp`.

#### Scenario: una invitación abre su pestaña

- WHEN el usuario pulsa una notificación de invitación recibida
- THEN la app SHALL abrirse en la página 3 (invitaciones)
- AND SHALL ser la misma página que abre Android, por venir del mismo `pagerPage`

### R105: Recordatorios — paridad reducida y declarada

`ReminderScheduler` de iOS SHALL programar una notificación local diaria con
`UNCalendarNotificationTrigger`. Su contenido SHALL ser genérico: NO SHALL prometer un importe ni
un nombre de deudor, porque se congela al programarse y no al dispararse.

#### Scenario: recordatorio diario genérico

- GIVEN el usuario tiene los recordatorios activados
- WHEN pasa la hora programada
- THEN SHALL aparecer una notificación invitando a abrir la app
- AND al abrirla, el detalle por deuda SHALL calcularse con datos frescos

#### Scenario: desactivar los cancela

- WHEN el usuario desactiva los recordatorios en ajustes
- THEN `cancel()` SHALL retirar las notificaciones pendientes

#### Scenario: la diferencia con Android es explícita

- GIVEN que Android calcula el contenido en el momento de disparar y iOS no
- WHEN se documente la feature
- THEN la limitación SHALL constar; NO SHALL presentarse como paridad

## Acceptance Criteria

1. Los tres puertos tienen `actual` en `iosMain` y el framework linka
2. Ningún texto de notificación se construye en `iosMain` o en Swift; todos vienen de `NotificationContent`
3. Los tests de `NotificationContentFactoryTest` y `PushPayloadParserTest` pasan también en el target iOS
4. Las huellas de deduplicación sobreviven a un reinicio de la app
5. La limitación de los recordatorios está documentada en el README y en `CLAUDE.md`
