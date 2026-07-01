package com.chat.controller;

import com.chat.datatype.CrearEncuestaRequest;
import com.chat.security.TokenService;
import com.chat.service.EncuestaService;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/encuestas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EncuestaController {

    @Inject
    private EncuestaService encuestaService;

    @Inject
    private TokenService tokenService;

    @POST
    @Path("/chat/{chatId}")
    public Response crear(
            @PathParam("chatId") int chatId,
            CrearEncuestaRequest request,
            @HeaderParam("Authorization") String token) {
        try {
            Long userId = tokenService.validarToken(token);

            if (request == null || request.getPregunta() == null || request.getPregunta().isBlank()
                    || request.getOpciones() == null || request.getOpciones().size() < 2) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Datos inválidos").build();
            }

            return Response.ok(encuestaService.crearEncuesta(
                    chatId, userId.intValue(), request.getPregunta(),
                    request.getOpciones(), request.isAnonima(), request.getExpires_at()
            )).build();

        } catch (RuntimeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error interno").build();
        }
    }

    @POST
    @Path("/{pollId}/opciones/{opcionId}/votar")
    public Response votar(
            @PathParam("pollId") int pollId,
            @PathParam("opcionId") int opcionId,
            @HeaderParam("Authorization") String token) {
        try {
            Long userId = tokenService.validarToken(token);
            return Response.ok(encuestaService.votar(pollId, opcionId, userId.intValue())).build();
        } catch (RuntimeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error interno").build();
        }
    }

    @GET
    @Path("/{pollId}/resultados")
    public Response resultados(
            @PathParam("pollId") int pollId,
            @HeaderParam("Authorization") String token) {
        try {
            Long userId = tokenService.validarToken(token);
            return Response.ok(encuestaService.resultados(pollId, userId.intValue())).build();
        } catch (RuntimeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error interno").build();
        }
    }
}
