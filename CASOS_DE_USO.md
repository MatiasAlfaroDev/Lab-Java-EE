# Casos de Uso — App de Mensajería Empresarial

> Versión 1.0 — Jakarta EE 10 / WildFly 31

---

## Actores

| Actor | Descripción |
|---|---|
| **Usuario** | Empleado autenticado con rol `USER` o `MANAGER` |
| **Admin** | Administrador del sistema, acceso total |
| **Guest** | Usuario con acceso limitado (solo lectura en canales permitidos) |
| **Sistema** | Procesos internos: expiración de mensajes, JMS, scheduler |
| **Sistema Externo** | Herramientas de CI/CD, ERP, monitoring que usan webhooks |

---

## Módulo 1 — Autenticación y Sesiones

### UC-01: Registrar usuario

| Campo | Detalle |
|---|---|
| **Actor principal** | Admin |
| **Precondiciones** | El admin está autenticado. El email no existe en el sistema. |
| **Flujo principal** | 1. Admin envía `POST /api/auth/register` con username, email, password, rol. <br> 2. El sistema valida el formato y unicidad del email. <br> 3. Se genera un hash bcrypt de la contraseña. <br> 4. Se crea el par de claves ECDH para el usuario (clave pública almacenada en `users.public_key`). <br> 5. Se persiste el usuario con estado `PENDING_MFA`. <br> 6. El sistema retorna 201 con los datos del usuario (sin datos sensibles). |
| **Flujo alternativo** | 2a. Email duplicado → 409 Conflict. <br> 2b. Password no cumple política → 422 Unprocessable Entity. |
| **Postcondiciones** | Usuario creado, pendiente de configurar MFA. |

---

### UC-02: Iniciar sesión (login)

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario, Admin, Guest |
| **Precondiciones** | El usuario existe y su estado es `ACTIVE`. |
| **Flujo principal** | 1. Actor envía `POST /api/auth/login` con email y password. <br> 2. El sistema verifica el hash de la contraseña. <br> 3. Si MFA está habilitado, se retorna un `mfa_required: true` con un challenge token. <br> 4. Actor envía el código TOTP en `POST /api/auth/mfa/verify`. <br> 5. El sistema valida el código contra `users.mfa_secret`. <br> 6. Se emite un JWT de acceso (corta vida) y un refresh token (larga vida, rotativo). <br> 7. El refresh token se persiste en `refresh_tokens` con hash. |
| **Flujo alternativo** | 2a. Contraseña incorrecta → 401. Tras N intentos → cuenta bloqueada temporalmente. <br> 5a. Código TOTP inválido o expirado → 401. |
| **Postcondiciones** | Sesión activa. JWT listo para usar en cabecera `Authorization: Bearer`. |

---

### UC-03: Configurar MFA (TOTP)

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | Usuario autenticado con estado `PENDING_MFA`. |
| **Flujo principal** | 1. Actor solicita `GET /api/auth/mfa/setup`. <br> 2. El sistema genera un `mfa_secret` y retorna un QR code compatible con Google Authenticator. <br> 3. Actor escanea el QR y envía el primer código TOTP de verificación. <br> 4. El sistema valida el código y almacena el `mfa_secret` cifrado en `users.mfa_secret`. <br> 5. El estado del usuario cambia a `ACTIVE`. |
| **Flujo alternativo** | 3a. Código inválido → el secreto no se guarda, se permite reintentar. |
| **Postcondiciones** | MFA activado. El usuario puede hacer login con segundo factor. |

---

### UC-04: Renovar token de acceso

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario (cliente) |
| **Precondiciones** | El refresh token es válido y no está revocado. |
| **Flujo principal** | 1. Cliente envía `POST /api/auth/refresh` con el refresh token. <br> 2. El sistema valida el hash del token contra `refresh_tokens`. <br> 3. Se emite un nuevo JWT de acceso y un nuevo refresh token (rotación). <br> 4. El refresh token anterior se marca como revocado. |
| **Flujo alternativo** | 2a. Token no encontrado, revocado o expirado → 401. Se invalida toda la familia de tokens (detección de reutilización). |
| **Postcondiciones** | Nuevo par JWT / refresh token activo. |

