# Diagrama Entidad-Relación — App de Mensajería Empresarial

> Versión 1.0 — Alcance total del proyecto

---

```mermaid
erDiagram

    users {
        uuid        id              PK
        string      username
        string      email
        string      password_hash
        string      public_key
        string      role
        string      mfa_secret
        string      status
        timestamp   created_at
        timestamp   updated_at
    }

    channels {
        uuid        id              PK
        string      name
        string      type
        uuid        created_by      FK
        boolean     is_ephemeral
        timestamp   expires_at
        timestamp   created_at
    }

    channel_members {
        uuid        channel_id      FK
        uuid        user_id         FK
        string      role
        timestamp   joined_at
    }

    messages {
        uuid        id              PK
        uuid        sender_id       FK
        uuid        channel_id      FK
        uuid        parent_id       FK
        text        content_enc
        string      iv
        timestamp   sent_at
        timestamp   delivered_at
        timestamp   read_at
        timestamp   expires_at
        timestamp   edited_at
        timestamp   deleted_at
    }

    attachments {
        uuid        id              PK
        uuid        message_id      FK
        string      file_name
        string      storage_path
        string      mime_type
        bigint      size_bytes
        string      hash_sha256
        timestamp   uploaded_at
        timestamp   scanned_at
        string      scan_result
    }

    message_reactions {
        uuid        id              PK
        uuid        message_id      FK
        uuid        user_id         FK
        string      emoji
        timestamp   created_at
    }

    polls {
        uuid        id              PK
        uuid        channel_id      FK
        uuid        created_by      FK
        text        question
        boolean     is_anonymous
        timestamp   expires_at
        timestamp   created_at
    }

    poll_options {
        uuid        id              PK
        uuid        poll_id         FK
        string      option_text
    }

    poll_votes {
        uuid        id              PK
        uuid        poll_option_id  FK
        uuid        user_id         FK
        timestamp   voted_at
    }

    notifications {
        uuid        id              PK
        uuid        user_id         FK
        string      type
        string      title
        text        body
        string      related_entity_type
        uuid        related_entity_id
        timestamp   read_at
        timestamp   created_at
    }

    audit_logs {
        uuid        id              PK
        uuid        user_id         FK
        string      action
        string      entity_type
        uuid        entity_id
        text        old_value
        text        new_value
        string      ip_address
        timestamp   created_at
    }

    refresh_tokens {
        uuid        id              PK
        uuid        user_id         FK
        string      token_hash
        timestamp   expires_at
        boolean     revoked
        timestamp   created_at
    }

    webhooks {
        uuid        id              PK
        uuid        channel_id      FK
        uuid        created_by      FK
        string      name
        string      url
        string      secret_hash
        boolean     is_active
        timestamp   created_at
    }

    meeting_rooms {
        uuid        id              PK
        uuid        channel_id      FK
        uuid        created_by      FK
        string      title
        timestamp   started_at
        timestamp   ended_at
    }

    %% ── Autenticación ──────────────────────────────────
    users           ||--o{ refresh_tokens      : "tiene"
    users           ||--o{ channel_members     : "pertenece a"

    %% ── Canales ────────────────────────────────────────
    users           ||--o{ channels            : "crea"
    channels        ||--o{ channel_members     : "tiene"
    channels        ||--o{ messages            : "contiene"
    channels        ||--o{ polls               : "tiene"
    channels        ||--o{ webhooks            : "tiene"
    channels        ||--o| meeting_rooms       : "tiene"

    %% ── Mensajes ───────────────────────────────────────
    users           ||--o{ messages            : "envía"
    messages        ||--o{ messages            : "tiene hilo"
    messages        ||--o{ attachments         : "tiene"
    messages        ||--o{ message_reactions   : "recibe"

    %% ── Interacciones ──────────────────────────────────
    users           ||--o{ message_reactions   : "hace"
    users           ||--o{ polls               : "crea"
    polls           ||--o{ poll_options        : "tiene"
    poll_options    ||--o{ poll_votes          : "recibe"
    users           |o--o{ poll_votes          : "emite"

    %% ── Sistema ────────────────────────────────────────
    users           ||--o{ notifications       : "recibe"
    users           ||--o{ audit_logs          : "genera"
    users           ||--o{ webhooks            : "configura"
    users           ||--o{ meeting_rooms       : "inicia"
```

---

## Notas del modelo

### Decisiones de diseño

| Entidad | Decisión | Motivo |
|---|---|---|
| `messages.parent_id` | Auto-referencia (nullable) | Soporta hilos sin tabla extra |
| `messages.content_enc` + `iv` | Contenido siempre cifrado | Requisito excluyente E2E / AES-256-GCM |
| `users.public_key` | Almacenada en servidor | Necesaria para derivación ECDH entre pares |
| `poll_votes.user_id` | Nullable | Soporte para votaciones anónimas |
| `channels.is_ephemeral` + `expires_at` | Flag + timestamp | Salas de reunión con auto-destrucción |
| `attachments.hash_sha256` | Hash de integridad | Verificación al descargar (sección 2 propuesta) |
| `audit_logs.old_value / new_value` | JSON serializado | Trazabilidad completa para compliance |

### Entidades por módulo

| Módulo | Entidades |
|---|---|
| Auth & Sesiones | `users`, `refresh_tokens` |
| Mensajería | `channels`, `channel_members`, `messages` |
| Archivos | `attachments` |
| Interacción | `message_reactions`, `polls`, `poll_options`, `poll_votes` |
| Notificaciones | `notifications` |
| Auditoría | `audit_logs` |
| Bots & Webhooks | `webhooks` |
| Reuniones | `meeting_rooms` |
```
