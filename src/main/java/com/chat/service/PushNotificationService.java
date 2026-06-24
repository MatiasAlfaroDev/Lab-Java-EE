package com.chat.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class PushNotificationService {

    public void enviarPush(
        String expoToken,
        String titulo,
        String mensaje
    ) {

        System.out.println(
            "ENVIANDO PUSH A: " +
            expoToken
        );
        String json = String.format(
            """
            {
              "to":"%s",
              "title":"%s",
              "body":"%s"
            }
            """,
            expoToken,
            titulo.replace("\"", "'"),
            mensaje.replace("\"", "'")
        );

        Client client = ClientBuilder.newClient();

        try {

            Response response =
                client.target(
                    "https://exp.host/--/api/v2/push/send"
                )
                .request(MediaType.APPLICATION_JSON)
                .post(
                    Entity.entity(
                        json,
                        MediaType.APPLICATION_JSON
                    )
                );

            String body =
                response.readEntity(String.class);

            System.out.println(
                "PUSH STATUS: "
                + response.getStatus()
            );

            System.out.println(
                "PUSH RESPONSE: "
                + body
            );

        } finally {

            client.close();

        }
    }
}