---

### UC-05: Cerrar sesión (logout)

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | Usuario autenticado con refresh token válido. |
| **Flujo principal** | 1. Actor envía `POST /api/auth/logout` con el refresh token. <br> 2. El sistema revoca el refresh token en `refresh_tokens`. <br> 3. Retorna 204 No Content. |
| **Postcondiciones** | El refresh token queda inutilizable. El JWT expira naturalmente. |

---

### UC-06: SSO corporativo (OAuth 2.0 / SAML 2.0)

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | El proveedor de identidad (IdP) corporativo está configurado en el sistema. |
| **Flujo principal** | 1. Actor elige "Login con SSO". <br> 2. El sistema redirige al IdP con un authorization request. <br> 3. El IdP autentica al usuario y retorna un assertion (SAML) o code (OAuth). <br> 4. El sistema valida el assertion/code e identifica o crea al usuario. <br> 5. Se emite JWT + refresh token locales. |
| **Flujo alternativo** | 4a. El usuario no existe y el auto-aprovisionamiento está deshabilitado → 403 con mensaje orientativo. |
| **Postcondiciones** | Sesión activa sin que el usuario haya ingresado contraseña local. |

---

## Módulo 2 — Gestión de Canales

### UC-07: Crear canal

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario, Manager, Admin |
| **Precondiciones** | Actor autenticado. |
| **Flujo principal** | 1. Actor envía `POST /api/channels` con name, type (`DM` o `GROUP`), lista inicial de miembros. <br> 2. El sistema valida que los miembros existen. <br> 3. Se crea el registro en `channels` y los registros en `channel_members` (el creador obtiene rol `OWNER`). <br> 4. Retorna 201 con el canal creado. |
| **Flujo alternativo** | 1a. `type = DM`: se valida que solo haya exactamente 2 miembros. Si ya existe un DM entre ambos, se retorna el existente. |
| **Postcondiciones** | Canal disponible. Los miembros reciben notificación de invitación. |

---

### UC-08: Agregar miembro a canal

| Campo | Detalle |
|---|---|
| **Actor principal** | Owner del canal, Admin |
| **Precondiciones** | Canal de tipo `GROUP` existe. El usuario a agregar existe y está activo. |
| **Flujo principal** | 1. Actor envía `POST /api/channels/{id}/members` con el `user_id`. <br> 2. El sistema verifica el rol del actor en el canal. <br> 3. Se crea el registro en `channel_members` con rol `MEMBER`. <br> 4. Se registra evento en `audit_logs`. <br> 5. El nuevo miembro recibe notificación. |
| **Flujo alternativo** | 2a. Actor no tiene permisos → 403. <br> 3a. El usuario ya es miembro → 409. |
| **Postcondiciones** | El nuevo miembro puede ver el historial y enviar mensajes. |

---

### UC-09: Eliminar miembro de canal

| Campo | Detalle |
|---|---|
| **Actor principal** | Owner del canal, Admin |
| **Precondiciones** | El usuario es miembro activo del canal. |
| **Flujo principal** | 1. Actor envía `DELETE /api/channels/{id}/members/{userId}`. <br> 2. El sistema verifica permisos del actor. <br> 3. Se elimina el registro de `channel_members`. <br> 4. Se registra en `audit_logs`. |
| **Flujo alternativo** | 2a. Actor intenta eliminarse a sí mismo siendo el único owner → 422 (debe transferir ownership primero). |
| **Postcondiciones** | El usuario pierde acceso al canal y su historial. |

---

### UC-10: Listar canales del usuario

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | Actor autenticado. |
| **Flujo principal** | 1. Actor envía `GET /api/channels`. <br> 2. El sistema retorna los canales donde el actor es miembro activo, con último mensaje y conteo de no leídos. |
| **Postcondiciones** | — |

---

