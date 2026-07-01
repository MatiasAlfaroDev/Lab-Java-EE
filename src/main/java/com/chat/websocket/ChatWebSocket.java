package com.chat.websocket;

import com.chat.dao.ChatDAO;
import com.chat.model.MiembroChat;
import com.chat.security.TokenService;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ServerEndpoint("/ws/chat")
public class ChatWebSocket {

    @Inject
    private TokenService tokenService;

    @Inject
    private ChatDAO chatDAO;

    private static final Map<Integer, Session> sessions =
        new ConcurrentHashMap<>();

    // reverse map: session.getId() → userId
    private static final Map<String, Integer> sessionToUser =
        new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {

        try {

            String token =
                session
                    .getRequestParameterMap()
                    .get("token")
                    .get(0);

            Long userId = new TokenService().validarToken(token);

            
            Session anterior =
                sessions.put(
                    userId.intValue(),
                    session
                );

            sessionToUser.put(session.getId(), userId.intValue());

            if (
                anterior != null &&
                anterior.isOpen()
            ) {
                anterior.close();
            }

            System.out.println(
                "WS conectado usuario: "
                + userId
            );

        } catch (Exception e) {

            try {
                session.close();
            } catch (IOException ignored) {}
        }
    }

    @OnClose
    public void onClose(Session session) {

        sessions.values().remove(session);
        sessionToUser.remove(session.getId());

        System.out.println(
            "WS cerrado: " + session.getId()
        );
    }

    @OnError
    public void onError(
        Session session,
        Throwable throwable
    ) {

        sessions.values().remove(session);

        System.out.println(
            "WS error: " + throwable
        );

        throwable.printStackTrace();
    }

    @OnMessage
    public void onMessage(String message, Session session) {

        if ("ping".equals(message)) return;

        try {
            // parse typing events without a full JSON library — find "tipo" field value
            if (!message.contains("\"TYPING\"") && !message.contains("\"STOP_TYPING\"")) return;

            String tipo    = extractString(message, "tipo");
            int    chatId  = Integer.parseInt(extractString(message, "chatId"));
            int    uid     = sessionToUser.getOrDefault(session.getId(), -1);
            String nombre  = extractString(message, "nombre");

            if (uid < 0 || chatId <= 0) return;

            List<MiembroChat> miembros = chatDAO.obtenerMiembros(chatId);
            List<Integer> destinatarios = miembros.stream()
                .map(m -> m.getUsuario().getId())
                .filter(id -> id != uid)
                .collect(Collectors.toList());

            String evento = "{\"tipo\":\"" + tipo + "\","
                + "\"chatId\":" + chatId + ","
                + "\"usuarioId\":" + uid + ","
                + "\"nombre\":\"" + nombre.replace("\"", "") + "\"}";

            sendToUsers(destinatarios, evento);

        } catch (Exception e) {
            System.out.println("onMessage typing error: " + e.getMessage());
        }
    }

    /** Minimal JSON string extractor — avoids a JSON library dependency. */
    private static String extractString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return "";
        String rest = json.substring(colon + 1).trim();
        if (rest.startsWith("\"")) {
            int end = rest.indexOf('"', 1);
            return end > 0 ? rest.substring(1, end) : "";
        }
        // numeric — strip to next , or }
        int end = rest.indexOf(',');
        if (end < 0) end = rest.indexOf('}');
        return end > 0 ? rest.substring(0, end).trim() : rest.trim();
    }

    public static void broadcastAll(String mensaje) {
        sendToUsers(new ArrayList<>(sessions.keySet()), mensaje);
    }

    public static void sendToUsers(
        List<Integer> usuarios,
        String mensaje
    ) {

        for (Integer userId : usuarios) {

            Session session =
                sessions.get(userId);

            if (
                session != null &&
                session.isOpen()
            ) {

                try {

                    session
                        .getAsyncRemote()
                        .sendText(mensaje);

                } catch (Exception e) {

                    try {
                        session.close();
                    } catch (IOException ignored) {}

                    sessions.remove(userId);
                }
            }
        }
    }
}