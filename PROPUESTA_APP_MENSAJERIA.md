# Propuesta: App de Mensajería Empresarial en Java EE

> Documento de discusión técnica — v0.1 (2026-04-06)

---

## 1. Visión general

Aplicación de mensajería orientada a entornos corporativos, construida sobre Java EE (Jakarta EE), con cifrado extremo a extremo como requisito excluyente. El objetivo es proveer una plataforma segura, auditable y extensible que pueda integrarse al ecosistema empresarial existente.

---

## 2. Requisito excluyente: Encriptación

| Capa | Mecanismo propuesto | Notas |
|---|---|---|
| Mensajes en tránsito | TLS 1.3 | Obligatorio en todos los endpoints |
| Mensajes en reposo | AES-256-GCM | Cifrado en BD antes de persistir |
| E2E (extremo a extremo) | ECDH + AES-256 | Clave derivada por sesión/par de usuarios |
| Claves de usuario | RSA-4096 o Curve25519 | Par pública/privada; clave privada nunca sale del cliente |
| Archivos adjuntos | AES-256 + hash SHA-256 | Verificación de integridad al descargar |

### Decisiones pendientes para el equipo
- [ ] ¿Implementamos E2E puro (Signal Protocol) o E2E gestionado por servidor (más simple, menor seguridad)?
- [ ] ¿KMS propio o delegamos a AWS KMS / Azure Key Vault?
- [ ] ¿Cómo manejamos la rotación de claves sin interrumpir conversaciones activas?

---

## 3. Funcionalidades base (MVP)

### 3.1 Mensajería
- Mensajes 1 a 1 en tiempo real (WebSocket)
- Grupos / canales (hasta N miembros configurables)
- Estado de mensaje: enviado / entregado / leído
- Historial con búsqueda full-text
- Edición y eliminación de mensajes (con auditoría)

### 3.2 Usuarios y autenticación
- Registro, login, logout
- MFA (TOTP — Google Authenticator compatible)
- SSO empresarial via SAML 2.0 / OAuth 2.0 + OIDC
- Gestión de sesiones con refresh token rotativo
- Roles: Admin, Manager, User, Guest

### 3.3 Notificaciones
- Push notifications (web / móvil)
- Notificaciones internas en-app
- Resumen diario por email (configurable)

### 3.4 Archivos y medios
- Subida de archivos (PDF, imágenes, documentos Office)
- Límite de tamaño configurable por rol
- Previsualización en línea
- Escaneo antivirus (ClamAV o integración externa)

---

## 4. Addons / diferenciadores

### 4.1 Mensajes con tiempo de expiración (auto-destrucción)
Mensajes configurables para eliminarse automáticamente después de X tiempo (estilo Snapchat for Business). Útil para comunicaciones sensibles.

### 4.2 Salas de reunión con chat efímero
Canal temporal asociado a una reunión; se archiva o destruye al finalizar. Integración futura con videollamadas (Jitsi / BigBlueButton).

### 4.3 Bot / asistente interno
- Recordatorios y alertas automáticas
- Notificaciones de sistemas externos (CI/CD, monitoring, ERP)
- Webhook inbound configurable por canal

### 4.4 Hilos de conversación (threads)
Respuestas anidadas dentro de un mensaje, evitando ruido en el canal principal (similar a Slack threads).

### 4.5 Reacciones y encuestas
- Reacciones emoji por mensaje
- Encuestas rápidas dentro de un canal (votación anónima opcional)

### 4.6 Panel de administración y auditoría
- Log de actividad exportable (CSV / JSON)
- Reportes de uso por departamento
- Políticas de retención de mensajes configurables (compliance GDPR / normativa interna)
- Bloqueo/suspensión de usuarios en tiempo real

### 4.7 Integración con directorio corporativo (LDAP / Active Directory)
Sincronización de usuarios y grupos desde el directorio empresarial existente.

---

## 5. Arquitectura propuesta