### UC-11: Crear sala de reunión efímera

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario, Manager |
| **Precondiciones** | Actor autenticado. |
| **Flujo principal** | 1. Actor envía `POST /api/meetings` con título, lista de participantes y `expires_at`. <br> 2. El sistema crea un canal con `is_ephemeral = true` y un registro en `meeting_rooms`. <br> 3. Se notifica a los participantes. |
| **Postcondiciones** | Canal efímero activo. El Sistema lo destruye automáticamente al vencer `expires_at`. |

---

### UC-12: Destruir sala de reunión (Sistema)

| Campo | Detalle |
|---|---|
| **Actor principal** | Sistema (scheduler) |
| **Precondiciones** | `meeting_rooms.ended_at` alcanzado o `channels.expires_at` vencido. |
| **Flujo principal** | 1. El scheduler evalúa canales efímeros vencidos. <br> 2. Los mensajes del canal son eliminados o archivados según política de retención. <br> 3. El canal se marca como cerrado. <br> 4. Se registra en `audit_logs`. |
| **Postcondiciones** | Canal y mensajes efímeros eliminados. |

---

## Módulo 3 — Mensajería

### UC-13: Enviar mensaje en tiempo real

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | Actor es miembro del canal. Conexión WebSocket establecida. |
| **Flujo principal** | 1. Actor envía el texto en el cliente. <br> 2. El cliente cifra el contenido con AES-256-GCM usando la clave de sesión derivada por ECDH. <br> 3. El cliente envía `{content_enc, iv, channel_id}` vía WebSocket. <br> 4. El servidor valida el JWT del WebSocket. <br> 5. El servidor persiste el mensaje en `messages` sin descifrar el contenido. <br> 6. El servidor difunde el mensaje cifrado a los miembros conectados. <br> 7. `sent_at` se registra al persistir. |
| **Flujo alternativo** | 4a. JWT inválido o expirado → cierre del WebSocket con código 4001. |
| **Postcondiciones** | Mensaje persistido y entregado en tiempo real. |

---

### UC-14: Confirmar entrega y lectura de mensaje

| Campo | Detalle |
|---|---|
| **Actor principal** | Cliente (Sistema) |
| **Precondiciones** | Mensaje recibido por el cliente receptor. |
| **Flujo principal** | 1. Al recibir el mensaje, el cliente envía ACK vía WebSocket. <br> 2. El servidor actualiza `messages.delivered_at`. <br> 3. Cuando el usuario visualiza el mensaje, el cliente envía READ. <br> 4. El servidor actualiza `messages.read_at` y notifica al remitente del estado "leído". |
| **Postcondiciones** | Estado del mensaje actualizado (enviado → entregado → leído). |

---

### UC-15: Responder en hilo (thread)

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | El mensaje padre existe en el canal. Actor es miembro. |
| **Flujo principal** | 1. Actor indica que su mensaje es respuesta a un mensaje padre. <br> 2. El cliente incluye `parent_id` en el payload. <br> 3. El servidor persiste el mensaje con `parent_id` referenciando el mensaje padre. <br> 4. El hilo se muestra anidado en el cliente. |
| **Postcondiciones** | Mensaje anidado bajo el padre, sin contaminar el flujo principal del canal. |

---

### UC-16: Editar mensaje

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario (autor del mensaje) |
| **Precondiciones** | El mensaje existe, no está eliminado, y el actor es el autor. |
| **Flujo principal** | 1. Actor envía `PUT /api/messages/{id}` con el nuevo contenido cifrado. <br> 2. El sistema verifica que el actor es el `sender_id`. <br> 3. Se actualiza `content_enc`, `iv` y se registra `edited_at`. <br> 4. Se registra en `audit_logs` con `old_value` y `new_value`. <br> 5. El servidor notifica a los miembros conectados sobre la edición. |
| **Flujo alternativo** | 2a. Actor no es el autor → 403. |
| **Postcondiciones** | Mensaje actualizado con historial de edición trazable. |

---

