package com.chat.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/ws/chat")
public class ChatWebSocket {

    private static final Set<Session> sessions =
            new CopyOnWriteArraySet<>();

    @OnOpen
    public void onOpen(Session session) {

        sessions.add(session);

        System.out.println(
                "WS conectado: " + session.getId()
        );
    }

    @OnClose
    public void onClose(Session session) {

        sessions.remove(session);

        System.out.println(
                "WS cerrado: " + session.getId()
        );
    }

    @OnError
    public void onError(
            Session session,
            Throwable throwable
    ) {

        sessions.remove(session);

        System.out.println(
                "WS error: " + throwable
        );

        throwable.printStackTrace();
    }

    public static void broadcast(String mensaje) {

        for (Session session : sessions) {

            if (!session.isOpen()) {
                sessions.remove(session);
                continue;
            }

            try {

                session.getAsyncRemote()
                        .sendText(mensaje);

            } catch (Exception e) {

                System.out.println(
                        "Error enviando a session "
                        + session.getId()
                );

                e.printStackTrace();

                try {
                    session.close();
                } catch (IOException ignored) {}

                sessions.remove(session);
            }
        }
    }
}