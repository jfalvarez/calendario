# Calendario · Compartir turnos (diseño)

Estado: **borrador**. SQL en [supabase/social.sql](../supabase/social.sql), sin aplicar.

## Objetivo

Que una persona pueda ver, en solo lectura, los turnos de un compañero (o de su familia) desde su propia app. El vínculo se pide con un **alias** que la persona se da a sí misma y comparte de boca a boca. El dueño decide si acepta, rechaza, bloquea o revoca.

Fuera de alcance por ahora: intercambio de turnos (siguiente fase), notificaciones push (fase 3), y compartir notas, deudas o festivos (privados por diseño).

## Decisiones principales

| Tema | Decisión | Por qué |
|------|----------|---------|
| Cómo se encuentran | **Alias** elegido por el usuario, compartido de boca a boca | Es lo que la gente ya entiende (como un usuario de WhatsApp o Telegram) y no requiere copiar códigos. |
| Formato del alias | 3–20 caracteres: letras, números y guion bajo. Único sin distinguir mayúsculas. **Se fija una sola vez** y no se puede cambiar | Evita confusiones tipo "Juan"/"juan" y nombres raros. Es la identidad con la que te encuentran: si cambiara, los compañeros que te tenían dejarían de encontrarte. |
| Búsqueda | **Solo exacta**. No hay listados ni búsqueda por prefijo | Un alias es adivinable; si se pudiera buscar parcialmente, la app serviría de buscador de personas. |
| Límite de búsquedas | **20 búsquedas por hora** por usuario, registradas en `link_attempts` | Frena la enumeración de alias. |
| Quién autoriza | El **dueño acepta** la solicitud | Nadie aparece en tu calendario sin tu visto bueno. |
| Respuestas | La función `request_link` devuelve un **código de texto**, no lanza errores de negocio | Un error deshacería el registro del intento y el límite no funcionaría. |
| Spam | Máximo **5 solicitudes pendientes** por solicitante | Limita el ruido en la bandeja del dueño. |
| Rechazo y bloqueo | Estado `rejected`; tabla `link_blocks` para bloquear | Permite decir que no, o cortar del todo. |
| Respuesta a bloqueados y rechazados | Se responde `sent`, igual que a una solicitud normal | No revela al solicitante que ha sido bloqueado o rechazado. |
| Reintento tras rechazo o revocación del dueño | **Enfriamiento de 7 días** (`ended_by` = dueño) | Evita que vuelvan a pedirlo cada pocos minutos. Si el solicitante cancela, puede volver a pedirlo al momento. |
| Dirección | **Unidireccional**: A ve los turnos de B. Para que B vea los de A, hace falta otro vínculo | Más simple de razonar y de revocar. |
| Acceso a datos | **Funciones `SECURITY DEFINER`** (`get_shared_schedule`), no políticas SELECT nuevas en `shifts`/`shift_assignments` | Si una política dejara leer filas ajenas, el `SyncEngine` las descargaría y mezclaría en la base local. |
| Escrituras | Solo por funciones (`set_my_alias`, `request_link`, `set_link_status`, `block_requester`) | Las tablas de vínculos no tienen políticas de insert/update/delete. |
| Caché en cliente | **Solo en memoria, consulta bajo demanda por mes** | No hay sincronización de datos ajenos: ni conflictos ni tablas locales nuevas. Aviso si no hay red. |
| Datos visibles | Turnos asignados y definición del turno (nombre, abreviatura, color, horario) | Lo necesario para pintar el calendario ajeno. |

## Modelo de datos

- `profiles(owner_id, alias, created_at)`: alias público. Índice único sobre `lower(alias)`. Solo lo lee su dueño directamente; el resto del mundo lo ve a través de funciones.
- `links(id, requester_id, owner_id, status, ended_by, updated_at)`: `pending` → `accepted` / `rejected` / `revoked`. Único por par `(requester_id, owner_id)`.
  - El dueño puede aceptar o rechazar (desde `pending`) y revocar (desde `pending` o `accepted`).
  - El solicitante solo puede cancelar o dejar de ver (`revoked`).
  - Tras `rejected` o `revoked` por el dueño, espera 7 días antes de volver a pedir.