### UC-17: Eliminar mensaje

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario (autor), Admin |
| **Precondiciones** | El mensaje existe y no está ya eliminado. |
| **Flujo principal** | 1. Actor envía `DELETE /api/messages/{id}`. <br> 2. El sistema aplica soft-delete: `deleted_at = NOW()`, `content_enc` se sobreescribe con valor vacío. <br> 3. Se registra en `audit_logs`. <br> 4. Los clientes reciben notificación de eliminación vía WebSocket. |
| **Postcondiciones** | Mensaje marcado como eliminado; el slot queda visible en el hilo como "Mensaje eliminado". |

---

### UC-18: Ver historial y buscar mensajes

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | Actor es miembro del canal. |
| **Flujo principal** | 1. Actor envía `GET /api/channels/{id}/messages?q=&before=&limit=`. <br> 2. El sistema retorna mensajes paginados (cursor-based). <br> 3. El cliente descifra los mensajes localmente. |
| **Flujo alternativo** | 1a. Con `q=texto`: búsqueda full-text sobre metadatos; el contenido cifrado no es buscable en el servidor (limitación E2E). |
| **Postcondiciones** | — |

---

### UC-19: Expiración automática de mensaje

| Campo | Detalle |
|---|---|
| **Actor principal** | Sistema (scheduler) |
| **Precondiciones** | `messages.expires_at` alcanzado. |
| **Flujo principal** | 1. El scheduler identifica mensajes con `expires_at <= NOW()` y `deleted_at IS NULL`. <br> 2. Aplica la misma lógica de eliminación que UC-17. <br> 3. Registra en `audit_logs` con `action = AUTO_EXPIRE`. |
| **Postcondiciones** | Mensaje eliminado automáticamente; conforme a política de retención. |

---

### UC-20: Enviar mensaje offline (sin WebSocket)

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | Actor autenticado pero sin conexión WebSocket activa. |
| **Flujo principal** | 1. Actor envía `POST /api/messages` con payload cifrado. <br> 2. El servidor persiste el mensaje. <br> 3. El servidor publica un evento en la cola JMS. <br> 4. El MDB de notificaciones procesa el evento y genera una notificación push para el receptor offline. |
| **Postcondiciones** | Mensaje persistido; receptor notificado vía push cuando vuelva a conectarse. |

---

## Módulo 4 — Archivos Adjuntos

### UC-21: Subir archivo adjunto

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | Actor es miembro del canal. El tamaño del archivo no excede el límite del rol. |
| **Flujo principal** | 1. Actor envía `POST /api/attachments` con el archivo (multipart). <br> 2. El servidor calcula el SHA-256 del archivo original. <br> 3. El servidor cifra el archivo con AES-256. <br> 4. El archivo cifrado se sube a MinIO (`storage_path`). <br> 5. Se registra el `attachment` con `hash_sha256`, `mime_type`, `size_bytes`. <br> 6. El adjunto se asocia a un mensaje en el canal. <br> 7. El servidor inicia escaneo antivirus asíncrono; `scan_result` se actualiza al completarse. |
| **Flujo alternativo** | 2a. Archivo supera el límite de tamaño configurado para el rol → 413. <br> 7a. El escaneo detecta malware → el archivo se marca como bloqueado y se notifica al admin. |
| **Postcondiciones** | Archivo cifrado disponible en MinIO; accesible solo para miembros del canal. |

---

### UC-22: Descargar archivo adjunto

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | Actor es miembro del canal al que pertenece el adjunto. `scan_result = CLEAN`. |
| **Flujo principal** | 1. Actor solicita `GET /api/attachments/{id}`. <br> 2. El servidor verifica membresía. <br> 3. El servidor descarga el archivo cifrado desde MinIO. <br> 4. Descifra y retorna el archivo al cliente. <br> 5. El cliente puede verificar la integridad comparando el SHA-256. |
| **Flujo alternativo** | 2a. Actor no es miembro → 403. <br> 4a. `scan_result = INFECTED` → 451 con mensaje de bloqueo. |
| **Postcondiciones** | — |

