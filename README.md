# Documentación del Proyecto: Chat Empresarial
**Grupo 5 — Jakarta EE 10 / WildFly — UTEC 5to año**

---

## Tabla de Contenidos

1. [Descripción General](#1-descripción-general)
2. [Arquitectura del Sistema](#2-arquitectura-del-sistema)
3. [Modelo Entidad-Relación (MER)](#3-modelo-entidad-relación-mer)
4. [Diagrama UML de Clases](#4-diagrama-uml-de-clases)
5. [Casos de Uso](#5-casos-de-uso)
6. [Requisitos del Sistema](#6-requisitos-del-sistema)
7. [Endpoints de la API REST](#7-endpoints-de-la-api-rest)
8. [Diseño UX/UI](#8-diseño-uxui)
9. [Glosario](#9-glosario)

---

## 1. Descripción General

El **Chat Empresarial** es una aplicación de mensajería en tiempo real orientada a entornos corporativos. Permite la comunicación entre empleados mediante chats individuales y grupales, con soporte para mensajes de texto, archivos adjuntos, encuestas, notificaciones y auditoría de actividad.

El sistema está desarrollado sobre **Jakarta EE 10** desplegado en **WildFly 31**, con una base de datos relacional **PostgreSQL** gestionada a través de Supabase, almacenamiento de archivos en **S3/MinIO**, y cifrado de extremo a extremo (**E2E**) mediante **AES-256-GCM** y derivación de claves **ECDH**.

### Objetivos del sistema

- Proveer comunicación segura y en tiempo real entre empleados de una organización.
- Soportar chats individuales (DM), grupales y salas de reunión efímeras.
- Garantizar trazabilidad y cumplimiento normativo mediante logs de auditoría.
- Facilitar la integración con sistemas externos mediante webhooks y eventos JMS.

---

## 2. Arquitectura del Sistema

```
┌────────────────────────────────────────┐
│               Clientes                 │
│   Web SPA (React)                      │
│   App Móvil (React Native)             │
└──────────────────┬─────────────────────┘
                   │ HTTPS / WSS
┌──────────────────▼─────────────────────┐
│       API Gateway / Load Balancer      │
│              (Nginx)                   │
└──────────────────┬─────────────────────┘
                   │
┌──────────────────▼─────────────────────┐
│     Java EE Backend (Jakarta EE 10)    │
│                                        │
│  ┌──────────────┐  ┌────────────────┐  │
│  │   REST API   │  │ WebSocket Srv  │  │
│  └──────────────┘  └────────────────┘  │
│  ┌──────────────┐  ┌────────────────┐  │
│  │  JPA / JPQL  │  │   Validation   │  │
│  └──────────────┘  └────────────────┘  │
└──────────────────┬─────────────────────┘
                   │
┌──────────────────▼─────────────────────┐
│            Infraestructura             │
│  Supabase — PostgreSQL (datos)         │
│  MinIO / S3 (almacenamiento archivos)  │
└────────────────────────────────────────┘

Cifrado E2E con VPN
```

### Componentes principales

| Componente | Tecnología |
|---|---|
| Frontend Web | React (SPA) |
| Frontend Móvil | React Native |
| Backend | Jakarta EE 10 / WildFly 31 |
| API | REST + WebSocket |
| Persistencia | JPA / JPQL |
| Base de datos | PostgreSQL (Supabase) |
| Almacenamiento archivos | MinIO / S3 |
| Mensajería asíncrona | JMS / MDB |
| Autenticación | JWT + Refresh Token rotativo |
| Segundo factor | TOTP (MFA) |
| Cifrado | AES-256-GCM + ECDH |
| Proxy / Balanceador | Nginx |

---

## 3. Modelo Entidad-Relación (MER)

### Diagrama conceptual

```
 USUARIO (N) ──── PERTENECE ──── (M) CHAT
     │                                 │
   ENVÍA                           CONTIENE
     │                                 │
     └──────────────► MENSAJE ◄────────┘
```

### Modelo Relacional (MR)

```
Usuario { id, nombre, email, contraseña, rol, estado, fecha_creado }
  PK: { id }

Chat { chatId, nombre, tipo, fecha_creado }
  PK: { chatId }

UsuarioChat { usuarioId, chatId, rol, fecha_unido }
  PK: { usuarioId, chatId }
  FK: { usuarioId } → Usuario
  FK: { chatId } → Chat

Mensaje { id, contenido, fecha_creado, fecha_entregado, fecha_leido, estado }
  PK: { id }
```

### Entidades del modelo extendido

| Entidad | Atributos principales |
|---|---|
| **users** | id (PK), username, email, password_hash, public_key, role, mfa_secret, status, created_at |
| **channels** | id (PK), name, type, created_by (FK), is_ephemeral, expires_at, created_at |
| **channel_members** | channel_id (FK), user_id (FK), role, joined_at |
| **messages** | id (PK), sender_id (FK), channel_id (FK), parent_id (FK), content_enc, iv, sent_at, delivered_at, read_at, expires_at, edited_at, deleted_at |
| **attachments** | id (PK), message_id (FK), file_name, storage_path, mime_type, size_bytes, hash_sha256, scan_result |
| **message_reactions** | id (PK), message_id (FK), user_id (FK), emoji, created_at |
| **polls** | id (PK), channel_id (FK), created_by (FK), question, is_anonymous, expires_at |
| **poll_options** | id (PK), poll_id (FK), option_text |
| **poll_votes** | id (PK), poll_option_id (FK), user_id (FK, nullable), voted_at |
| **notifications** | id (PK), user_id (FK), type, title, body, related_entity_type, read_at |
| **audit_logs** | id (PK), user_id (FK), action, entity_type, entity_id, old_value, new_value, ip_address, created_at |
| **refresh_tokens** | id (PK), user_id (FK), token_hash, expires_at, revoked |
| **webhooks** | id (PK), channel_id (FK), created_by (FK), name, url, secret_hash, is_active |
| **meeting_rooms** | id (PK), channel_id (FK), created_by (FK), title, started_at, ended_at |

### Relaciones

| Relación | Cardinalidad | Descripción |
|---|---|---|
| users → channels | 1:N | Un usuario puede crear muchos canales |
| users ↔ channels | N:M (vía channel_members) | Un usuario pertenece a muchos canales |
| channels → messages | 1:N | Un canal contiene muchos mensajes |
| users → messages | 1:N | Un usuario envía muchos mensajes |
| messages → messages | Auto-referencia | Soporte de hilos (parent_id) |
| messages → attachments | 1:N | Un mensaje puede tener archivos adjuntos |
| users → refresh_tokens | 1:N | Gestión de sesiones activas |
| polls → poll_options → poll_votes | 1:N:N | Encuestas con opciones y votos |

### Decisiones de diseño del modelo

| Decisión | Motivo |
|---|---|
| `messages.parent_id` auto-referencia nullable | Soporta hilos sin necesidad de tabla extra |
| `messages.content_enc + iv` | Contenido siempre cifrado (E2E / AES-256-GCM) |
| `users.public_key` en servidor | Necesaria para derivación ECDH entre pares |
| `poll_votes.user_id` nullable | Soporte para votaciones anónimas |
| `channels.is_ephemeral + expires_at` | Salas de reunión con auto-destrucción |
| `attachments.hash_sha256` | Verificación de integridad al descargar |
| `audit_logs.old_value / new_value` JSON | Trazabilidad completa para cumplimiento normativo |

---

## 4. Diagrama UML de Clases

### Enumeraciones

```
enum TipoEstado         enum TipoChat          enum EstadoMensaje     enum ChatRol
─────────────────       ──────────────         ──────────────────     ────────────
ONLINE                  INDIVIDUAL             ENVIADO                CREADOR
OFFLINE                 GRUPO                  PENDIENTE              ADMINISTRADOR
                        PRIVADO                RECHAZADO              MIEMBRO
```

### Tipo de dato auxiliar

```
<<DataType>>
DtFecha
───────────
dia: int
mes: int
anio: int
```

### Clases principales

```
Usuario                    MiembroChat               Chat
───────────────────────    ──────────────────────    ──────────────────────
- id: int                  - chatRol: ChatRol         - chatId: int
- nombre: string           - fechaUnido: DtFecha      - nombre: string
- email: string            [1..*]────────────────►    - tipo: TipoChat
- password: string                                    - fechaCreacion: DtFecha
- estado: TipoEstado       [1]◄── integra ──[1..*]
- fechaCreacion: DtFecha
- rol: String
     [1]
      │ envia
      ▼ [*]
Mensaje
──────────────────────────────
- id: int
- emisor: Usuario
- contenido: String
- fechaEviado: DtFecha
- fechaEntregado: DtFecha
- fechaLeido: DtFecha
- eliminado: Bool
- editado: Bool
- estado: EstadoMensaje
```

---

## 5. Casos de Uso

### Actores del sistema

| Actor | Descripción |
|---|---|
| **Usuario** | Empleado autenticado con rol USER o MANAGER |
| **Administrador** | Administrador del sistema, acceso total |
| **Guest** | Usuario con acceso limitado (solo lectura en canales permitidos) |
| **Sistema** | Procesos internos: expiración de mensajes, JMS, scheduler |
| **Sistema Externo** | Herramientas de CI/CD, ERP, monitoring que usan webhooks |

---

### Módulo 1 — Autenticación y Sesiones

#### UC-01: Registrar Usuario

| Campo | Detalle |
|---|---|
| **Actor principal** | Administrador |
| **Precondiciones** | El admin está autenticado. El email no existe en el sistema. |
| **Postcondiciones** | Usuario creado, pendiente de configurar MFA. |

**Flujo principal:**
1. El admin envía `POST /api/auth/register` con username, email, password y rol.
2. El sistema valida formato y unicidad del email.
3. Se genera un hash bcrypt de la contraseña.
4. Se crea el par de claves ECDH para el usuario (clave pública almacenada en `users.public_key`).
5. Se persiste el usuario con estado `PENDING_MFA`.
6. El sistema retorna `201` con los datos del usuario (sin datos sensibles).

**Flujos alternativos:**
- `2a.` Email duplicado → `409 Conflict`.
- `2b.` Password no cumple política → `422 Unprocessable Entity`.

---

#### UC-02: Iniciar Sesión

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario / Administrador / Guest |
| **Precondiciones** | El usuario existe y su estado es `ACTIVE`. |
| **Postcondiciones** | Sesión activa. JWT listo para usar en cabecera `Authorization: Bearer`. |

**Flujo principal:**
1. Actor envía `POST /api/auth/login` con email y password.
2. El sistema verifica el hash de la contraseña.
3. Si MFA está habilitado, se retorna `mfa_required: true` con un challenge token.
4. Actor envía el código TOTP en `POST /api/auth/mfa/verify`.
5. El sistema valida el código contra `users.mfa_secret`.
6. Se emite un JWT de acceso (corta vida) y un refresh token (larga vida, rotativo).

**Flujos alternativos:**
- `2a.` Contraseña incorrecta → `401`. Tras N intentos → cuenta bloqueada temporalmente.
- `5a.` Código TOTP inválido o expirado → `401`.

---

#### UC-03: Configurar MFA (TOTP)

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | Usuario autenticado con estado `PENDING_MFA`. |
| **Postcondiciones** | MFA activado. El usuario puede hacer login con segundo factor. |

**Flujo principal:**
1. Actor solicita `GET /api/auth/mfa/setup`.
2. El sistema genera un `mfa_secret` y retorna un QR code compatible con Google Authenticator.
3. Actor escanea el QR y envía el primer código TOTP de verificación.
4. El sistema valida el código y almacena el `mfa_secret` cifrado.
5. El estado del usuario cambia a `ACTIVE`.

---

#### UC-04: Renovar Token de Acceso

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario (cliente) |
| **Precondiciones** | El refresh token es válido y no está revocado. |
| **Postcondiciones** | Nuevo par JWT / refresh token activo. |

**Flujo principal:**
1. Cliente envía `POST /api/auth/refresh` con el refresh token.
2. El sistema valida el hash del token contra `refresh_tokens`.
3. Se emite un nuevo JWT de acceso y un nuevo refresh token (rotación).
4. El refresh token anterior se marca como revocado.

---

#### UC-05: Cerrar Sesión

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | Usuario autenticado con refresh token válido. |
| **Postcondiciones** | El refresh token queda inutilizable. El JWT expira naturalmente. |

**Flujo principal:**
1. Actor envía `POST /api/auth/logout` con el refresh token.
2. El sistema revoca el refresh token en `refresh_tokens`.
3. Retorna `204 No Content`.

---

### Módulo 2 — Gestión de Canales

#### UC-06: Crear Chat / Canal

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario / Manager / Administrador |
| **Precondiciones** | Actor autenticado. |
| **Postcondiciones** | Canal disponible. Los miembros reciben notificación de invitación. |

**Flujo principal:**
1. El usuario selecciona crear chat.
2. Ingresa nombre y tipo de chat (`DM` o `GRUPO`).
3. El sistema crea el chat.
4. El sistema crea un registro de miembro con rol `CREADOR`.

**Flujo alternativo:**
- Si el tipo es `DM`: se valida que solo haya exactamente 2 miembros. Si ya existe un DM entre ambos, se retorna el existente.

---

#### UC-07: Agregar Miembro a Chat

| Campo | Detalle |
|---|---|
| **Actor principal** | Administrador / Creador del canal |
| **Precondiciones** | Canal de tipo GRUPO existe. El usuario a agregar existe y está activo. |
| **Postcondiciones** | El nuevo miembro puede ver el historial y enviar mensajes. |

**Flujo principal:**
1. Actor selecciona el chat.
2. Actor selecciona el usuario a agregar.
3. El sistema crea un registro de miembro en el chat con rol `MIEMBRO`.
4. Se registra evento en `audit_logs`.
5. El nuevo miembro recibe notificación.

**Flujos alternativos:**
- `2a.` Actor no tiene permisos → `403`.
- `3a.` El usuario ya es miembro → `409`.

---

#### UC-08: Eliminar Miembro del Chat

| Campo | Detalle |
|---|---|
| **Actor principal** | Administrador del canal |
| **Precondiciones** | El usuario que ejecuta la acción debe ser administrador del chat. |
| **Postcondiciones** | El usuario pierde acceso al canal y su historial. |

**Flujo principal:**
1. El administrador selecciona el chat.
2. El administrador selecciona el miembro a eliminar.
3. El sistema verifica que el usuario seleccionado pertenece al chat.
4. El sistema elimina la relación de membresía entre el usuario y el chat.
5. Se registra en `audit_logs`.

**Flujo alternativo:**
- El actor intenta eliminarse siendo el único owner → `422` (debe transferir ownership primero).

---

#### UC-09: Listar Canales del Usuario

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | Actor autenticado. |
| **Postcondiciones** | — |

**Flujo principal:**
1. Actor envía `GET /api/channels`.
2. El sistema retorna los canales donde el actor es miembro activo, con último mensaje y conteo de no leídos.

---

#### UC-10: Crear Sala de Reunión Efímera

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario / Manager |
| **Precondiciones** | Actor autenticado. |
| **Postcondiciones** | Canal efímero activo. El sistema lo destruye automáticamente al vencer `expires_at`. |

**Flujo principal:**
1. Actor envía `POST /api/meetings` con título, lista de participantes y `expires_at`.
2. El sistema crea un canal con `is_ephemeral = true` y un registro en `meeting_rooms`.
3. Se notifica a los participantes.

---

### Módulo 3 — Mensajería

#### UC-11: Enviar Mensaje

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | El usuario debe pertenecer al chat. Conexión WebSocket establecida. |
| **Postcondiciones** | Mensaje persistido y entregado en tiempo real. |

**Flujo principal:**
1. El usuario selecciona el chat y escribe el mensaje.
2. El cliente cifra el contenido con AES-256-GCM usando la clave de sesión derivada por ECDH.
3. El cliente envía `{content_enc, iv, channel_id}` vía WebSocket.
4. El servidor valida el JWT del WebSocket.
5. El servidor persiste el mensaje en `messages` sin descifrar el contenido.
6. El servidor difunde el mensaje cifrado a los miembros conectados.

**Flujos alternativos:**
- `3A.` Sin conexión WebSocket: el sistema registra el mensaje con estado `PENDIENTE`.
- `3B.` Error en el envío: el sistema registra el mensaje con estado `RECHAZADO` e informa al usuario.

---

#### UC-12: Confirmar Entrega y Lectura de Mensaje

| Campo | Detalle |
|---|---|
| **Actor principal** | Cliente (Sistema) |
| **Precondiciones** | Mensaje recibido por el cliente receptor. |
| **Postcondiciones** | Estado del mensaje actualizado (enviado → entregado → leído). |

**Flujo principal:**
1. Al recibir el mensaje, el cliente envía ACK vía WebSocket.
2. El servidor actualiza `messages.delivered_at`.
3. Cuando el usuario visualiza el mensaje, el cliente envía READ.
4. El servidor actualiza `messages.read_at` y notifica al remitente del estado "leído".

---

#### UC-13: Ver Mensajes / Historial

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | El usuario debe pertenecer al chat. |
| **Postcondiciones** | Mensajes visualizados y actualizados con fecha de lectura. |

**Flujo principal:**
1. El usuario accede al chat.
2. El sistema recupera los mensajes asociados (`GET /api/channels/{id}/messages?q=&before=&limit=`).
3. El sistema muestra los mensajes paginados (cursor-based).
4. El cliente descifra los mensajes localmente.
5. El sistema actualiza la fecha de lectura de los mensajes.

**Flujo alternativo:**
- `2A.` No existen mensajes en el chat: el chat se muestra vacío con mensaje informativo.

---

#### UC-14: Editar Mensaje

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario (autor del mensaje) |
| **Precondiciones** | El usuario debe ser el emisor del mensaje. El mensaje no está eliminado. |
| **Postcondiciones** | Mensaje actualizado con historial de edición trazable. |

**Flujo principal:**
1. El usuario selecciona el mensaje a editar.
2. El usuario modifica el contenido del mensaje.
3. El sistema actualiza `content_enc`, `iv` y registra `edited_at`.
4. Se registra en `audit_logs` con `old_value` y `new_value`.
5. El servidor notifica a los miembros conectados sobre la edición.

**Flujo alternativo:**
- Actor no es el autor → `403`.

---

#### UC-15: Eliminar Mensaje

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario (autor) / Administrador |
| **Precondiciones** | El mensaje existe y no está ya eliminado. |
| **Postcondiciones** | Mensaje marcado como eliminado; el slot queda visible como "Mensaje eliminado". |

**Flujo principal:**
1. Actor envía `DELETE /api/messages/{id}`.
2. El sistema aplica soft-delete: `deleted_at = NOW()`, `content_enc` se sobreescribe con valor vacío.
3. Se registra en `audit_logs`.
4. Los clientes reciben notificación de eliminación vía WebSocket.

---

#### UC-16: Responder en Hilo (Thread)

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | El mensaje padre existe en el canal. Actor es miembro. |
| **Postcondiciones** | Mensaje anidado bajo el padre, sin contaminar el flujo principal del canal. |

**Flujo principal:**
1. Actor indica que su mensaje es respuesta a un mensaje padre.
2. El cliente incluye `parent_id` en el payload.
3. El servidor persiste el mensaje con `parent_id` referenciando el mensaje padre.
4. El hilo se muestra anidado en el cliente.

---

### Módulo 4 — Archivos Adjuntos

#### UC-17: Subir Archivo Adjunto

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | Actor es miembro del canal. El tamaño del archivo no excede el límite del rol. |
| **Postcondiciones** | Archivo cifrado disponible en MinIO; accesible solo para miembros del canal. |

**Flujo principal:**
1. Actor envía `POST /api/attachments` con el archivo (multipart).
2. El servidor calcula el SHA-256 del archivo original.
3. El servidor cifra el archivo con AES-256.
4. El archivo cifrado se sube a MinIO (`storage_path`).
5. Se registra el attachment con `hash_sha256`, `mime_type`, `size_bytes`.
6. El servidor inicia escaneo antivirus asíncrono; `scan_result` se actualiza al completarse.

**Flujos alternativos:**
- Archivo supera el límite de tamaño → `413`.
- Escaneo detecta malware → archivo bloqueado y notificación al admin.

---

#### UC-18: Descargar Archivo Adjunto

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | Actor es miembro del canal al que pertenece el adjunto. `scan_result = CLEAN`. |

**Flujo principal:**
1. Actor solicita `GET /api/attachments/{id}`.
2. El servidor verifica membresía.
3. El servidor descarga el archivo cifrado desde MinIO, lo descifra y retorna al cliente.
4. El cliente puede verificar la integridad comparando el SHA-256.

---

### Módulo 5 — Interacciones

#### UC-19: Reaccionar a un Mensaje (Emoji)

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | Actor es miembro del canal. El mensaje existe y no está eliminado. |
| **Postcondiciones** | Conteo de reacciones actualizado en tiempo real. |

**Flujo principal:**
1. Actor envía `POST /api/messages/{id}/reactions` con `emoji`.
2. Se crea un registro en `message_reactions`.
3. Los clientes conectados reciben la actualización vía WebSocket.

**Flujo alternativo:** El actor ya reaccionó con ese emoji → se elimina la reacción (toggle).

---

#### UC-20: Crear Encuesta en Canal

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario / Manager |
| **Precondiciones** | Actor es miembro del canal. |
| **Postcondiciones** | Encuesta activa en el canal. |

**Flujo principal:**
1. Actor envía `POST /api/channels/{id}/polls` con `question`, `options[]`, `is_anonymous`, `expires_at`.
2. Se crea un registro en `polls` y los registros en `poll_options`.
3. Los miembros reciben notificación de nueva encuesta.

---

#### UC-21: Votar en Encuesta

| Campo | Detalle |
|---|---|
| **Actor principal** | Usuario |
| **Precondiciones** | La encuesta existe, no está expirada, y el actor no ha votado previamente. |
| **Postcondiciones** | Voto registrado; resultados actualizados. |

**Flujo principal:**
1. Actor envía `POST /api/polls/{pollId}/options/{optionId}/votes`.
2. Se crea un registro en `poll_votes`. Si `is_anonymous = true`, `user_id` se almacena como `null`.
3. Los resultados parciales se actualizan vía WebSocket.

**Flujos alternativos:**
- Actor ya votó → `409 Conflict`.
- Encuesta expirada → `422`.

---

### Módulo 6 — Notificaciones

#### UC-22: Recibir Notificación en la Aplicación

| Campo | Detalle |
|---|---|
| **Actor principal** | Sistema → Usuario |
| **Precondiciones** | Ocurre un evento relevante (nuevo mensaje, mención, encuesta, etc.). |
| **Postcondiciones** | Notificación accesible en la bandeja del usuario. |

**Flujo principal:**
1. El MDB procesa el evento desde la cola JMS.
2. Crea un registro en `notifications` con `type`, `title`, `body`, `related_entity_*`.
3. Si el usuario está conectado, la notificación se entrega vía WebSocket.
4. Si el usuario está offline, se envía push notification (web/mobile).

---

#### UC-23: Marcar Notificaciones como Leídas

**Flujo principal:**
1. Actor envía `PATCH /api/notifications/read` (todas) o `PATCH /api/notifications/{id}/read`.
2. El sistema actualiza `notifications.read_at`.

---

### Módulo 7 — Administración y Auditoría

#### UC-24: Ver Log de Auditoría

| Campo | Detalle |
|---|---|
| **Actor principal** | Administrador |
| **Precondiciones** | Actor autenticado con rol `ADMIN`. |

**Flujo principal:**
1. Admin solicita `GET /api/admin/audit-logs` con filtros opcionales (usuario, acción, rango de fechas).
2. El sistema retorna registros de `audit_logs` paginados con `old_value` / `new_value`.

---

#### UC-25: Bloquear / Suspender Usuario

| Campo | Detalle |
|---|---|
| **Actor principal** | Administrador |
| **Postcondiciones** | Usuario sin acceso al sistema hasta que el admin lo reactive. |

**Flujo principal:**
1. Admin envía `PATCH /api/admin/users/{id}/status` con `status = SUSPENDED`.
2. El sistema actualiza `users.status`.
3. Los refresh tokens activos del usuario son revocados inmediatamente.
4. Si el usuario está conectado, su WebSocket es cerrado con código `4003`.

---

#### UC-26: Configurar Política de Retención de Mensajes

| Campo | Detalle |
|---|---|
| **Actor principal** | Administrador |

**Flujo principal:**
1. Admin configura retención por canal (X días) o por tipo (DM, GRUPO).
2. El sistema persiste la política.
3. El scheduler aplica la política automáticamente, marcando mensajes para expiración.

---

### Módulo 8 — Webhooks e Integraciones

#### UC-27: Registrar Webhook en Canal

| Campo | Detalle |
|---|---|
| **Actor principal** | Manager / Administrador |
| **Postcondiciones** | Sistemas externos pueden publicar mensajes en el canal vía HTTP POST. |

**Flujo principal:**
1. Actor envía `POST /api/channels/{id}/webhooks` con `name`, `url`, `secret`.
2. El sistema almacena un hash del secret en `webhooks.secret_hash`.
3. Retorna la URL del webhook inbound (única por integración).

---

#### UC-28: Recibir Evento desde Sistema Externo

**Flujo principal:**
1. Sistema externo hace `POST /api/webhooks/{token}` con payload y firma HMAC-SHA256.
2. El servidor valida la firma usando `secret_hash`.
3. Si válida, publica el mensaje en el canal asociado como si fuera un bot.

---

### Resumen de Casos de Uso

| ID | Nombre | Actor principal | Módulo |
|---|---|---|---|
| UC-01 | Registrar usuario | Administrador | Autenticación |
| UC-02 | Iniciar sesión | Usuario / Admin / Guest | Autenticación |
| UC-03 | Configurar MFA | Usuario | Autenticación |
| UC-04 | Renovar token de acceso | Sistema (cliente) | Autenticación |
| UC-05 | Cerrar sesión | Usuario | Autenticación |
| UC-06 | Crear chat / canal | Usuario / Manager / Admin | Canales |
| UC-07 | Agregar miembro a chat | Administrador / Creador | Canales |
| UC-08 | Eliminar miembro del chat | Administrador | Canales |
| UC-09 | Listar canales del usuario | Usuario | Canales |
| UC-10 | Crear sala de reunión efímera | Usuario / Manager | Canales |
| UC-11 | Enviar mensaje | Usuario | Mensajería |
| UC-12 | Confirmar entrega y lectura | Sistema (cliente) | Mensajería |
| UC-13 | Ver mensajes / historial | Usuario | Mensajería |
| UC-14 | Editar mensaje | Usuario | Mensajería |
| UC-15 | Eliminar mensaje | Usuario / Admin | Mensajería |
| UC-16 | Responder en hilo | Usuario | Mensajería |
| UC-17 | Subir archivo adjunto | Usuario | Archivos |
| UC-18 | Descargar archivo adjunto | Usuario | Archivos |
| UC-19 | Reaccionar a un mensaje | Usuario | Interacciones |
| UC-20 | Crear encuesta en canal | Usuario / Manager | Interacciones |
| UC-21 | Votar en encuesta | Usuario | Interacciones |
| UC-22 | Recibir notificación en-app | Sistema → Usuario | Notificaciones |
| UC-23 | Marcar notificaciones como leídas | Usuario | Notificaciones |
| UC-24 | Ver log de auditoría | Administrador | Administración |
| UC-25 | Bloquear / suspender usuario | Administrador | Administración |
| UC-26 | Configurar retención de mensajes | Administrador | Administración |
| UC-27 | Registrar webhook en canal | Manager / Admin | Webhooks |
| UC-28 | Recibir evento externo | Sistema Externo | Webhooks |

---

## 6. Requisitos del Sistema

### Requisitos Funcionales

#### RF-01 — Gestión de Usuarios
- El sistema debe permitir registrar usuarios con nombre, email, contraseña y rol.
- El sistema debe validar que el email sea único.
- El sistema debe encriptar las contraseñas con bcrypt.
- El sistema debe soportar autenticación de dos factores (TOTP/MFA).
- El sistema debe gestionar estados de usuario: `ONLINE`, `OFFLINE`, `PENDING_MFA`, `ACTIVE`, `SUSPENDED`.
- El sistema debe emitir JWT de acceso y refresh tokens rotativos.

#### RF-02 — Gestión de Canales (Chats)
- El sistema debe soportar chats de tipo `INDIVIDUAL` (DM), `GRUPO` y `PRIVADO`.
- Los chats DM entre dos usuarios deben ser únicos (no duplicarse).
- El sistema debe gestionar roles dentro del chat: `CREADOR`, `ADMINISTRADOR`, `MIEMBRO`.
- El sistema debe soportar canales efímeros con fecha de expiración automática.

#### RF-03 — Mensajería
- El sistema debe permitir el envío de mensajes en tiempo real vía WebSocket.
- El sistema debe soportar mensajes offline con almacenamiento en estado `PENDIENTE`.
- Los mensajes deben registrar estados: `ENVIADO`, `PENDIENTE`, `RECHAZADO`.
- El sistema debe registrar fechas de envío, entrega y lectura de cada mensaje.
- El sistema debe soportar respuestas en hilo (threads) mediante `parent_id`.
- El sistema debe permitir editar y eliminar mensajes (soft-delete).

#### RF-04 — Archivos Adjuntos
- El sistema debe permitir adjuntar archivos a mensajes.
- Los archivos deben ser cifrados con AES-256 antes de almacenarse.
- El sistema debe calcular y verificar el hash SHA-256 de cada archivo.
- El sistema debe realizar escaneo antivirus asíncrono de los archivos subidos.

#### RF-05 — Interacciones
- El sistema debe permitir reaccionar a mensajes con emojis (con toggle).
- El sistema debe soportar la creación y votación en encuestas con soporte para votos anónimos.

#### RF-06 — Notificaciones
- El sistema debe enviar notificaciones en tiempo real a usuarios conectados vía WebSocket.
- El sistema debe enviar notificaciones push para usuarios offline.
- El sistema debe permitir marcar notificaciones como leídas.

#### RF-07 — Administración
- El sistema debe mantener logs de auditoría de todas las acciones relevantes.
- El administrador debe poder suspender o bloquear usuarios.
- El administrador debe poder configurar políticas de retención de mensajes.
- El sistema debe permitir exportar logs de auditoría en CSV o JSON.

#### RF-08 — Webhooks
- El sistema debe soportar el registro de webhooks por canal.
- Los webhooks deben validarse mediante firma HMAC-SHA256.

---

### Requisitos No Funcionales

| ID | Categoría | Descripción |
|---|---|---|
| RNF-01 | Seguridad | Todos los mensajes deben estar cifrados de extremo a extremo (E2E) con AES-256-GCM. |
| RNF-02 | Seguridad | Las contraseñas deben almacenarse con hash bcrypt. |
| RNF-03 | Seguridad | Los tokens JWT deben tener vida corta; los refresh tokens deben rotarse. |
| RNF-04 | Seguridad | El sistema debe soportar VPN para tráfico interno. |
| RNF-05 | Rendimiento | El sistema debe soportar múltiples conexiones WebSocket concurrentes. |
| RNF-06 | Rendimiento | Las consultas de historial deben estar paginadas (cursor-based). |
| RNF-07 | Disponibilidad | El sistema debe contar con un load balancer (Nginx) para alta disponibilidad. |
| RNF-08 | Escalabilidad | La arquitectura debe permitir escalar horizontalmente el backend Jakarta EE. |
| RNF-09 | Cumplimiento | Los logs de auditoría deben soportar trazabilidad completa para GDPR. |
| RNF-10 | Mantenibilidad | El código debe seguir las convenciones de Jakarta EE 10 y JPA/JPQL. |
| RNF-11 | Portabilidad | El sistema debe estar contenedorizado con Docker (WildFly + docker-compose). |
| RNF-12 | Integridad | Los archivos adjuntos deben verificarse mediante SHA-256 al descargar. |

---

## 7. Endpoints de la API REST

> **Base URL:** `https://<host>/api`
> **Autenticación:** todos los endpoints (salvo los de autenticación pública) requieren cabecera `Authorization: Bearer <JWT>`.
> **Formato:** `Content-Type: application/json` salvo indicación de `multipart/form-data`.

---

### 7.1 Autenticación y Sesiones

| Método | Endpoint | Descripción | Roles permitidos | UC |
|--------|----------|-------------|-----------------|-----|
| `POST` | `/auth/register` | Registrar nuevo usuario | ADMIN | UC-01 |
| `POST` | `/auth/login` | Iniciar sesión (email + contraseña) | Público | UC-02 |
| `POST` | `/auth/mfa/verify` | Verificar código TOTP | Público (con challenge token) | UC-02 |
| `GET` | `/auth/mfa/setup` | Obtener QR para configurar MFA | USER (PENDING_MFA) | UC-03 |
| `POST` | `/auth/refresh` | Renovar JWT con refresh token | Público (con refresh token) | UC-04 |
| `POST` | `/auth/logout` | Cerrar sesión y revocar refresh token | USER, ADMIN | UC-05 |
| `POST` | `/auth/sso` | Iniciar flujo SSO corporativo | Público | UC-06 |

#### Detalle de payloads

**`POST /auth/register`**
```json
// Request
{
  "username": "juan.perez",
  "email": "juan.perez@empresa.com",
  "password": "P@ssw0rd123!",
  "rol": "USER"
}

// Response 201
{
  "id": "uuid",
  "username": "juan.perez",
  "email": "juan.perez@empresa.com",
  "status": "PENDING_MFA",
  "created_at": "2025-04-21T10:00:00Z"
}
```

**`POST /auth/login`**
```json
// Request
{
  "email": "juan.perez@empresa.com",
  "password": "P@ssw0rd123!"
}

// Response 200 — sin MFA
{
  "access_token": "eyJ...",
  "refresh_token": "eyJ...",
  "expires_in": 900
}

// Response 200 — con MFA habilitado
{
  "mfa_required": true,
  "challenge_token": "eyJ..."
}
```

**`POST /auth/mfa/verify`**
```json
// Request
{
  "challenge_token": "eyJ...",
  "totp_code": "123456"
}

// Response 200
{
  "access_token": "eyJ...",
  "refresh_token": "eyJ...",
  "expires_in": 900
}
```

**`POST /auth/refresh`**
```json
// Request
{ "refresh_token": "eyJ..." }

// Response 200
{
  "access_token": "eyJ...",
  "refresh_token": "eyJ...",
  "expires_in": 900
}
```

---

### 7.2 Canales

| Método | Endpoint | Descripción | Roles permitidos | UC |
|--------|----------|-------------|-----------------|-----|
| `GET` | `/channels` | Listar canales del usuario autenticado | USER, MANAGER, ADMIN | UC-09 |
| `POST` | `/channels` | Crear nuevo canal (DM o grupal) | USER, MANAGER, ADMIN | UC-06 |
| `GET` | `/channels/{id}` | Obtener detalle de un canal | Miembro del canal | — |
| `DELETE` | `/channels/{id}` | Eliminar canal | ADMIN, CREADOR | — |
| `GET` | `/channels/{id}/members` | Listar miembros de un canal | Miembro del canal | — |
| `POST` | `/channels/{id}/members` | Agregar miembro al canal | OWNER, ADMIN | UC-07 |
| `DELETE` | `/channels/{id}/members/{userId}` | Eliminar miembro del canal | OWNER, ADMIN | UC-08 |
| `POST` | `/meetings` | Crear sala de reunión efímera | USER, MANAGER | UC-10 |

#### Detalle de payloads

**`POST /channels`**
```json
// Request
{
  "nombre": "Equipo Backend",
  "tipo": "GRUPO",
  "miembros": ["uuid-user-1", "uuid-user-2"]
}

// Response 201
{
  "id": "uuid",
  "nombre": "Equipo Backend",
  "tipo": "GRUPO",
  "fecha_creado": "2025-04-21T10:00:00Z",
  "miembros": [
    { "usuario_id": "uuid", "rol": "CREADOR" }
  ]
}
```

**`POST /channels/{id}/members`**
```json
// Request
{ "user_id": "uuid-nuevo-miembro" }

// Response 201
{
  "usuario_id": "uuid-nuevo-miembro",
  "canal_id": "uuid",
  "rol": "MIEMBRO",
  "fecha_unido": "2025-04-21T10:05:00Z"
}
```

**`POST /meetings`**
```json
// Request
{
  "titulo": "Reunión de planificación Sprint 3",
  "participantes": ["uuid-user-1", "uuid-user-2"],
  "expires_at": "2025-04-21T12:00:00Z"
}

// Response 201
{
  "id": "uuid",
  "canal_id": "uuid",
  "titulo": "Reunión de planificación Sprint 3",
  "expires_at": "2025-04-21T12:00:00Z"
}
```

---

### 7.3 Mensajes

| Método | Endpoint | Descripción | Roles permitidos | UC |
|--------|----------|-------------|-----------------|-----|
| `GET` | `/channels/{id}/messages` | Listar mensajes (paginado, cursor-based) | Miembro del canal | UC-13 |
| `POST` | `/messages` | Enviar mensaje offline (sin WebSocket) | Miembro del canal | UC-20 |
| `PUT` | `/messages/{id}` | Editar mensaje | Autor del mensaje | UC-14 |
| `DELETE` | `/messages/{id}` | Eliminar mensaje (soft-delete) | Autor, ADMIN | UC-15 |
| `PATCH` | `/messages/{id}/read` | Marcar mensaje como leído | Miembro del canal | UC-12 |

#### Parámetros de consulta — `GET /channels/{id}/messages`

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `q` | string | Búsqueda full-text sobre metadatos |
| `before` | string (cursor) | Cursor para paginación hacia atrás |
| `after` | string (cursor) | Cursor para paginación hacia adelante |
| `limit` | int | Cantidad de mensajes por página (default: 50, max: 100) |

```json
// Response 200
{
  "data": [
    {
      "id": "uuid",
      "sender_id": "uuid",
      "content_enc": "base64...",
      "iv": "base64...",
      "sent_at": "2025-04-21T10:00:00Z",
      "delivered_at": "2025-04-21T10:00:01Z",
      "read_at": null,
      "edited_at": null,
      "deleted_at": null,
      "parent_id": null,
      "reacciones": []
    }
  ],
  "cursor_next": "eyJ...",
  "cursor_prev": "eyJ...",
  "total": 120
}
```

**`POST /messages`** *(offline)*
```json
// Request
{
  "channel_id": "uuid",
  "content_enc": "base64...",
  "iv": "base64...",
  "parent_id": null
}

// Response 201
{
  "id": "uuid",
  "estado": "PENDIENTE",
  "sent_at": "2025-04-21T10:00:00Z"
}
```

**`PUT /messages/{id}`**
```json
// Request
{
  "content_enc": "base64_nuevo...",
  "iv": "base64_nuevo..."
}

// Response 200
{
  "id": "uuid",
  "edited_at": "2025-04-21T10:05:00Z"
}
```

---

### 7.4 WebSocket

> **URL de conexión:** `wss://<host>/ws/chat`
> El JWT se pasa como parámetro de query: `?token=<JWT>`

#### Eventos del cliente → servidor

| Evento | Payload | Descripción |
|--------|---------|-------------|
| `MESSAGE_SEND` | `{ channel_id, content_enc, iv, parent_id? }` | Enviar mensaje cifrado |
| `MESSAGE_ACK` | `{ message_id }` | Confirmar recepción del mensaje |
| `MESSAGE_READ` | `{ message_id }` | Confirmar lectura del mensaje |
| `TYPING_START` | `{ channel_id }` | Indicar que el usuario está escribiendo |
| `TYPING_STOP` | `{ channel_id }` | Indicar que el usuario dejó de escribir |

#### Eventos del servidor → cliente

| Evento | Payload | Descripción |
|--------|---------|-------------|
| `MESSAGE_NEW` | `{ id, channel_id, sender_id, content_enc, iv, sent_at, parent_id? }` | Nuevo mensaje entrante |
| `MESSAGE_DELIVERED` | `{ message_id, delivered_at }` | Confirmación de entrega |
| `MESSAGE_READ` | `{ message_id, read_at }` | Confirmación de lectura |
| `MESSAGE_EDITED` | `{ message_id, content_enc, iv, edited_at }` | Mensaje editado |
| `MESSAGE_DELETED` | `{ message_id, deleted_at }` | Mensaje eliminado |
| `REACTION_UPDATED` | `{ message_id, reacciones[] }` | Reacciones actualizadas |
| `MEMBER_ADDED` | `{ channel_id, user_id, rol }` | Nuevo miembro en canal |
| `MEMBER_REMOVED` | `{ channel_id, user_id }` | Miembro eliminado del canal |
| `USER_STATUS` | `{ user_id, estado }` | Cambio de estado online/offline |
| `NOTIFICATION_NEW` | `{ id, type, title, body }` | Nueva notificación en-app |
| `TYPING` | `{ channel_id, user_id, activo }` | Indicador de escritura |

#### Códigos de cierre WebSocket

| Código | Descripción |
|--------|-------------|
| `4001` | JWT inválido o expirado |
| `4003` | Usuario suspendido por administrador |
| `4004` | Canal no encontrado o acceso denegado |

---

### 7.5 Archivos Adjuntos

| Método | Endpoint | Descripción | Roles permitidos | UC |
|--------|----------|-------------|-----------------|-----|
| `POST` | `/attachments` | Subir archivo adjunto | Miembro del canal | UC-17 |
| `GET` | `/attachments/{id}` | Descargar archivo adjunto | Miembro del canal | UC-18 |
| `GET` | `/attachments/{id}/preview` | Previsualizar archivo inline | Miembro del canal | UC-18 |

**`POST /attachments`** — `Content-Type: multipart/form-data`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `archivo` | file | Archivo a subir |
| `channel_id` | string | ID del canal al que se adjunta |
| `message_id` | string (opcional) | ID del mensaje al que se asocia |

```json
// Response 201
{
  "id": "uuid",
  "file_name": "reporte_q1.pdf",
  "mime_type": "application/pdf",
  "size_bytes": 204800,
  "hash_sha256": "abc123...",
  "scan_result": "PENDING",
  "uploaded_at": "2025-04-21T10:00:00Z"
}
```

---

### 7.6 Interacciones

| Método | Endpoint | Descripción | Roles permitidos | UC |
|--------|----------|-------------|-----------------|-----|
| `POST` | `/messages/{id}/reactions` | Agregar o quitar reacción (toggle) | Miembro del canal | UC-19 |
| `POST` | `/channels/{id}/polls` | Crear encuesta en canal | USER, MANAGER | UC-20 |
| `POST` | `/polls/{pollId}/options/{optionId}/votes` | Votar en encuesta | Miembro del canal | UC-21 |
| `GET` | `/polls/{id}/results` | Ver resultados de encuesta | Miembro del canal | UC-21 |

**`POST /messages/{id}/reactions`**
```json
// Request
{ "emoji": "👍" }

// Response 200
{
  "message_id": "uuid",
  "reacciones": [
    { "emoji": "👍", "cantidad": 3, "reaccionado_por_mi": true }
  ]
}
```

**`POST /channels/{id}/polls`**
```json
// Request
{
  "pregunta": "¿Cuándo hacemos el siguiente retrospectivo?",
  "opciones": ["Lunes 14hs", "Miércoles 10hs", "Viernes 16hs"],
  "anonima": false,
  "expires_at": "2025-04-25T23:59:00Z"
}

// Response 201
{
  "id": "uuid",
  "pregunta": "¿Cuándo hacemos el siguiente retrospectivo?",
  "opciones": [
    { "id": "uuid-opt-1", "texto": "Lunes 14hs", "votos": 0 },
    { "id": "uuid-opt-2", "texto": "Miércoles 10hs", "votos": 0 },
    { "id": "uuid-opt-3", "texto": "Viernes 16hs", "votos": 0 }
  ],
  "expires_at": "2025-04-25T23:59:00Z"
}
```

---

### 7.7 Notificaciones

| Método | Endpoint | Descripción | Roles permitidos | UC |
|--------|----------|-------------|-----------------|-----|
| `GET` | `/notifications` | Listar notificaciones del usuario | USER, ADMIN | — |
| `PATCH` | `/notifications/read` | Marcar todas las notificaciones como leídas | USER, ADMIN | UC-23 |
| `PATCH` | `/notifications/{id}/read` | Marcar una notificación como leída | USER, ADMIN | UC-23 |

```json
// GET /notifications — Response 200
{
  "data": [
    {
      "id": "uuid",
      "type": "MENSAJE_NUEVO",
      "title": "Juan Pérez te mencionó",
      "body": "...en el canal #backend",
      "read_at": null,
      "created_at": "2025-04-21T10:00:00Z"
    }
  ],
  "no_leidas": 5
}
```

---

### 7.8 Administración

| Método | Endpoint | Descripción | Roles permitidos | UC |
|--------|----------|-------------|-----------------|-----|
| `GET` | `/admin/users` | Listar todos los usuarios | ADMIN | — |
| `PATCH` | `/admin/users/{id}/status` | Cambiar estado de usuario | ADMIN | UC-25 |
| `GET` | `/admin/audit-logs` | Ver log de auditoría (con filtros) | ADMIN | UC-24 |
| `GET` | `/admin/audit-logs/export` | Exportar log de auditoría (CSV/JSON) | ADMIN | UC-24 |
| `GET` | `/admin/reports/usage` | Reporte de uso por departamento | ADMIN, MANAGER | — |

#### Parámetros de consulta — `GET /admin/audit-logs`

| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `usuario_id` | string | Filtrar por usuario |
| `accion` | string | Filtrar por tipo de acción |
| `desde` | ISO 8601 | Fecha de inicio del rango |
| `hasta` | ISO 8601 | Fecha de fin del rango |
| `page` | int | Número de página |
| `limit` | int | Registros por página (default: 50) |

**`PATCH /admin/users/{id}/status`**
```json
// Request
{ "status": "SUSPENDED" }

// Response 200
{
  "id": "uuid",
  "username": "juan.perez",
  "status": "SUSPENDED",
  "updated_at": "2025-04-21T10:00:00Z"
}
```

**`GET /admin/audit-logs/export?format=csv`**
```
// Response 200
Content-Type: text/csv
Content-Disposition: attachment; filename="audit_logs_2025-04-21.csv"

id,usuario_id,accion,entidad_tipo,entidad_id,valor_anterior,valor_nuevo,ip,fecha
uuid,uuid,EDIT_MESSAGE,messages,uuid,"{...}","{...}",192.168.1.1,2025-04-21T10:00:00Z
```

---

### 7.9 Webhooks

| Método | Endpoint | Descripción | Roles permitidos | UC |
|--------|----------|-------------|-----------------|-----|
| `POST` | `/channels/{id}/webhooks` | Registrar webhook en canal | OWNER, ADMIN | UC-27 |
| `GET` | `/channels/{id}/webhooks` | Listar webhooks de un canal | OWNER, ADMIN | — |
| `PATCH` | `/webhooks/{id}` | Activar / desactivar webhook | OWNER, ADMIN | UC-28 |
| `DELETE` | `/webhooks/{id}` | Eliminar webhook | OWNER, ADMIN | — |
| `POST` | `/webhooks/{token}` | Recibir evento desde sistema externo | Sistema Externo | UC-28 |

**`POST /channels/{id}/webhooks`**
```json
// Request
{
  "nombre": "CI/CD Pipeline",
  "url": "https://mi-sistema.com/callback",
  "secret": "mi_secreto_seguro"
}

// Response 201
{
  "id": "uuid",
  "nombre": "CI/CD Pipeline",
  "url_inbound": "https://<host>/api/webhooks/abc123token",
  "is_active": true,
  "created_at": "2025-04-21T10:00:00Z"
}
```

**`POST /webhooks/{token}`** *(sistema externo)*
```json
// Headers requeridos
// X-Hub-Signature-256: sha256=<firma_hmac>

// Request Body
{
  "texto": "✅ Build #42 finalizado correctamente en rama main."
}

// Response 200
{ "publicado": true }
```

---

### 7.10 Resumen de Códigos de Respuesta HTTP

| Código | Significado | Uso habitual |
|--------|-------------|-------------|
| `200` | OK | Consulta o actualización exitosa |
| `201` | Created | Recurso creado exitosamente |
| `204` | No Content | Eliminación o cierre de sesión exitoso |
| `400` | Bad Request | Payload malformado o parámetros inválidos |
| `401` | Unauthorized | JWT inválido, expirado o credenciales incorrectas |
| `403` | Forbidden | Sin permisos para la acción solicitada |
| `404` | Not Found | Recurso no encontrado |
| `409` | Conflict | Recurso ya existente (email duplicado, miembro ya en canal) |
| `413` | Payload Too Large | Archivo supera el límite de tamaño permitido |
| `422` | Unprocessable Entity | Datos válidos pero regla de negocio violada |
| `451` | Unavailable For Legal Reasons | Archivo bloqueado por resultado de escaneo antivirus |
| `500` | Internal Server Error | Error interno no controlado del servidor |

---

## 8. Diseño UX/UI

### 7.1 Principios de Diseño

El diseño de la interfaz del Chat Empresarial se basa en los siguientes principios:

- **Claridad:** La interfaz debe ser intuitiva, reduciendo la curva de aprendizaje para empleados sin experiencia técnica avanzada.
- **Eficiencia:** Las acciones más frecuentes (enviar mensajes, buscar contactos, ver notificaciones) deben requerir el menor número de interacciones posible.
- **Consistencia:** Los componentes visuales, iconografía y paleta de colores deben mantenerse uniformes en toda la aplicación.
- **Accesibilidad:** La interfaz debe cumplir con estándares WCAG 2.1 nivel AA.
- **Seguridad visible:** Los indicadores de cifrado E2E y estado de entrega deben ser claros para el usuario sin ser intrusivos.

---

### 7.2 Flujos de Usuario Principales

#### Flujo de Autenticación

```
Pantalla de Login
      │
      ├── [Credenciales correctas + MFA habilitado]
      │         └── Pantalla de verificación TOTP
      │                    └── Dashboard principal
      │
      └── [Primer acceso / PENDING_MFA]
                 └── Pantalla de configuración MFA (QR + verificación)
                            └── Dashboard principal
```

#### Flujo de Mensajería

```
Dashboard / Lista de Canales
      │
      ├── Seleccionar canal existente
      │         └── Vista de conversación
      │                  ├── Escribir y enviar mensaje
      │                  ├── Adjuntar archivo
      │                  ├── Reaccionar con emoji
      │                  ├── Responder en hilo
      │                  └── Editar / Eliminar mensaje propio
      │
      └── Crear nuevo chat
                 ├── Chat individual (DM)
                 └── Chat grupal
                          └── Agregar miembros → Confirmar → Vista de conversación
```

---

### 7.3 Pantallas Principales

#### 1. Pantalla de Login

**Elementos:**
- Logo de la empresa / aplicación.
- Campo de email.
- Campo de contraseña (con toggle de visibilidad).
- Botón "Iniciar sesión".
- Enlace "Olvidé mi contraseña".
- Opción de "Login con SSO corporativo".

**Estados:**
- `Error`: mensaje de validación en rojo bajo el campo correspondiente.
- `Cargando`: indicador de spinner en el botón.
- `Bloqueado`: mensaje informando los minutos restantes de bloqueo.

---

#### 2. Pantalla de Verificación MFA

**Elementos:**
- Campo numérico de 6 dígitos (código TOTP).
- Contador de expiración del código.
- Botón "Verificar".
- Enlace "Usar código de recuperación".

---

#### 3. Dashboard Principal

**Estructura de Layout:**

```
┌──────────┬─────────────────────────────┬─────────────────┐
│          │   Encabezado del canal       │                 │
│  Barra   ├─────────────────────────────┤    Panel de     │
│ lateral  │                             │   detalles /    │
│          │   Área de mensajes          │    Miembros /   │
│ Canales  │   (scroll)                  │    Hilos        │
│ Directos │                             │                 │
│          ├─────────────────────────────┤                 │
│ Buscar   │   Barra de composición      │                 │
└──────────┴─────────────────────────────┴─────────────────┘
```

**Barra lateral izquierda:**
- Avatar y nombre del usuario con indicador de estado (verde = ONLINE, gris = OFFLINE).
- Buscador global de canales y usuarios.
- Sección "Mensajes directos" con lista de chats DM y badge de mensajes no leídos.
- Sección "Canales grupales" con lista de grupos.
- Sección "Salas de reunión" con salas efímeras activas.
- Botón "+" para crear nuevo chat o unirse a canal.
- Ícono de configuración y notificaciones en la parte inferior.

**Área de mensajes:**
- Mensajes agrupados por fecha.
- Burbuja de mensaje con: avatar, nombre, hora de envío.
- Indicadores de estado: ✓ enviado, ✓✓ entregado, ✓✓ (azul) leído.
- Etiqueta "editado" en mensajes modificados.
- Etiqueta "Mensaje eliminado" en mensajes borrados.
- Reacciones emoji agrupadas con contador.
- Vista de hilo (thread) accesible desde cada mensaje.
- Previsualización inline de imágenes y PDFs.

**Barra de composición:**
- Campo de texto con soporte para formato básico (negrita, cursiva, código).
- Botón de adjuntar archivo.
- Botón de emoji.
- Botón de encuesta.
- Botón de envío (activo solo cuando hay contenido).

---

#### 4. Vista de Hilo (Thread)

**Elementos:**
- Mensaje padre en la parte superior (destacado).
- Respuestas anidadas debajo.
- Campo de respuesta al hilo.
- Indicador del número de respuestas en el canal principal.

---

#### 5. Panel de Administración

**Accesible solo para el rol Administrador.**

Secciones:
- **Usuarios:** listado, buscar, ver detalle, suspender / activar.
- **Canales:** listado de todos los canales, ver miembros, eliminar canal.
- **Auditoría:** tabla de `audit_logs` con filtros por usuario, acción y fecha; exportar CSV/JSON.
- **Configuración:** política de retención de mensajes por canal o tipo.
- **Reportes:** métricas de uso por departamento.
- **Webhooks:** listado de webhooks registrados, activar / desactivar.

---

### 7.4 Componentes de Interfaz Reutilizables

| Componente | Descripción |
|---|---|
| `AvatarEstado` | Avatar circular con indicador de estado online/offline |
| `BurbujaMensaje` | Componente de mensaje con soporte de estados, edición y eliminación |
| `IndicadorCifrado` | Ícono de candado que indica que la conversación está cifrada E2E |
| `BadgeNoLeidos` | Contador de mensajes no leídos sobre el ícono de canal |
| `PreviewArchivo` | Vista previa inline para imágenes y PDFs adjuntos |
| `ReaccionEmoji` | Fila de emojis con contadores y toggle de reacción |
| `EncuestaCard` | Tarjeta de encuesta con opciones, barra de progreso y resultados |
| `NotificacionToast` | Notificación emergente no bloqueante en la esquina superior derecha |
| `HiloThread` | Panel lateral deslizable con el hilo de respuestas |
| `BuscadorGlobal` | Buscador con resultados de canales, usuarios y mensajes |

---

### 7.5 Paleta de Colores y Tipografía

| Elemento | Valor sugerido |
|---|---|
| Color primario | `#1A73E8` (azul corporativo) |
| Color secundario | `#34A853` (verde para estados activos) |
| Color de fondo | `#F8F9FA` (gris muy claro) |
| Color de fondo sidebar | `#1E2430` (azul oscuro) |
| Color de texto principal | `#202124` |
| Color de texto secundario | `#5F6368` |
| Color de error | `#D93025` |
| Color de éxito | `#1E8E3E` |
| Tipografía principal | Inter / Roboto, 14px base |
| Tipografía monoespaciada | JetBrains Mono (bloques de código) |

---

### 7.6 Consideraciones de Accesibilidad

- Todos los íconos deben contar con `aria-label` descriptivo.
- El contraste de color debe cumplir con una relación mínima de 4.5:1 para texto normal.
- La aplicación debe ser completamente navegable por teclado.
- Los estados de carga deben anunciarse mediante `aria-live`.
- Los mensajes de error deben estar asociados al campo correspondiente via `aria-describedby`.
- El tamaño mínimo de área de toque para elementos interactivos debe ser de 44×44px (criterio móvil).

---

## 9. Glosario

| Término | Definición |
|---|---|
| **AES-256-GCM** | Algoritmo de cifrado simétrico de 256 bits en modo Galois/Counter, usado para cifrar mensajes y archivos. |
| **Canal** | Espacio de comunicación entre dos o más usuarios. Puede ser DM (directo), grupal o efímero. |
| **Chat DM** | Chat directo entre exactamente dos usuarios. |
| **ECDH** | Elliptic-curve Diffie-Hellman. Protocolo de intercambio de claves para derivar claves de sesión compartidas. |
| **E2E** | Cifrado de extremo a extremo. El servidor no puede descifrar el contenido de los mensajes. |
| **JWT** | JSON Web Token. Token firmado que representa una sesión autenticada. |
| **JMS** | Jakarta Messaging Service. Sistema de mensajería asíncrona para comunicación entre componentes. |
| **MDB** | Message-Driven Bean. Componente Jakarta EE que procesa mensajes JMS de forma asíncrona. |
| **MFA / TOTP** | Autenticación multifactor mediante contraseñas de un solo uso basadas en tiempo (RFC 6238). |
| **MinIO** | Servicio de almacenamiento de objetos compatible con la API de Amazon S3. |
| **Refresh Token** | Token de larga duración usado para renovar el JWT de acceso sin requerir nueva autenticación. |
| **Sala efímera** | Canal temporal con fecha de expiración automática, usado para reuniones. |
| **Soft-delete** | Técnica de eliminación lógica que marca un registro como eliminado sin borrarlo físicamente. |
| **Thread / Hilo** | Cadena de respuestas anidadas bajo un mensaje padre dentro de un canal. |
| **Webhook** | Endpoint HTTP que permite a sistemas externos publicar mensajes en un canal. |
| **WebSocket** | Protocolo de comunicación bidireccional en tiempo real sobre TCP. |
| **JPQL** | Jakarta Persistence Query Language. Lenguaje de consulta orientado a objetos para JPA. |

---

*Documentación generada para el Proyecto Chat Empresarial — Grupo 5 — UTEC 5to año*
*Versión 1.0 — Jakarta EE 10 / WildFly 31*