- `link_blocks(owner_id, blocked_id)`: el dueño bloquea a un solicitante. Sus futuras solicitudes se responden `sent` sin crear nada.
- `link_attempts(user_id, attempted_at)`: registro de búsquedas para el límite de 20 por hora.

## Flujos

1. **Elegir alias**: el usuario abre Ajustes → sección "Compañeros" → `set_my_alias(alias)`. Solo se puede hacer una vez: después la sección muestra el alias sin campo de edición. Errores: `invalid_alias`, `alias_taken`, `alias_locked`.
2. **Compartir**: lo dice a su compañero de boca a boca ("búscame como *maria_turnos*").
3. **Solicitar**: el compañero escribe el alias → `request_link(alias)` → `sent`, `alias_not_found`, `self`, `too_many_attempts` o `too_many_pending`.
4. **Aviso al dueño**: al abrir la app o volver a primer plano, `list_pending_requests()` alimenta un banner en la pantalla principal: *"Tienes una solicitud de compañero pendiente"*, con el alias del solicitante y acciones Aceptar / Rechazar / Bloquear. En fase 3, además, una notificación push.
5. **Responder**: `set_link_status(id, 'accepted' | 'rejected')` o `block_requester(id)`.
6. **Ver**: `list_my_links()` muestra vínculos entrantes y salientes con el alias de cada parte. Al elegir un compañero con vínculo `outgoing` accepted, `get_shared_schedule(owner_id, mes_inicio, mes_fin)` pinta su calendario en solo lectura.
7. **Revocar**: cualquiera de los dos → `set_link_status(id, 'revoked')`. Efecto inmediato en el servidor; el cliente descarta la caché al volver a consultar.

## Riesgos y mitigaciones

- **Enumeración de alias**: límite de 20 búsquedas por hora, búsqueda exacta, y respuestas que no revelan bloqueos ni rechazos. Como el límite lo cuenta la propia tabla y no se revierte, no se puede saltar con errores.
- **Acoso o spam**: máximo 5 pendientes, enfriamiento de 7 días tras rechazo o revocación del dueño, y bloqueo definitivo.
- **Alias ofensivo o que suplanta a alguien**: el alias se valida solo por formato. Falta una lista de palabras reservadas y un mecanismo de reporte. Pendiente de decidir.
- **Cambio de alias**: no permitido. `set_my_alias` lanza `alias_locked` si ya hay alias. Así nadie puede "perder" a sus compañeros cambiando de nombre ni suplantar un alias ya conocido. Si alguna vez hiciera falta corregir uno, sería una acción de soporte fuera de la app.
- **Sin notificación en el móvil** hasta la fase 3: el banner solo se ve con la app abierta. Aceptable para empezar.
- **Cuenta eliminada**: `on delete cascade` borra vínculos y bloqueos de ambos lados.

## Pruebas previstas

- Extender `supabase/rls-test.ps1` con: alias duplicado en distinto caso (`Juan`/`juan`) → `alias_taken`; búsqueda 21 veces en una hora → `too_many_attempts`; sexta solicitud pendiente → `too_many_pending`; bloqueado y rechazado reciben `sent` sin crear fila; el solicitante no puede aceptarse; sin acceso a `get_shared_schedule` salvo vínculo `accepted`; nadie puede escribir en `links`, `profiles` ni `link_blocks` directamente.
- Tests unitarios en `shared`: validación del alias (formato y normalización) y el mapeo de los códigos de `request_link` a mensajes de UI.

## Fases propuestas

1. **Alias, vínculos y vista compartida de solo lectura** (este diseño), con el banner de pendientes.
2. **Intercambio de turnos**: tabla de solicitudes; al aceptar, una función modifica `shift_assignments` de ambas partes con comprobaciones explícitas. Requiere diseño propio.
3. **Notificaciones push (FCM)**: tabla de tokens por dispositivo y envío desde una Edge Function al crear una solicitud.

## Preguntas abiertas

- ¿Mostramos el alias del solicitante en el banner o también un mensaje opcional ("Hola, soy de la planta 3")? Un mensaje abre la puerta a texto libre; por ahora no lo incluyo.
- ¿Quieres una lista de alias reservados (admin, soporte, etc.) y un botón de reportar?
- ¿Desbloqueo desde Ajustes? No está en el borrador.