---

### UC-23: Previsualizar archivo en línea

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | El `mime_type` es compatible con previsualización (imagen, PDF). |
| **Flujo principal** | 1. El cliente solicita el adjunto. <br> 2. El servidor retorna el contenido descifrado con el Content-Type apropiado. <br> 3. El navegador renderiza la previsualización inline. |
| **Postcondiciones** | — |

---

## Módulo 5 — Interacciones

### UC-24: Reaccionar a un mensaje (emoji)

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | Actor es miembro del canal. El mensaje existe y no está eliminado. |
| **Flujo principal** | 1. Actor envía `POST /api/messages/{id}/reactions` con `emoji`. <br> 2. Se crea un registro en `message_reactions`. <br> 3. Los clientes conectados reciben la actualización vía WebSocket. |
| **Flujo alternativo** | 2a. El actor ya reaccionó con ese emoji → se elimina la reacción (toggle). |
| **Postcondiciones** | Conteo de reacciones actualizado en tiempo real. |

---

### UC-25: Crear encuesta en canal

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario, Manager |
| **Precondiciones** | Actor es miembro del canal. |
| **Flujo principal** | 1. Actor envía `POST /api/channels/{id}/polls` con `question`, `options[]`, `is_anonymous`, `expires_at`. <br> 2. Se crea un registro en `polls` y los registros en `poll_options`. <br> 3. Los miembros reciben notificación de nueva encuesta. |
| **Postcondiciones** | Encuesta activa en el canal. |

---

### UC-26: Votar en encuesta

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | La encuesta existe, no está expirada, y el actor no ha votado previamente. |
| **Flujo principal** | 1. Actor envía `POST /api/polls/{pollId}/options/{optionId}/votes`. <br> 2. Se crea un registro en `poll_votes`. Si `is_anonymous = true`, `user_id` se almacena como null. <br> 3. Los resultados parciales se actualizan vía WebSocket para los miembros. |
| **Flujo alternativo** | 2a. Actor ya votó → 409 Conflict. <br> 2b. Encuesta expirada → 422. |
| **Postcondiciones** | Voto registrado; resultados actualizados. |

---

### UC-27: Ver resultados de encuesta

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | Actor es miembro del canal. |
| **Flujo principal** | 1. Actor solicita `GET /api/polls/{id}/results`. <br> 2. El servidor retorna conteo de votos por opción. Si `is_anonymous = true`, no se retornan los `user_id`. |
| **Postcondiciones** | — |

---

## Módulo 6 — Notificaciones

### UC-28: Recibir notificación en-app

| Campo | Detalle |
|---|---|
| **Actor principal** | Sistema → Usuario |
| **Precondiciones** | Ocurre un evento relevante (nuevo mensaje en canal, mención, encuesta, etc.). |
| **Flujo principal** | 1. El MDB procesa el evento desde la cola JMS. <br> 2. Crea un registro en `notifications` con `type`, `title`, `body`, `related_entity_*`. <br> 3. Si el usuario está conectado, la notificación se entrega vía WebSocket. <br> 4. Si el usuario está offline, se envía push notification (web/mobile). |
| **Postcondiciones** | Notificación accesible en la bandeja del usuario. |

---

### UC-29: Marcar notificaciones como leídas

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | Existen notificaciones no leídas. |
| **Flujo principal** | 1. Actor envía `PATCH /api/notifications/read` (todas) o `PATCH /api/notifications/{id}/read`. <br> 2. El sistema actualiza `notifications.read_at`. |
| **Postcondiciones** | Conteo de no leídas actualizado. |

---

### UC-30: Resumen diario por email

| Campo | Detalle |
|---|---|
| **Actor principal** | Sistema (scheduler) |
| **Precondiciones** | El usuario tiene habilitado el resumen por email en su configuración. |
| **Flujo principal** | 1. El scheduler se ejecuta diariamente. <br> 2. Agrega notificaciones no leídas por usuario del día anterior. <br> 3. Envía el email resumen. |
| **Postcondiciones** | Usuario informado sin necesidad de haber ingresado a la app. |

