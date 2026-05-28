package com.chat.websocket;

import com.chat.security.TokenService;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/ws/chat")
public class ChatWebSocket {

    @Inject
    private TokenService tokenService;

    private static final Map<Integer, Session> sessions =
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

            sessions.put(
                userId.intValue(),
                session
            );

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
    public void onMessage(String message) {

        if ("ping".equals(message)) {

            System.out.println("PING");
        }
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