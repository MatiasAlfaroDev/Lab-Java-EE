package com.chat.controller;

import com.chat.service.ChatService;
import com.chat.datatype.CrearChatRequest;
import com.chat.datatype.AgregarMiembroRequest;
import com.chat.datatype.ChatDTO;
import com.chat.datatype.EliminarMiembroRequest;
import com.chat.enums.TipoChat;
import com.chat.model.Chat;
import com.chat.model.MiembroChat;
import com.chat.security.TokenService;

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

    @Inject
    private TokenService tokenService;

    @POST
    public Response crearChat(
        CrearChatRequest request,
        @HeaderParam("Authorization") String token
    ) {
        try {
            // VALIDAR TOKEN
            if (token == null || token.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Falta token")
                        .build();
            }

            Long userId = tokenService.validarToken(token);

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
                request.getUsuarios(),
                userId
            );

            return Response.ok("Chat creado correctamente").build();

        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Token inválido")
                    .build();
            }
    }

    @GET
    public Response obtenerChats(@HeaderParam("Authorization") String token) {

        try {
            if (token == null || token.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Falta token")
                        .build();
            }

            Long userId = tokenService.validarToken(token);

            List<Chat> chats = chatService.obtenerChats();

            List<ChatDTO> resultado = chats.stream()
                .map(chat -> new ChatDTO(
                    chat.getChatId(),
                    obtenerNombre(chat, userId.intValue())
                ))
                .toList();

            return Response.ok(resultado).build();

        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Token inválido")
                    .build();
        }
    }

    @POST
    @Path("/agregar-miembro")
    public Response agregarMiembro(
        AgregarMiembroRequest request,
        @HeaderParam("Authorization") String token
    ) {
        try {
            // VALIDAR TOKEN
            if (token == null || token.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Falta token")
                        .build();
            }

            Long adminId = tokenService.validarToken(token);

            // Validación básica
            if (request == null ||
                request.getChatId() <= 0 ||
                request.getUsuarioId() <= 0) {

                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Datos inválidos")
                        .build();
            }

            chatService.agregarMiembro(
                request.getChatId(),
                adminId.intValue(), 
                request.getUsuarioId()
            );

            return Response.ok("Miembro agregado correctamente").build();

        } catch (RuntimeException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Token inválido")
                    .build();
        }
    }

    @POST
    @Path("/eliminar-miembro")
    public Response eliminarMiembro(
        EliminarMiembroRequest request,
        @HeaderParam("Authorization") String token
    ) {
        try {

            if (token == null || token.isBlank()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Falta token")
                        .build();
            }

            Long adminId = tokenService.validarToken(token);

            if (request == null ||
                request.getChatId() <= 0 ||
                request.getUsuarioId() <= 0) {

                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Datos inválidos")
                        .build();
            }

            chatService.eliminarMiembro(
                request.getChatId(),
                adminId.intValue(),
                request.getUsuarioId()
            );

            return Response.ok("Miembro eliminado").build();

        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }
    }

    private String obtenerNombre(Chat chat, int usuarioActualId) {

        if (chat.getTipo() == TipoChat.PRIVADO) {

            List<MiembroChat> miembros = chat.getMiembros();

            if (miembros.size() == 1) {
                return miembros.get(0).getUsuario().getNombre();
            }

            return miembros.stream()
                .filter(m -> m.getUsuario().getId() != usuarioActualId)
                .findFirst()
                .get()
                .getUsuario()
                .getNombre();
        }

        return chat.getNombre();
    }
}