---

## Módulo 7 — Administración y Auditoría

### UC-31: Ver log de auditoría

| Campo | Detalle |
|---|---|
| **Actor principal** | Admin |
| **Precondiciones** | Actor autenticado con rol `ADMIN`. |
| **Flujo principal** | 1. Admin solicita `GET /api/admin/audit-logs` con filtros opcionales (usuario, acción, rango de fechas). <br> 2. El sistema retorna registros de `audit_logs` paginados con `old_value` / `new_value`. |
| **Postcondiciones** | — |

---

### UC-32: Exportar log de auditoría

| Campo | Detalle |
|---|---|
| **Actor principal** | Admin |
| **Precondiciones** | Actor autenticado con rol `ADMIN`. |
| **Flujo principal** | 1. Admin solicita `GET /api/admin/audit-logs/export?format=csv`. <br> 2. El sistema genera el archivo CSV o JSON con el filtro aplicado. <br> 3. Retorna el archivo como descarga. |
| **Postcondiciones** | Archivo exportado para análisis o compliance. |

---

### UC-33: Bloquear / suspender usuario

| Campo | Detalle |
|---|---|
| **Actor principal** | Admin |
| **Precondiciones** | El usuario objetivo existe. |
| **Flujo principal** | 1. Admin envía `PATCH /api/admin/users/{id}/status` con `status = SUSPENDED`. <br> 2. El sistema actualiza `users.status`. <br> 3. Los refresh tokens activos del usuario son revocados inmediatamente. <br> 4. Se registra en `audit_logs`. <br> 5. Si el usuario está conectado, su WebSocket es cerrado con código 4003. |
| **Postcondiciones** | Usuario sin acceso al sistema hasta que el admin lo reactive. |

---

### UC-34: Configurar política de retención de mensajes

| Campo | Detalle |
|---|---|
| **Actor principal** | Admin |
| **Precondiciones** | Actor autenticado con rol `ADMIN`. |
| **Flujo principal** | 1. Admin configura via panel: retención por canal (X días) o por tipo (DM, GROUP). <br> 2. El sistema persiste la política. <br> 3. El scheduler aplica la política automáticamente, marcando mensajes para expiración. |
| **Postcondiciones** | Mensajes expirados según la política; conformidad con GDPR u otras normativas. |

---

### UC-35: Ver reporte de uso por departamento

| Campo | Detalle |
|---|---|
| **Actor principal** | Admin, Manager |
| **Precondiciones** | Actor autenticado con rol `ADMIN` o `MANAGER`. |
| **Flujo principal** | 1. Actor solicita `GET /api/admin/reports/usage?department=&from=&to=`. <br> 2. El sistema agrega métricas: mensajes enviados, canales activos, usuarios activos. <br> 3. Retorna el reporte en JSON o como descarga CSV. |
| **Postcondiciones** | — |

---

## Módulo 8 — Webhooks y Bots

### UC-36: Registrar webhook en canal

| Campo | Detalle |
|---|---|
| **Actor principal** | Manager, Admin |
| **Precondiciones** | Actor es owner del canal o es Admin. |
| **Flujo principal** | 1. Actor envía `POST /api/channels/{id}/webhooks` con `name`, `url`, `secret`. <br> 2. El sistema almacena un hash del secret en `webhooks.secret_hash`. <br> 3. Retorna la URL del webhook inbound (única por integración). |
| **Postcondiciones** | Sistemas externos pueden publicar mensajes en el canal vía HTTP POST a esa URL. |

---

### UC-37: Recibir evento desde sistema externo (webhook inbound)