```
┌─────────────────────────────────────────┐
│           Clientes                      │
│  Web SPA (React/Angular)  |  App Móvil  │
└────────────┬────────────────────────────┘
             │ HTTPS / WSS (TLS 1.3)
┌────────────▼────────────────────────────┐
│          API Gateway / Load Balancer    │
│          (Nginx / WildFly / Payara)     │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│        Java EE Backend (Jakarta EE 10)  │
│  ┌─────────────┐  ┌──────────────────┐  │
│  │ REST API    │  │ WebSocket Server │  │
│  │ (JAX-RS)    │  │ (JSR-356)        │  │
│  └─────────────┘  └──────────────────┘  │
│  ┌─────────────┐  ┌──────────────────┐  │
│  │ EJB / CDI   │  │ MDB (JMS)        │  │
│  │ (lógica)    │  │ (async/eventos)  │  │
│  └─────────────┘  └──────────────────┘  │
│  ┌─────────────┐  ┌──────────────────┐  │
│  │ JPA / JPQL  │  │ Bean Validation  │  │
│  └─────────────┘  └──────────────────┘  │
└────────────┬────────────────────────────┘
             │
┌────────────▼─────────────────────────────────┐
│  Infraestructura                             │
│  PostgreSQL (datos)  |  Redis (caché/sesión) │
│  ActiveMQ / Artemis (JMS broker)             │
│  MinIO / S3 (almacenamiento archivos)        │
└──────────────────────────────────────────────┘
```

---

## 6. Stack tecnológico

| Capa | Tecnología | Alternativa |
|---|---|---|
| Runtime | WildFly 31 / Payara 6 | TomEE |
| ORM | Hibernate 6 (JPA) | EclipseLink |
| Mensajería async | ActiveMQ Artemis (JMS) | RabbitMQ |
| WebSocket | Jakarta WebSocket (JSR-356) | — |
| REST | JAX-RS (RESTEasy) | Jersey |
| Seguridad | Jakarta Security + JJWT | Keycloak |
| Cifrado | Bouncy Castle + Java Security | — |
| Base de datos | PostgreSQL 16 | MySQL 8 |
| Caché | Redis 7 | Hazelcast |
| Almacenamiento | MinIO (S3-compatible) | AWS S3 |
| Build | Maven 3 | Gradle |
| Contenedores | Docker + Docker Compose | Podman |

---

## 7. Modelo de datos (preliminar)

```
User            Message           Channel
-----           -------           -------
id (UUID)       id (UUID)         id (UUID)
username        sender_id (FK)    name
email           channel_id (FK)   type (DM/GROUP)
password_hash   content_enc       created_by (FK)
public_key      iv                created_at
role            sent_at
mfa_secret      delivered_at      ChannelMember
status          read_at           -------------
                expires_at        channel_id (FK)
                edited            user_id (FK)
                deleted           role (OWNER/MEMBER)
                                  joined_at
```

---

## 8. Consideraciones de seguridad adicionales

- **Rate limiting** por usuario/IP para prevenir spam y brute force
- **CSRF protection** en todos los endpoints de mutación
- **CSP headers** estrictos en el frontend
- **OWASP Top 10** como checklist obligatorio antes de cada release
- **Penetration testing** interno antes de despliegue en producción
- **Secrets management**: variables de entorno + Vault, nunca hardcoded

---

## 9. Fases de desarrollo sugeridas

### Fase 1 — MVP (Core seguro)
- Auth con MFA
- Mensajería 1 a 1 cifrada (E2E o server-side, definir en equipo)
- Grupos básicos
- WebSocket en tiempo real
- Panel admin básico

### Fase 2 — Addons clave
- Archivos adjuntos cifrados
- Hilos de conversación
- Mensajes con expiración
- Integración LDAP

### Fase 3 — Diferenciadores y escala
- Bot / webhooks
- Auditoría completa + reportes
- SSO SAML/OIDC
- Optimización de rendimiento y escalabilidad horizontal

---

## 10. Preguntas abiertas para el equipo

1. **Encriptación**: ¿E2E puro (clave privada solo en cliente) o E2E con backup de clave en servidor?
2. **Hosting**: ¿On-premise, nube privada o nube pública? Impacta la elección de KMS y almacenamiento.
3. **Clientes**: ¿Solo web o también app móvil nativa en el alcance actual?
4. **Compliance**: ¿Hay normativa específica a cumplir (GDPR, HIPAA, normativa local)?
5. **Escala inicial**: ¿Cuántos usuarios concurrentes esperamos en el primer despliegue?
6. **Autenticación**: ¿Ya existe un proveedor de identidad (Active Directory, Keycloak) que debamos integrar?
7. **Prioridad de addons**: ¿Cuáles de los diferenciadores son must-have para el cliente?

---

## 11. Próximos pasos

- [ ] Revisar y completar este documento con el equipo
- [ ] Resolver las preguntas abiertas (sección 10)
- [ ] Definir criterios de aceptación del MVP
- [ ] Estimar esfuerzo por fase
- [ ] Crear repositorio de proyecto y estructura base
- [ ] Configurar entorno de desarrollo con Docker Compose

---

*Autor inicial: MatiasAlfaroDev — para discusión interna del equipo*
