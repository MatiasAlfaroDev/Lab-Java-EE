package com.chat.controller;

import com.chat.service.ChatService;
import com.chat.datatype.CrearChatRequest;
import com.chat.model.Chat;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/chats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChatController {

    @Inject
    private ChatService chatService;

    @POST
    public Response crearChat(CrearChatRequest request) {
        try {
            // Validación básica
            if (request.getNombre() == null || request.getNombre().isBlank() ||
                request.getTipo() == null || request.getTipo().isBlank() ||
                request.getUsuarios() == null || request.getUsuarios().isEmpty()) {

                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Datos inválidos para crear el chat")
                        .build();
            }

            chatService.crearChat(
                request.getNombre(),
                request.getTipo(),
                request.getUsuarios()
            );

            return Response.ok("Chat creado correctamente").build();

        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @GET
    public Response obtenerChats() {
        List<Chat> chats = chatService.obtenerChats();
        return Response.ok(chats).build();
    }
}