| Campo | Detalle |
|---|---|
| **Actor principal** | Sistema Externo |
| **Precondiciones** | El webhook está registrado y activo (`is_active = true`). |
| **Flujo principal** | 1. Sistema externo hace `POST /api/webhooks/{token}` con el payload y firma HMAC-SHA256. <br> 2. El servidor valida la firma usando `secret_hash`. <br> 3. Si válida, publica el mensaje en el canal asociado como si fuera un bot. <br> 4. El mensaje se distribuye a los miembros del canal. |
| **Flujo alternativo** | 2a. Firma inválida → 401. Se registra el intento en `audit_logs`. |
| **Postcondiciones** | Notificación de sistema externo visible en el canal. |

---

### UC-38: Activar / desactivar webhook

| Campo | Detalle |
|---|---|
| **Actor principal** | Manager, Admin |
| **Precondiciones** | El webhook existe en el canal. |
| **Flujo principal** | 1. Actor envía `PATCH /api/webhooks/{id}` con `is_active = false`. <br> 2. El sistema actualiza el estado. Las solicitudes entrantes futuras reciben 404. |
| **Postcondiciones** | Webhook inactivo; fácilmente reactivable. |

---

## Resumen de casos de uso

| ID | Nombre | Actor principal | Módulo |
|---|---|---|---|
| UC-01 | Registrar usuario | Admin | Autenticación |
| UC-02 | Iniciar sesión | Usuario / Admin / Guest | Autenticación |
| UC-03 | Configurar MFA | Usuario | Autenticación |
| UC-04 | Renovar token de acceso | Sistema (cliente) | Autenticación |
| UC-05 | Cerrar sesión | Usuario | Autenticación |
| UC-06 | SSO corporativo | Usuario | Autenticación |
| UC-07 | Crear canal | Usuario / Manager / Admin | Canales |
| UC-08 | Agregar miembro a canal | Owner / Admin | Canales |
| UC-09 | Eliminar miembro de canal | Owner / Admin | Canales |
| UC-10 | Listar canales del usuario | Usuario | Canales |
| UC-11 | Crear sala de reunión efímera | Usuario / Manager | Canales |
| UC-12 | Destruir sala de reunión | Sistema | Canales |
| UC-13 | Enviar mensaje en tiempo real | Usuario | Mensajería |
| UC-14 | Confirmar entrega y lectura | Sistema (cliente) | Mensajería |
| UC-15 | Responder en hilo | Usuario | Mensajería |
| UC-16 | Editar mensaje | Usuario | Mensajería |
| UC-17 | Eliminar mensaje | Usuario / Admin | Mensajería |
| UC-18 | Ver historial y buscar | Usuario | Mensajería |
| UC-19 | Expiración automática de mensaje | Sistema | Mensajería |
| UC-20 | Enviar mensaje offline | Usuario | Mensajería |
| UC-21 | Subir archivo adjunto | Usuario | Archivos |
| UC-22 | Descargar archivo adjunto | Usuario | Archivos |
| UC-23 | Previsualizar archivo | Usuario | Archivos |
| UC-24 | Reaccionar a mensaje | Usuario | Interacciones |
| UC-25 | Crear encuesta | Usuario / Manager | Interacciones |
| UC-26 | Votar en encuesta | Usuario | Interacciones |
| UC-27 | Ver resultados de encuesta | Usuario | Interacciones |
| UC-28 | Recibir notificación en-app | Sistema → Usuario | Notificaciones |
| UC-29 | Marcar notificaciones como leídas | Usuario | Notificaciones |
| UC-30 | Resumen diario por email | Sistema | Notificaciones |
| UC-31 | Ver log de auditoría | Admin | Administración |
| UC-32 | Exportar log de auditoría | Admin | Administración |
| UC-33 | Bloquear / suspender usuario | Admin | Administración |
| UC-34 | Configurar retención de mensajes | Admin | Administración |
| UC-35 | Ver reporte de uso | Admin / Manager | Administración |
| UC-36 | Registrar webhook en canal | Manager / Admin | Webhooks |
| UC-37 | Recibir evento externo | Sistema Externo | Webhooks |
| UC-38 | Activar / desactivar webhook | Manager / Admin | Webhooks |

---

*Versión 1.0 — MatiasAlfaroDev — Lab Jakarta EE 10 UTEC 5to año*
