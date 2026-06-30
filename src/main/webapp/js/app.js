/* ════════════════════════════════════════════════════════════
   Terotalk — cliente web
   Mismo diseño y funcionalidad que la app Expo (Java-EE-Front).
   Consume la API REST (/api) y el WebSocket (/ws/chat) del WAR.
   ════════════════════════════════════════════════════════════ */

(() => {
    "use strict";

    // ───────── Estado ─────────
    const state = {
        token: localStorage.getItem("token") || null,
        usuario: JSON.parse(localStorage.getItem("usuario") || "null"),
        chats: [],
        usuarios: [],
        miembros: [],
        chatActual: null,
        mensajes: [],
        editando: null,        // mensaje en edición
        menuMensaje: null,     // mensaje del menú abierto
        reenviarId: null,
        seleccionGrupo: new Set(),
        ws: null,
        wsTimer: null,
        wsPing: null,
        online: false,
        groupKeys: {},    // chatId (number) → CryptoKey AES-GCM
        pubKeyCache: {}   // userId (number) → b64 SPKI
    };

    const $ = (sel) => document.querySelector(sel);

    // ───────── Helpers (utils de la app Expo) ─────────

    // constants/emojis.ts
    const QUICK_EMOJIS = ["👍", "❤️", "😂", "😮", "😢", "🔥", "👏", "✅"];

    // utils/avatar.ts
    const AVATAR_COLORS = [
        "#1A3558", "#2D1B69", "#0C3344", "#1B3A26",
        "#3B1F2B", "#1F2D3B", "#2B2600", "#1A2D44"
    ];
    const avatarColor = (str) =>
        AVATAR_COLORS[(str || "").charCodeAt(0) % AVATAR_COLORS.length];

    const getInitials = (nombre) => {
        const partes = String(nombre || "?").trim().split(/\s+/);
        if (partes.length === 1) return partes[0].slice(0, 2).toUpperCase();
        return (partes[0][0] + partes[partes.length - 1][0]).toUpperCase();
    };

    // utils/fecha.ts
    const formatDuracion = (s) => {
        const m = Math.floor((s || 0) / 60);
        const sec = Math.floor((s || 0) % 60);
        return `${m}:${sec.toString().padStart(2, "0")}`;
    };

    const horaCorta = (iso) => {
        const d = new Date(iso);
        if (isNaN(d)) return "";
        return d.toLocaleTimeString("es-PE", { hour: "2-digit", minute: "2-digit", hour12: false });
    };

    const fechaSeparador = (iso) => {
        const d = new Date(iso);
        if (isNaN(d)) return "";
        return d.toLocaleDateString("es-PE", { weekday: "long", day: "numeric", month: "long" });
    };

    function crearAvatar(nombre, size, initials) {
        const span = document.createElement("span");
        span.className = `avatar avatar-${size}`;
        span.textContent = initials || getInitials(nombre);
        span.style.background = avatarColor(span.textContent);
        return span;
    }

    function setAvatar(el, nombre, initials) {
        el.textContent = initials || getInitials(nombre);
        el.style.background = avatarColor(el.textContent);
    }

    // ───────── Toast ─────────
    function mostrarToast(msg, tipo = "info") {
        let cont = document.getElementById("toast-container");
        if (!cont) {
            cont = document.createElement("div");
            cont.id = "toast-container";
            document.body.appendChild(cont);
        }
        const t = document.createElement("div");
        t.className = `toast ${tipo}`;
        t.textContent = msg;
        cont.appendChild(t);
        setTimeout(() => t.remove(), 3000);
    }

    // ───────── Cliente API ─────────
    async function api(method, path, body) {
        const headers = {};
        if (body !== undefined) headers["Content-Type"] = "application/json";
        if (state.token) headers["Authorization"] = state.token;

        const res = await fetch(path, {
            method,
            headers,
            body: body !== undefined ? JSON.stringify(body) : undefined
        });

        const texto = await res.text();
        if (!res.ok) {
            console.log("STATUS:", res.status);
            console.log("RESPUESTA:", texto);
            throw new Error(texto || `Error ${res.status}`);
        }
        try { return JSON.parse(texto); } catch { return texto; }
    }

    // ───────── Autenticación ─────────
    async function login(email, password) {
        const data = await api("POST", "api/usuarios/login", { email, password });
        const u = data.usuario;
        state.token = data.token;
        state.usuario = {
            id: u.id,
            nombre: u.nombre,
            email: u.email,
            rol: u.rol || "USER",
            initials: getInitials(u.nombre)
        };
        localStorage.setItem("token", state.token);
        localStorage.setItem("usuario", JSON.stringify(state.usuario));
        entrarApp();
    }

    async function logout() {
        try { await api("POST", "api/usuarios/logout"); } catch { /* token ya inválido */ }
        cerrarSesionLocal();
    }

    function cerrarSesionLocal() {
        localStorage.removeItem("token");
        localStorage.removeItem("usuario");
        state.token = null;
        state.usuario = null;
        state.chatActual = null;
        if (state.ws) { state.ws.onclose = null; state.ws.close(); state.ws = null; }
        clearTimeout(state.wsTimer);
        clearInterval(state.wsPing);
        cerrarOverlays();
        $("#vista-app").hidden = true;
        $("#vista-login").hidden = false;
        $("#login-password").value = "";
    }

    // ───────── WebSocket ─────────
    function wsUrl() {
        const proto = location.protocol === "https:" ? "wss://" : "ws://";
        const dir = location.pathname.endsWith("/")
            ? location.pathname
            : location.pathname.replace(/[^/]*$/, "");
        return proto + location.host + dir + "ws/chat?token=" + encodeURIComponent(state.token);
    }

    function conectarWs() {
        if (!state.token) return;

        const ws = new WebSocket(wsUrl());
        state.ws = ws;

        ws.onopen = () => {
            setOnline(true);
            clearInterval(state.wsPing);
            state.wsPing = setInterval(() => {
                if (ws.readyState === WebSocket.OPEN) ws.send("ping");
            }, 25000);
        };

        ws.onmessage = (ev) => {
            let data;
            try { data = JSON.parse(ev.data); } catch { return; }
            manejarEventoWs(data);
        };

        ws.onclose = () => {
            setOnline(false);
            clearInterval(state.wsPing);
            if (state.token) {
                clearTimeout(state.wsTimer);
                state.wsTimer = setTimeout(conectarWs, 3000);
            }
        };

        ws.onerror = () => ws.close();
    }

    function setOnline(online) {
        state.online = online;
        $("#mi-dot").className = "dot " + (online ? "dot-online" : "dot-offline");
    }

    // typing indicator state
    let typingTimer = null;
    const typingUsers = new Map(); // nombre → timer

    function mostrarTyping() {
        const ind = $("#typing-indicator");
        const nombres = [...typingUsers.keys()];
        if (nombres.length === 0) { ind.hidden = true; return; }
        const label = nombres.length === 1
            ? `${nombres[0]} está escribiendo`
            : `${nombres.slice(0, 2).join(" y ")} están escribiendo`;
        ind.querySelector(".typing-texto").textContent = label;
        ind.hidden = false;
    }

    function enviarTyping() {
        if (!state.ws || !state.chatActual || !state.usuario) return;
        state.ws.send(JSON.stringify({
            tipo: "TYPING",
            chatId: state.chatActual.id,
            nombre: state.usuario.nombre
        }));
        clearTimeout(typingTimer);
        typingTimer = setTimeout(() => {
            if (!state.ws || !state.chatActual) return;
            state.ws.send(JSON.stringify({
                tipo: "STOP_TYPING",
                chatId: state.chatActual.id,
                nombre: state.usuario.nombre
            }));
        }, 3000);
    }

    async function manejarEventoWs(data) {
        const chatId = Number(data.chatId);
        const msgId = Number(data.messageId ?? data.mensajeId ?? data.id);

        // typing events
        if (data.tipo === "TYPING" || data.tipo === "STOP_TYPING") {
            if (!state.chatActual || chatId !== state.chatActual.id) return;
            const nombre = data.nombre || "Alguien";
            if (data.tipo === "TYPING") {
                clearTimeout(typingUsers.get(nombre));
                typingUsers.set(nombre, setTimeout(() => {
                    typingUsers.delete(nombre);
                    mostrarTyping();
                }, 4000));
            } else {
                clearTimeout(typingUsers.get(nombre));
                typingUsers.delete(nombre);
            }
            mostrarTyping();
            return;
        }

        switch (data.type) {
            case "message_delivered":
            case "message_read":
            case "message_edited":
            case "message_deleted":
            case "MESSAGE_REACTION": {
                const esDelChatAbierto =
                    (state.chatActual && chatId === state.chatActual.id) ||
                    state.mensajes.some(m => m.id === msgId);
                if (esDelChatAbierto && state.chatActual) {
                    await cargarMensajes(state.chatActual.id, { mantenerScroll: true });
                }
                if (data.type === "message_edited" || data.type === "message_deleted") {
                    cargarChats();
                }
                return;
            }
            default: {
                // mensaje nuevo: { id, chatId, remitente, remitenteId, contenido, timestamp }
                if (data.remitenteId === undefined || data.chatId === undefined) return;

                const esMio = Number(data.remitenteId) === state.usuario.id;

                if (!esMio && msgId) {
                    api("POST", `api/mensajes/${msgId}/entregado`).catch(() => {});
                }

                if (state.chatActual && chatId === state.chatActual.id) {
                    if (!esMio) api("POST", `api/mensajes/${chatId}/leido`).catch(() => {});
                    await cargarMensajes(chatId);
                }
                cargarChats();
            }
        }
    }

    // ───────── Chats (sidebar) ─────────
    async function cargarChats() {
        try {
            state.chats = await api("GET", "api/chats");
        } catch {
            return;
        }
        renderChats();
    }

    function renderChats() {
        const filtro = $("#buscar-chat").value.trim().toLowerCase();
        const lista = $("#lista-chats");
        lista.innerHTML = "";

        const visibles = state.chats.filter(c =>
            !filtro || (c.nombre || "").toLowerCase().includes(filtro));

        $("#chats-vacio").hidden = visibles.length > 0;
        lista.hidden = visibles.length === 0;

        for (const chat of visibles) {
            const nombre = chat.nombre && chat.nombre.trim() ? chat.nombre : "Chat sin nombre";
            const preview = chat.lastMsg && chat.lastMsg.trim() ? chat.lastMsg : "Sin mensajes aún";

            const li = document.createElement("li");
            li.className = "chat-row" + (state.chatActual?.id === chat.id ? " activo" : "");

            const avatarWrap = document.createElement("div");
            avatarWrap.className = "avatar-wrap";
            avatarWrap.appendChild(crearAvatar(nombre, 44, nombre.slice(0, 2).toUpperCase()));
            if (chat.estado != null) {
                const rowDot = document.createElement("span");
                rowDot.className = "dot " + (chat.estado === "ONLINE" ? "dot-online" : "dot-offline");
                avatarWrap.appendChild(rowDot);
            }
            li.appendChild(avatarWrap);

            const body = document.createElement("div");
            body.className = "chat-row-body";

            const top = document.createElement("div");
            top.className = "chat-row-top";
            const nom = document.createElement("span");
            nom.className = "chat-row-nombre";
            nom.textContent = nombre;
            const hora = document.createElement("span");
            hora.className = "chat-row-hora";
            hora.textContent = chat.lastMsgTime ? horaCorta(chat.lastMsgTime) : "";
            top.append(nom, hora);

            const bottom = document.createElement("div");
            bottom.className = "chat-row-bottom";
            const msg = document.createElement("span");
            msg.className = "chat-row-msg";
            msg.textContent = preview;
            bottom.appendChild(msg);
            if (chat.unread > 0) {
                const badge = document.createElement("span");
                badge.className = "badge";
                badge.textContent = chat.unread;
                bottom.appendChild(badge);
            }

            body.append(top, bottom);
            li.appendChild(body);
            li.addEventListener("click", () => abrirChat(chat));
            lista.appendChild(li);
        }
    }

    async function abrirChat(chat) {
        state.chatActual = chat;
        typingUsers.clear();
        $("#typing-indicator").hidden = true;
        cancelarEdicion();
        $("#emoji-picker").hidden = true;
        $("#panel-vacio").hidden = true;
        $("#panel-chat").hidden = false;

        // responsive móvil
        document.querySelector(".sidebar").classList.add("oculta");
        document.querySelector(".panel").classList.add("visible");

        const nombre = chat.nombre || "Chat";
        $("#chat-nombre").textContent = nombre;
        
        const estado = $("#chat-estado");

            if (chat.estado == null) {

                estado.textContent = "";

                estado.classList.remove("online");

            } else if (chat.estado === "ONLINE") {

                estado.textContent = "En línea";

                estado.classList.add("online");

            } else {

                estado.textContent = "Desconectado";

                estado.classList.remove("online");
            }

        setAvatar($("#chat-avatar"), nombre, nombre.slice(0, 2).toUpperCase());

        await cargarMensajes(chat.id);
        api("POST", `api/mensajes/${chat.id}/leido`).catch(() => {});
        cargarChats();
        renderChats();
        $("#input-mensaje").focus();
    }

    function cerrarChat() {
        state.chatActual = null;
        $("#panel-chat").hidden = true;
        $("#panel-vacio").hidden = false;
        document.querySelector(".sidebar").classList.remove("oculta");
        document.querySelector(".panel").classList.remove("visible");
        renderChats();
    }

    // ───────── Mensajes ─────────
    async function cargarMensajes(chatId, { mantenerScroll = false } = {}) {
        let mensajes;
        try {
            mensajes = await api("GET", `api/mensajes/chat/${chatId}`);
        } catch {
            return;
        }
        if (!state.chatActual || state.chatActual.id !== chatId) return;

        state.mensajes = mensajes;

        // Descifrar mensajes cifrados
        const esGrupo = state.chatActual?.tipo === "GRUPO";
        for (const m of state.mensajes) {
            if (!m.cifrado || m.eliminado) { m._plain = m.contenido; continue; }
            try {
                if (esGrupo) {
                    const gk = await getGroupKey(chatId);
                    m._plain = gk ? await Crypto.decryptGroup(m.contenido, gk) : null;
                } else {
                    const otherPub = await getPubKey(m.sender_id === state.usuario.id
                        ? state.miembros?.find(x => x.id !== state.usuario.id)?.id
                        : m.sender_id);
                    m._plain = otherPub ? await Crypto.decryptDirect(m.contenido, otherPub) : null;
                }
            } catch { m._plain = null; }
        }

        const zona = $("#zona-mensajes");
        const estabaAbajo = zona.scrollHeight - zona.scrollTop - zona.clientHeight < 80;
        const scrollPrevio = zona.scrollTop;

        renderMensajes();

        if (mantenerScroll && !estabaAbajo) zona.scrollTop = scrollPrevio;
        else zona.scrollTop = zona.scrollHeight;
    }

    function renderMensajes() {
        const zona = $("#zona-mensajes");
        zona.innerHTML = "";
        let fechaPrevia = null;

        for (const m of state.mensajes) {
            const etiqueta = fechaSeparador(m.sent_at);
            if (etiqueta && etiqueta !== fechaPrevia) {
                fechaPrevia = etiqueta;
                const sep = document.createElement("div");
                sep.className = "separador";
                const span = document.createElement("span");
                span.textContent = etiqueta;
                sep.appendChild(span);
                zona.appendChild(sep);
            }
            zona.appendChild(renderMensaje(m));
        }
    }

    // ícono de estado — mismo orden que MessageBubble.tsx
    function iconoEstado(m) {
        let name = "checkmark-outline";
        let cls = "";
        if (m.estado === "PENDIENTE") name = "time-outline";
        else if (m.estado === "RECHAZADO") name = "alert-circle-outline";
        else if (m.leido) { name = "checkmark-done-outline"; cls = "leido"; }
        else if (m.entregado) name = "checkmark-done-outline";
        const icon = document.createElement("ion-icon");
        icon.setAttribute("name", name);
        if (cls) icon.className = cls;
        return icon;
    }

    async function obtenerUrlImagen(adjunto) {

        console.log("Adjunto:", adjunto);
        const res = await fetch(
            `api/mensajes/adjunto/${adjunto.urlArchivo}`,
            {
                headers: {
                    Authorization: state.token
                }
            }
        );
        
        console.log("Status:", res.status);
        
        if (!res.ok) {
            throw new Error("No se pudo descargar la imagen");
        }

        const blob = await res.blob();
        console.log("Blob:", blob);
        return URL.createObjectURL(blob);
    }

    function renderMensaje(m) {
        const esMio = m.sender_id === state.usuario.id;

        const wrap = document.createElement("div");
        wrap.className = "msg-wrap" + (esMio ? " mio" : "");

        if (!esMio) {
            wrap.appendChild(crearAvatar(m.sender_username, 28, m.sender_initials));
        }

        const col = document.createElement("div");
        col.className = "msg-col";

        if (!esMio) {
            const sender = document.createElement("span");
            sender.className = "msg-sender";
            sender.textContent = m.sender_username;
            col.appendChild(sender);
        }

        const bubble = document.createElement("div");
        bubble.className = "bubble " + (esMio ? "bubble-mio" : "bubble-otro");
        if (state.editando?.id === m.id) bubble.classList.add("editando");

        if (m.mensajeOrigenId > 0 && !m.eliminado) {
            const fwd = document.createElement("span");
            fwd.className = "bubble-fwd";
            fwd.textContent = "↪ Reenviado";
            bubble.appendChild(fwd);
        }

        const texto = document.createElement("span");
        texto.className = "bubble-texto" + (m.eliminado ? " eliminado" : "");
        if (m.eliminado) {
            texto.textContent = "Mensaje eliminado";
        } else if ((m.tipo === "ARCHIVO" || m.tipo === "IMAGEN" || m.tipo === "VIDEO") && m.cifrado) {
            const parsed = m._plain ? (() => { try { return JSON.parse(m._plain); } catch { return null; } })() : null;
            if (parsed) {
                texto.textContent = `🔒📎 ${parsed.fileName}`;
                texto.style.cursor = "pointer";
                texto.addEventListener("click", async () => {
                    try {
                        const res = await fetch(`api/mensajes/adjunto/${parsed.url}`, { headers: { Authorization: state.token } });
                        if (!res.ok) throw new Error("Error al descargar");
                        const encBlob = await res.blob();
                        const ciphertextB64 = await new Promise((resolve) => {
                            const reader = new FileReader();
                            reader.onload = () => resolve(reader.result.split(",")[1]);
                            reader.readAsDataURL(encBlob);
                        });
                        const decBlob = await Crypto.decryptFile(ciphertextB64, parsed.fileKeyB64);
                        if (!decBlob) { mostrarToast("No se pudo descifrar el archivo", "error"); return; }
                        const typedBlob = new Blob([await decBlob.arrayBuffer()], { type: parsed.mimeType });
                        window.open(URL.createObjectURL(typedBlob), "_blank");
                    } catch (e) { mostrarToast(e.message, "error"); }
                });
            } else {
                texto.textContent = "[adjunto no descifrable]";
            }
        } else if (m.tipo === "AUDIO" && m.adjunto) {
            const audioEl = document.createElement("audio");
            audioEl.preload = "metadata";
            obtenerUrlImagen(m.adjunto).then(url => { audioEl.src = url; }).catch(() => {});
            const audioWrap = document.createElement("div");
            audioWrap.className = "bubble-audio";
            const btnPlay = document.createElement("button");
            btnPlay.className = "btn-play-audio";
            btnPlay.innerHTML = '<ion-icon name="play-circle-outline"></ion-icon>';
            const barra = document.createElement("div");
            barra.className = "audio-barra";
            const progreso = document.createElement("div");
            progreso.className = "audio-progreso";
            barra.appendChild(progreso);
            const dur = document.createElement("span");
            dur.className = "audio-duracion";
            dur.textContent = "0:00";
            btnPlay.addEventListener("click", () => {
                if (audioEl.paused) { audioEl.play(); btnPlay.innerHTML = '<ion-icon name="pause-circle-outline"></ion-icon>'; }
                else { audioEl.pause(); btnPlay.innerHTML = '<ion-icon name="play-circle-outline"></ion-icon>'; }
            });
            audioEl.addEventListener("timeupdate", () => {
                const p = audioEl.duration ? (audioEl.currentTime / audioEl.duration) * 100 : 0;
                progreso.style.width = p + "%";
                dur.textContent = formatDuracion(audioEl.currentTime);
            });
            audioEl.addEventListener("ended", () => {
                btnPlay.innerHTML = '<ion-icon name="play-circle-outline"></ion-icon>';
                progreso.style.width = "0%";
                dur.textContent = "0:00";
            });
            audioWrap.append(btnPlay, barra, dur);
            bubble.appendChild(audioWrap);
        } else if ((m.tipo === "ARCHIVO" || m.tipo === "IMAGEN" || m.tipo === "VIDEO") && m.adjunto) {
            const esImagen = m.tipo === "IMAGEN";
            const esVideo = m.tipo === "VIDEO";
            if (esImagen) {
                const img = document.createElement("img");
                img.className = "bubble-image";
                obtenerUrlImagen(m.adjunto).then(url => { img.src = url; }).catch(console.error);
                img.addEventListener("click", async () => {
                    try { await abrirAdjunto(m.adjunto); } catch (e) { mostrarToast(e.message, "error"); }
                });
                bubble.appendChild(img);
            } else if (esVideo) {
                const video = document.createElement("div");
                video.className = "bubble-video";
                video.innerHTML = `<ion-icon name="play-circle" class="video-icon"></ion-icon><span>${m.adjunto.nombreArchivo}</span>`;
                video.addEventListener("click", async () => {
                    try { await abrirAdjunto(m.adjunto); } catch (e) { mostrarToast(e.message, "error"); }
                });
                bubble.appendChild(video);
            } else {
                texto.textContent = `📎 ${m.adjunto.nombreArchivo}`;
                texto.style.cursor = "pointer";
                texto.addEventListener("click", async () => {
                    try { await abrirAdjunto(m.adjunto); } catch (e) { mostrarToast(e.message, "error"); }
                });
                bubble.appendChild(texto);
            }
        } else {
            if (m.cifrado && m._plain === null) {
                texto.textContent = "[no se puede descifrar en este dispositivo]";
                texto.style.fontStyle = "italic";
                texto.style.opacity = "0.6";
            } else {
                texto.textContent = (m.cifrado ? m._plain : m.contenido) ?? "";
            }
        }

        if (!m.adjunto || m.cifrado) {
            bubble.appendChild(texto);
        }

        if (!m.eliminado) {
            const meta = document.createElement("span");
            meta.className = "bubble-meta";
            if (m.editado) {
                const ed = document.createElement("span");
                ed.className = "editado";
                ed.textContent = "editado";
                meta.appendChild(ed);
            }
            const hora = document.createElement("span");
            hora.className = "hora";
            hora.textContent = horaCorta(m.sent_at);
            meta.appendChild(hora);
            if (esMio) meta.appendChild(iconoEstado(m));
            bubble.appendChild(meta);

            // reacciones agrupadas (ReactionBar)
            if (m.reacciones && m.reacciones.length) {
                const grupos = {};
                for (const r of m.reacciones) {
                    grupos[r.emoji] = grupos[r.emoji] || { count: 0, mia: false, nombres: [] };
                    grupos[r.emoji].count++;
                    grupos[r.emoji].nombres.push(r.usuarioNombre);
                    if (r.usuarioId === state.usuario.id) grupos[r.emoji].mia = true;
                }
                const fila = document.createElement("div");
                fila.className = "reacciones";
                for (const [emoji, g] of Object.entries(grupos)) {
                    const chip = document.createElement("span");
                    chip.className = "reaccion-chip" + (g.mia ? " mia" : "");
                    chip.textContent = g.count > 1 ? `${emoji} ${g.count}` : emoji;
                    chip.title = g.nombres.join(", ");
                    fila.appendChild(chip);
                }
                bubble.appendChild(fila);
            }

            bubble.addEventListener("click", () => abrirMenuMensaje(m, esMio));
        }

        col.appendChild(bubble);
        wrap.appendChild(col);
        return wrap;
    }

    // ───────── Menú de mensaje (overlay centrado, como Expo) ─────────
    function abrirMenuMensaje(m, esMio) {
            console.log("TIPO:", m.tipo);
    console.log("MENSAJE:", m);
        state.menuMensaje = m;
        const box = $("#menu-mensaje-box");
        box.innerHTML = "";

        // reacciones rápidas
        const emojis = document.createElement("div");
        emojis.className = "menu-emojis";
        for (const e of QUICK_EMOJIS.slice(0, 6)) {
            const b = document.createElement("button");
            b.textContent = e;
            b.addEventListener("click", () => { cerrarMenuMensaje(); reaccionar(m.id, e); });
            emojis.appendChild(b);
        }
        box.appendChild(emojis);

        const item = (texto, fn, peligro = false) => {
            const b = document.createElement("button");
            b.textContent = texto;
            if (peligro) b.classList.add("peligro");
            b.addEventListener("click", () => { cerrarMenuMensaje(); fn?.(); });
            box.appendChild(b);
        };

        if (esMio && m.tipo === 'TEXTO') item("Editar", () => iniciarEdicion(m));
        item("Eliminar para mí", () => eliminarParaMi(m.id), true);
        if (esMio) item("Eliminar para todos", () => eliminarParaTodos(m.id), true);
        item("Reenviar", () => abrirModalReenviar(m.id));
        item("Cancelar");

        $("#menu-mensaje").hidden = false;
    }

    function cerrarMenuMensaje() {
        state.menuMensaje = null;
        $("#menu-mensaje").hidden = true;
    }

    // ───────── Acciones de mensaje ─────────
    async function enviarMensaje() {
        const input = $("#input-mensaje");
        const contenido = input.value.trim();
        if (!contenido || !state.chatActual) return;

        const chatId = state.chatActual.id;
        const esGrupo = state.chatActual.tipo === "GRUPO";

        try {
            if (state.editando) {
                // Edición: cifrar igual que al enviar
                let payload = contenido;
                let cifrado = false;
                if (esGrupo) {
                    const gk = await getGroupKey(chatId);
                    if (gk) { payload = await Crypto.encryptGroup(contenido, gk); cifrado = true; }
                } else {
                    // 1:1: necesitamos la pub del otro miembro
                    const otro = state.miembros?.find(m => m.id !== state.usuario.id);
                    if (otro) {
                        const pub = await getPubKey(otro.id);
                        if (pub) { payload = await Crypto.encryptDirect(contenido, pub); cifrado = true; }
                    }
                }
                await api("PUT", `api/mensajes/${state.editando.id}`, { contenido: payload, cifrado });
                cancelarEdicion();
            } else {
                let payload = contenido;
                let cifrado = false;
                if (esGrupo) {
                    const gk = await getGroupKey(chatId);
                    if (gk) { payload = await Crypto.encryptGroup(contenido, gk); cifrado = true; }
                } else {
                    const otro = state.miembros?.find(m => m.id !== state.usuario.id);
                    if (!otro) {
                        // cargar miembros si no los tenemos aún
                        const ms = await api("GET", `api/chats/${chatId}/miembros`);
                        state.miembros = ms;
                    }
                    const otroM = state.miembros?.find(m => m.id !== state.usuario.id);
                    if (otroM) {
                        const pub = await getPubKey(otroM.id);
                        if (pub) { payload = await Crypto.encryptDirect(contenido, pub); cifrado = true; }
                    }
                    if (!cifrado) {
                        throw new Error("No se puede cifrar: destinatario sin clave E2E registrada");
                    }
                    await api("POST", "api/mensajes/enviar", { chatId, contenido: payload, tipo: "TEXTO", cifrado });
                }
                if (esGrupo) {
                    if (!cifrado) {
                        throw new Error("No se puede cifrar: destinatario sin clave E2E registrada");
                    }
                    await api("POST", "api/mensajes/enviar", { chatId, contenido: payload, tipo: "TEXTO", cifrado });
                }
            }
            input.value = "";
            actualizarBotonEnviar();
            await cargarMensajes(chatId);
            cargarChats();
        } catch (e) {
            mostrarToast("No se pudo enviar: " + e.message, "error");
        }
    }

    async function subirAdjunto(file) {
        const base64 = await new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(reader.result.split(",")[1]);
            reader.onerror = reject;
            reader.readAsDataURL(file);
        });
        return api("POST", "api/mensajes/upload", {
            nombreArchivo: file.name,
            mimeType: file.type,
            contenidoBase64: base64
        });
    }

    async function abrirAdjunto(adjunto) {
        const res = await fetch(
            `api/mensajes/adjunto/${adjunto.urlArchivo}`,
            {
                headers: {
                    Authorization: state.token
                }
            }
        );
        if (!res.ok) {
            throw new Error("No se pudo descargar el archivo");
        }
        const blob = await res.blob();

        const nombre = adjunto.nombreArchivo.toLowerCase();

        const esImagen =
            nombre.endsWith(".jpg") ||
            nombre.endsWith(".jpeg") ||
            nombre.endsWith(".png") ||
            nombre.endsWith(".webp") ||
            nombre.endsWith(".gif");

        const esVideo =
            nombre.endsWith(".mp4") ||
            nombre.endsWith(".mov") ||
            nombre.endsWith(".avi") ||
            nombre.endsWith(".mkv");

        const url = URL.createObjectURL(blob);

        if (esImagen || esVideo) {

            window.open(url, "_blank");

        } else {

            const a = document.createElement("a");
            a.href = url;
            a.download = adjunto.nombreArchivo;
            a.click();

        }
    }

    function iniciarEdicion(m) {
        state.editando = m;
        $("#barra-edicion").hidden = false;
        const input = $("#input-mensaje");
        input.value = m.contenido;
        actualizarBotonEnviar();
        renderMensajes();
        $("#zona-mensajes").scrollTop = $("#zona-mensajes").scrollHeight;
        input.focus();
    }

    function cancelarEdicion() {
        if (state.editando) {
            $("#input-mensaje").value = "";
            state.editando = null;
            renderMensajes();
        }
        $("#barra-edicion").hidden = true;
        actualizarBotonEnviar();
    }

    async function eliminarParaMi(id) {
        try {
            await api("PUT", `api/mensajes/${id}/eliminar-para-mi`);
            await cargarMensajes(state.chatActual.id, { mantenerScroll: true });
            cargarChats();
        } catch (e) { mostrarToast(e.message, "error"); }
    }

    async function eliminarParaTodos(id) {
        try {
            await api("PUT", `api/mensajes/${id}/eliminar-para-todos`);
            await cargarMensajes(state.chatActual.id, { mantenerScroll: true });
            cargarChats();
        } catch (e) { mostrarToast(e.message, "error"); }
    }

    async function reaccionar(mensajeId, emoji) {
        try {
            await api("POST", "api/reacciones", { mensajeId, emoji });
            await cargarMensajes(state.chatActual.id, { mantenerScroll: true });
        } catch (e) { mostrarToast(e.message, "error"); }
    }

    function actualizarBotonEnviar() {
        const tieneTexto = $("#input-mensaje").value.trim().length > 0;
        $("#btn-enviar").classList.toggle("activo", tieneTexto);
        $(".btn-camara").hidden = tieneTexto;
    }

    // ───────── Emoji picker ─────────
    function initEmojiPicker() {
        const picker = $("#emoji-picker");
        for (const e of QUICK_EMOJIS) {
            const b = document.createElement("button");
            b.textContent = e;
            b.addEventListener("click", () => {
                const input = $("#input-mensaje");
                input.value += e;
                picker.hidden = true;
                actualizarBotonEnviar();
                input.focus();
            });
            picker.appendChild(b);
        }
    }

    // ───────── Personas (filas reutilizables) ─────────
    function filaPersona(u, extra) {
        const li = document.createElement("li");
        li.className = "persona";
        li.appendChild(crearAvatar(u.nombre, 44, u.initials || u.nombre.slice(0, 2).toUpperCase()));
        const info = document.createElement("div");
        info.className = "persona-info";
        const n = document.createElement("span");
        n.className = "persona-nombre";
        n.textContent = u.nombre;
        const e = document.createElement("span");
        e.className = "persona-email";
        e.textContent = u.email || "";
        info.append(n, e);
        li.appendChild(info);
        if (extra) li.appendChild(extra);
        return li;
    }

    function notaLista(ul, texto) {
        ul.innerHTML = "";
        const li = document.createElement("li");
        li.className = "lista-nota";
        li.textContent = texto;
        ul.appendChild(li);
    }

    async function cargarUsuarios() {
        state.usuarios = await api("GET", "api/usuarios/listar");
        return state.usuarios.filter(u => u.id !== state.usuario.id);
    }

    // ───────── Nuevo chat privado ─────────
    async function abrirModalPrivado() {
        cerrarOverlays();
        $("#privado-error").textContent = "";
        $("#buscar-privado").value = "";
        const ul = $("#lista-privado");
        notaLista(ul, "Cargando usuarios…");
        $("#modal-privado").hidden = false;

        try {
            await cargarUsuarios();
        } catch {
            notaLista(ul, "No se pudieron cargar los usuarios");
            return;
        }
        renderListaPrivado();
    }

    function renderListaPrivado() {
        const filtro = $("#buscar-privado").value.trim().toLowerCase();
        const ul = $("#lista-privado");
        ul.innerHTML = "";

        const contactos = state.usuarios.filter(u =>
            u.id !== state.usuario.id &&
            u.nombre.toLowerCase().includes(filtro));

        if (!contactos.length) { notaLista(ul, "No hay usuarios"); return; }

        for (const u of contactos) {
            const li = filaPersona(u);
            li.addEventListener("click", () => crearPrivado(u));
            ul.appendChild(li);
        }
    }

    async function crearPrivado(u) {
        try {
            const chat = await api("POST", "api/chats", {
                nombre: u.nombre,
                tipo: "PRIVADO",
                usuarios: [state.usuario.id, u.id]
            });
            cerrarOverlays();
            await cargarChats();
            const creado = state.chats.find(c => c.id === chat.id) || { id: chat.id, nombre: u.nombre };
            abrirChat(creado);
        } catch (e) {
            $("#privado-error").textContent = e.message;
        }
    }

    // ───────── Nuevo grupo ─────────
    async function abrirModalGrupo() {
        cerrarOverlays();
        state.seleccionGrupo = new Set();
        $("#grupo-error").textContent = "";
        $("#grupo-nombre").value = "";
        $("#buscar-grupo").value = "";
        const ul = $("#lista-grupo");
        notaLista(ul, "Cargando usuarios…");
        $("#modal-grupo").hidden = false;

        try {
            await cargarUsuarios();
        } catch {
            notaLista(ul, "No se pudieron cargar los usuarios");
            return;
        }
        renderListaGrupo();
    }

    function renderListaGrupo() {
        const filtro = $("#buscar-grupo").value.trim().toLowerCase();
        const ul = $("#lista-grupo");
        ul.innerHTML = "";

        const contactos = state.usuarios.filter(u =>
            u.id !== state.usuario.id &&
            u.nombre.toLowerCase().includes(filtro));

        if (!contactos.length) { notaLista(ul, "No hay usuarios"); return; }

        for (const u of contactos) {
            const check = document.createElement("ion-icon");
            check.className = "check";
            check.setAttribute("name",
                state.seleccionGrupo.has(u.id) ? "checkmark-circle" : "ellipse-outline");
            const li = filaPersona(u, check);
            li.addEventListener("click", () => {
                if (state.seleccionGrupo.has(u.id)) state.seleccionGrupo.delete(u.id);
                else state.seleccionGrupo.add(u.id);
                renderListaGrupo();
            });
            ul.appendChild(li);
        }
    }

    async function crearGrupo() {
        const nombre = $("#grupo-nombre").value.trim();
        const errorEl = $("#grupo-error");
        errorEl.textContent = "";

        if (!nombre) { errorEl.textContent = "Ingrese un nombre para el grupo"; return; }
        if (state.seleccionGrupo.size < 1) { errorEl.textContent = "Seleccione al menos un usuario"; return; }

        try {
            const chat = await api("POST", "api/chats", {
                nombre,
                tipo: "GRUPO",
                usuarios: [state.usuario.id, ...state.seleccionGrupo]
            });
            // Cargar miembros recién creados para distribuir la clave
            const todosIds = [state.usuario.id, ...state.seleccionGrupo];
            const miembrosParaClave = todosIds.map(id => ({ id }));
            await distribuirClaveGrupo(chat.id, miembrosParaClave).catch(e =>
                console.warn("distribuirClaveGrupo (crear):", e)
            );
            cerrarOverlays();
            await cargarChats();
            const creado = state.chats.find(c => c.id === chat.id) || { id: chat.id, nombre };
            abrirChat(creado);
        } catch (e) {
            errorEl.textContent = e.message;
        }
    }

    // ───────── Perfil ─────────
    function abrirPerfil() {
        const u = state.usuario;
        setAvatar($("#perfil-avatar"), u.nombre, u.initials);
        $("#perfil-nombre").textContent = u.nombre;
        $("#perfil-email").textContent = u.email;
        const rol = $("#perfil-rol");
        rol.textContent = u.rol;
        rol.className = "tag " + (u.rol === "ADMIN" ? "admin" : u.rol === "MANAGER" ? "manager" : "");
        $("#info-usuario").textContent = u.nombre;
        $("#info-email").textContent = u.email;
        const estado = $("#info-estado");
        estado.textContent = state.online ? "En línea" : "Desconectado";
        estado.classList.toggle("online", state.online);
        $("#modal-perfil").hidden = false;
    }

    // ───────── Info del grupo ─────────
    async function abrirGrupoInfo() {
        if (!state.chatActual) return;
        const nombre = state.chatActual.nombre || "Chat";
        setAvatar($("#ginfo-avatar"), nombre, nombre.slice(0, 2).toUpperCase());
        $("#ginfo-nombre").textContent = nombre;
        $("#ginfo-error").textContent = "";
        $("#ginfo-editar").hidden = true;

        const esGrupo = state.chatActual.tipo === "GRUPO";
        $("#btn-renombrar").hidden            = !esGrupo;
        $("#btn-agregar-participante").hidden  = !esGrupo;
        $("#btn-salir-grupo").hidden           = !esGrupo;

        const ul = $("#ginfo-miembros");
        notaLista(ul, "Cargando participantes…");
        $("#modal-grupo-info").hidden = false;

        try {
            state.miembros = await api("GET", `api/chats/${state.chatActual.id}/miembros`);
        } catch (e) {
            state.miembros = [];
            notaLista(ul, e.message || "No se pudieron cargar los participantes");
            return;
        }
        renderMiembros();
    }

    function renderMiembros() {
        const ul = $("#ginfo-miembros");
        ul.innerHTML = "";

        if (!state.miembros.length) {
            notaLista(ul, "Sin participantes");
            return;
        }

        const esGrupo = state.chatActual?.tipo === "GRUPO";
        for (const m of state.miembros) {
            let extra = null;
            if (esGrupo && m.id !== state.usuario.id) {
                extra = document.createElement("button");
                extra.className = "btn-icono quitar";
                extra.title = "Eliminar del grupo";
                extra.innerHTML = `<ion-icon name="remove-circle-outline"></ion-icon>`;
                extra.addEventListener("click", async (ev) => {
                    ev.stopPropagation();
                    if (!confirm(`¿Eliminar a ${m.nombre} del grupo?`)) return;
                    try {
                        await api("POST", "api/chats/eliminar-miembro", {
                            chatId: state.chatActual.id,
                            usuarioId: m.id
                        });
                        state.miembros = state.miembros.filter(x => x.id !== m.id);
                        // Rotar clave de grupo
                        delete state.groupKeys[state.chatActual.id];
                        const miembrosRestantes = state.miembros.filter(x => x.id !== m.id);
                        await distribuirClaveGrupo(state.chatActual.id, miembrosRestantes)
                            .catch(e => console.warn("distribuirClaveGrupo (eliminar):", e));
                        renderMiembros();
                    } catch (e) {
                        $("#ginfo-error").textContent = e.message;
                    }
                });
            }
            const li = filaPersona(m, extra);
            if (m.rol && m.rol !== "MIEMBRO") {
                const tag = document.createElement("span");
                tag.className = "tag admin";
                tag.textContent = m.rol;
                li.insertBefore(tag, extra);
            }
            ul.appendChild(li);
        }
    }

    async function abrirModalAgregar() {
        $("#agregar-error").textContent = "";
        $("#buscar-agregar").value = "";
        const ul = $("#lista-agregar");
        notaLista(ul, "Cargando usuarios…");
        $("#modal-agregar").hidden = false;

        try {
            await cargarUsuarios();
        } catch {
            notaLista(ul, "No se pudieron cargar los usuarios");
            return;
        }
        renderListaAgregar();
    }

    function renderListaAgregar() {
        const filtro = $("#buscar-agregar").value.trim().toLowerCase();
        const ul = $("#lista-agregar");
        ul.innerHTML = "";

        const ids = new Set(state.miembros.map(m => m.id));
        const disponibles = state.usuarios.filter(u =>
            !ids.has(u.id) && u.nombre.toLowerCase().includes(filtro));

        if (!disponibles.length) { notaLista(ul, "No hay usuarios disponibles"); return; }

        for (const u of disponibles) {
            const add = document.createElement("ion-icon");
            add.className = "check";
            add.setAttribute("name", "person-add-outline");
            const li = filaPersona(u, add);
            li.addEventListener("click", async () => {
                try {
                    await api("POST", "api/chats/agregar-miembro", {
                        chatId: state.chatActual.id,
                        usuarioId: u.id
                    });
                    // Rotar clave de grupo (antes de push para que [...state.miembros, {id}] no duplique)
                    delete state.groupKeys[state.chatActual.id]; // invalidar caché
                    const miembrosActualizados = [...state.miembros, { id: u.id }];
                    await distribuirClaveGrupo(state.chatActual.id, miembrosActualizados)
                        .catch(e => console.warn("distribuirClaveGrupo (agregar):", e));
                    state.miembros.push({ id: u.id, nombre: u.nombre, email: u.email, rol: "MIEMBRO" });
                    renderMiembros();
                    renderListaAgregar();
                } catch (e) {
                    $("#agregar-error").textContent = e.message;
                }
            });
            ul.appendChild(li);
        }
    }

    async function guardarNombreGrupo() {
        const nuevo = $("#ginfo-nuevo-nombre").value.trim();
        const errorEl = $("#ginfo-error");
        errorEl.textContent = "";
        if (!nuevo) { errorEl.textContent = "El nombre no puede estar vacío"; return; }

        try {
            await api("PATCH", `api/chats/${state.chatActual.id}`, { nombre: nuevo });
            state.chatActual.nombre = nuevo;
            $("#ginfo-nombre").textContent = nuevo;
            setAvatar($("#ginfo-avatar"), nuevo, nuevo.slice(0, 2).toUpperCase());
            $("#chat-nombre").textContent = nuevo;
            setAvatar($("#chat-avatar"), nuevo, nuevo.slice(0, 2).toUpperCase());
            $("#ginfo-editar").hidden = true;
            cargarChats();
        } catch (e) {
            errorEl.textContent = e.message || "No se pudo cambiar el nombre";
        }
    }

    async function salirDelGrupo() {
        if (!confirm("¿Salir del grupo?")) return;
        try {
            await api("POST", "api/chats/eliminar-miembro", {
                chatId: state.chatActual.id,
                usuarioId: state.usuario.id
            });
            cerrarOverlays();
            cerrarChat();
            cargarChats();
        } catch (e) {
            $("#ginfo-error").textContent = e.message;
        }
    }

    // ───────── Reenviar ─────────
    function abrirModalReenviar(mensajeId) {
        state.reenviarId = mensajeId;
        $("#reenviar-error").textContent = "";
        const ul = $("#lista-reenviar");
        ul.innerHTML = "";

        for (const chat of state.chats) {
            const nombre = chat.nombre || "Chat";
            const li = filaPersona({ nombre, email: chat.lastMsg || "" , initials: nombre.slice(0,2).toUpperCase()});
            li.addEventListener("click", async () => {
                try {
                    // 1. Encontrar el mensaje original en memoria
                    const original = state.mensajes.find(m => m.id === state.reenviarId);
                    if (!original) throw new Error("Mensaje no encontrado en pantalla");

                    // 2. Obtener el texto plano (ya descifrado en m._plain, o plano si no era cifrado)
                    const textoPlano = original.cifrado ? original._plain : original.contenido;
                    if (textoPlano == null) throw new Error("No se puede descifrar el mensaje original");

                    const chatDestino = chat; // 'chat' viene del closure del for
                    const esGrupoDestino = chatDestino.tipo === "GRUPO";

                    // 3. Cifrar para el chat destino
                    let payload = textoPlano;
                    let cifrado = false;
                    if (esGrupoDestino) {
                        const gk = await getGroupKey(chatDestino.id);
                        if (gk) { payload = await Crypto.encryptGroup(textoPlano, gk); cifrado = true; }
                    } else {
                        const ms = await api("GET", `api/chats/${chatDestino.id}/miembros`);
                        const otro = ms.find(m => m.id !== state.usuario.id);
                        if (otro) {
                            const pub = await getPubKey(otro.id);
                            if (pub) { payload = await Crypto.encryptDirect(textoPlano, pub); cifrado = true; }
                        }
                    }

                    // 4. Enviar como mensaje normal (el servidor lo ve como un mensaje nuevo)
                    if (!cifrado) {
                        throw new Error("No se puede cifrar: destinatario sin clave E2E registrada");
                    }
                    await api("POST", "api/mensajes/enviar", {
                        chatId: chatDestino.id,
                        contenido: payload,
                        tipo: original.tipo || "TEXTO",
                        cifrado
                    });

                    $("#modal-reenviar").hidden = true;
                    if (state.chatActual?.id === chatDestino.id) await cargarMensajes(chatDestino.id);
                    cargarChats();
                } catch (e) {
                    $("#reenviar-error").textContent = e.message;
                }
            });
            ul.appendChild(li);
        }
        $("#modal-reenviar").hidden = false;
    }

    // ───────── Overlays ─────────
    function cerrarOverlays() {
        document.querySelectorAll(".overlay").forEach(o => o.hidden = true);
    }

    // ───────── E2E helpers ─────────
    async function getPubKey(userId) {
        if (state.pubKeyCache[userId]) return state.pubKeyCache[userId];
        try {
            const res = await api("GET", `api/usuarios/${userId}/clave-publica`);
            const key = typeof res === "string" ? JSON.parse(res).clavePub : res.clavePub;
            if (key) state.pubKeyCache[userId] = key;
            return key || null;
        } catch { return null; }
    }

    async function getGroupKey(chatId) {
        if (state.groupKeys[chatId]) return state.groupKeys[chatId];
        try {
            const res = await api("GET", `api/chats/${chatId}/clave-grupo`);
            const data = typeof res === "string" ? JSON.parse(res) : res;
            if (!data || !data.claveEnvuelta) return null;
            const distribPub = await getPubKey(data.distribuidorId);
            if (!distribPub) return null;
            const ck = await Crypto.unwrapGroupKey(data.claveEnvuelta, distribPub);
            if (ck) state.groupKeys[chatId] = ck;
            return ck;
        } catch { return null; }
    }

    async function distribuirClaveGrupo(chatId, miembros) {
        const gk = await Crypto.generateGroupKey();
        state.groupKeys[chatId] = gk;

        const version = Date.now(); // número único creciente, suficiente para el proyecto
        const envueltas = [];
        for (const m of miembros) {
            const pub = await getPubKey(m.id);
            if (!pub) continue;
            const claveEnvuelta = await Crypto.wrapGroupKey(gk, pub);
            envueltas.push({ miembroId: m.id, claveEnvuelta });
        }

        await api("PUT", `api/chats/${chatId}/clave-grupo`, {
            version,
            distribuidorId: state.usuario.id,
            envueltas
        });
    }

    // ───────── Arranque ─────────
    function entrarApp() {
        $("#vista-login").hidden = true;
        $("#vista-app").hidden = false;

        setAvatar($("#mi-avatar"), state.usuario.nombre, state.usuario.initials);

        if (state.usuario.rol === "ADMIN") {
            $(".search-pill").hidden = true;
            $(".section-label").hidden = true;
            $("#lista-chats").hidden = true;
            $("#chats-vacio").hidden = true;
            $("#btn-nuevo-chat").hidden = true;
            $("#panel-vacio").hidden = true;
            $("#panel-chat").hidden = true;
            $("#panel-admin").hidden = false;

            cargarUsuariosAdmin();
            conectarWs();
            return;
        }

        $("#panel-admin").hidden = true;
        $("#panel-chat").hidden = true;
        $("#panel-vacio").hidden = false;

        if (state.usuario.rol !== "ADMIN") {
            Crypto.ensureKeyPair(api).catch(e => console.warn("ensureKeyPair:", e));
        }
        cargarChats();
        conectarWs();
    }

    //admin
    async function cargarUsuariosAdmin() {
        try {
            const usuarios = await api("GET", "api/usuarios/listar");

            const sinAdmin = usuarios.filter(u => u.rol !== "ADMIN");
            $("#metric-total").textContent     = sinAdmin.length;
            $("#metric-online").textContent    = sinAdmin.filter(u => u.estado === "ONLINE").length;
            $("#metric-bloqueados").textContent = sinAdmin.filter(u => u.bloqueado).length;

            const ul = $("#lista-usuarios-admin");
            ul.innerHTML = "";

            for (const u of usuarios) {
                // No mostrar al administrador
                if (u.rol === "ADMIN") continue;

                const accion = document.createElement("button");
                accion.className = "btn-icono";

                accion.innerHTML = u.bloqueado
                    ? '<ion-icon name="lock-open-outline"></ion-icon>'
                    : '<ion-icon name="ban-outline"></ion-icon>';

                accion.title = u.bloqueado ? "Desbloquear" : "Bloquear";

                accion.addEventListener("click", () => abrirModalBloqueo(u));

                const li = filaPersona({
                    ...u,
                    estadoTexto: u.estado === "ONLINE"
                        ? "🟢 En línea"
                        : "⚪ Desconectado"
                }, accion);

                ul.appendChild(li);
            }
        } catch (e) {
            mostrarToast(e.message, "error");
        }
    }

    let usuarioSeleccionado = null;
    function abrirModalBloqueo(usuario) {
        const modal = document.getElementById("modal-bloqueo");
        const texto = document.getElementById("texto-modal-bloqueo");
        const btnConfirmar = document.getElementById("btn-confirmar-bloqueo");
        const btnCancelar = document.getElementById("btn-cancelar");

        const accion = usuario.bloqueado ? "desbloquear" : "bloquear";

        texto.innerText = `¿Deseás ${accion} a ${usuario.nombre}?`;

        // CONFIRMAR
        btnConfirmar.onclick = async () => {
            try {
                if (usuario.bloqueado) {
                    await api("PUT", `api/usuarios/desbloquear/${usuario.id}`);
                } else {
                    await api("PUT", `api/usuarios/bloquear/${usuario.id}`);
                }

                cerrarModalBloqueo();
                cargarUsuariosAdmin();

            } catch (e) {
                mostrarToast(e.message, "error");
            }
        };

        // ❌ CANCELAR (ACÁ ESTABA EL PROBLEMA)
        btnCancelar.onclick = cerrarModalBloqueo;

        modal.style.display = "flex";
    }
    function cerrarModalBloqueo() {
        document.getElementById("modal-bloqueo").style.display = "none";
        usuarioSeleccionado = null;
    }
    // ───────── Validación de registro (como utils/validators.ts) ─────────
    function validarRegistro(nombre, email, password, confirmar) {
        if (!nombre) return "Ingresá un nombre de usuario";
        if (password.length < 8) return "La contraseña debe tener al menos 8 caracteres";
        if (!/[A-Z]/.test(password)) return "La contraseña debe incluir una mayúscula";
        if (!/[0-9]/.test(password)) return "La contraseña debe incluir un número";
        if (!/[^A-Za-z0-9]/.test(password)) return "La contraseña debe incluir un símbolo";
        if (password !== confirmar) return "Las contraseñas no coinciden";
        return null;
    }

    // ───────── Eventos ─────────
    function initEventos() {
        // tabs login/registro
        $("#tab-login").addEventListener("click", () => {
            $("#tab-login").classList.add("activo");
            $("#tab-registro").classList.remove("activo");
            $("#form-login").hidden = false;
            $("#form-registro").hidden = true;
            $("#auth-error").textContent = "";
        });
        $("#tab-registro").addEventListener("click", () => {
            $("#tab-registro").classList.add("activo");
            $("#tab-login").classList.remove("activo");
            $("#form-registro").hidden = false;
            $("#form-login").hidden = true;
            $("#auth-error").textContent = "";
        });

        $("#form-login").addEventListener("submit", async (ev) => {
            ev.preventDefault();
            $("#auth-error").textContent = "";

            try {
                await login(
                    $("#login-email").value.trim(),
                    $("#login-password").value
                );
            } catch (e) {

                const msg = e?.message;

                if (msg === "Usuario bloqueado") {
                    $("#auth-error").textContent = "Usuario bloqueado";
                } 
                else if (msg === "Credenciales inválidas") {
                    $("#auth-error").textContent = "Credenciales incorrectas";
                } 
                else {
                    $("#auth-error").textContent = "Error al iniciar sesión";
                }
            }
        });

        $("#form-registro").addEventListener("submit", async (ev) => {
            ev.preventDefault();
            const errorEl = $("#auth-error");
            errorEl.textContent = "";

            const nombre = $("#reg-nombre").value.trim();
            const email = $("#reg-email").value.trim();
            const password = $("#reg-password").value;
            const confirmar = $("#reg-confirmar").value;

            const invalido = validarRegistro(nombre, email, password, confirmar);
            if (invalido) { errorEl.textContent = invalido; return; }

            try {
                await api("POST", "api/usuarios/registro", { nombre, email, password, rol: "USER" });
                $("#form-registro").reset();
                $("#login-email").value = email;
                $("#tab-login").click();
                $("#login-password").focus();
            } catch (e) {
                errorEl.textContent = e.message || "Error al registrarse";
            }
        });

        // sidebar
        $("#btn-perfil").addEventListener("click", abrirPerfil);
        $("#btn-nuevo-chat").addEventListener("click", () => {
            cerrarOverlays();
            $("#modal-nuevo-chat").hidden = false;
        });
        $("#buscar-chat").addEventListener("input", renderChats);

        // chat
        $("#btn-volver").addEventListener("click", cerrarChat);
        $("#btn-grupo-info").addEventListener("click", abrirGrupoInfo);
        $("#chat-header-centro").addEventListener("click", abrirGrupoInfo);

        // composer
        $("#btn-enviar").addEventListener("click", enviarMensaje);
        $("#input-mensaje").addEventListener("input", (ev) => {
            actualizarBotonEnviar(ev);
            enviarTyping();
        });
        $("#input-mensaje").addEventListener("keydown", (ev) => {
            if (ev.key === "Enter" && !ev.shiftKey) { ev.preventDefault(); enviarMensaje(); }
            if (ev.key === "Escape") { cancelarEdicion(); $("#emoji-picker").hidden = true; }
        });
        $("#cancelar-edicion").addEventListener("click", cancelarEdicion);
        $("#btn-emoji").addEventListener("click", (ev) => {
            ev.stopPropagation();
            $("#emoji-picker").hidden = !$("#emoji-picker").hidden;
        });
        $("#btn-adjunto").addEventListener("click", () => {
            $("#input-adjunto").click();
        });
        $("#input-adjunto").addEventListener("change", async (ev) => {
            const file = ev.target.files[0];
            ev.target.value = "";
            if (!file || !state.chatActual) return;
            try {
                const archivo = await subirAdjunto(file);
                await api("POST", "api/mensajes/enviar", {
                    chatId: state.chatActual.id,
                    contenido: archivo.urlArchivo,
                    tipo: file.type.startsWith("image/") ? "IMAGEN"
                        : file.type.startsWith("video/") ? "VIDEO"
                        : file.type.startsWith("audio/") ? "AUDIO"
                        : "ARCHIVO",
                    nombreArchivo: archivo.nombreArchivo,
                    tamanoArchivo: archivo.tamanoArchivo,
                    mimeType: file.type
                });
                await cargarMensajes(state.chatActual.id);
                cargarChats();
            } catch (e) {
                mostrarToast("Error enviando archivo: " + e.message, "error");
            }
        });
        document.addEventListener("click", (ev) => {
            if (!ev.target.closest("#emoji-picker") && !ev.target.closest("#btn-emoji")) {
                $("#emoji-picker").hidden = true;
            }
        });

        // menú de mensaje: cerrar al tocar el fondo
        $("#menu-mensaje").addEventListener("click", (ev) => {
            if (ev.target === $("#menu-mensaje")) cerrarMenuMensaje();
        });

        // overlays: botones cerrar y click en el fondo
        document.querySelectorAll(".cerrar").forEach(btn =>
            btn.addEventListener("click", () => $("#" + btn.dataset.overlay).hidden = true));
        document.querySelectorAll(".overlay").forEach(ov =>
            ov.addEventListener("click", (ev) => {
                if (ev.target === ov) ov.hidden = true;
            }));

        // nuevo chat
        $("#opcion-privado").addEventListener("click", abrirModalPrivado);
        $("#opcion-grupo").addEventListener("click", abrirModalGrupo);
        $("#buscar-privado").addEventListener("input", renderListaPrivado);
        $("#buscar-grupo").addEventListener("input", renderListaGrupo);
        $("#btn-crear-grupo").addEventListener("click", crearGrupo);

        // perfil
        $("#btn-logout").addEventListener("click", () => {
            if (confirm("¿Seguro que querés cerrar sesión?")) logout();
        });

        // grupo info
        $("#btn-agregar-participante").addEventListener("click", abrirModalAgregar);
        $("#buscar-agregar").addEventListener("input", renderListaAgregar);
        $("#btn-renombrar").addEventListener("click", () => {
            $("#ginfo-nuevo-nombre").value = state.chatActual?.nombre || "";
            $("#ginfo-editar").hidden = false;
            $("#ginfo-nuevo-nombre").focus();
        });
        $("#btn-guardar-nombre").addEventListener("click", guardarNombreGrupo);
        $("#btn-salir-grupo").addEventListener("click", salirDelGrupo);
    }

    // ───────── Init ─────────
    initEventos();
    initEmojiPicker();
    if (state.token && state.usuario) {
        api("GET", "api/chats")
            .then(() => entrarApp())
            .catch(() => cerrarSesionLocal());
    }
})();
