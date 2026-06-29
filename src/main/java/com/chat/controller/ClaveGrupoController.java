package com.chat.controller;

import com.chat.dao.ClaveGrupoDAO;
import com.chat.datatype.ClaveGrupoRequest;
import com.chat.model.ClaveGrupo;
import com.chat.security.TokenService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/chats/{chatId}/clave-grupo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClaveGrupoController {

    @Inject private ClaveGrupoDAO claveGrupoDAO;
    @Inject private TokenService tokenService;

    @PUT
    public Response distribuir(
        @PathParam("chatId") int chatId,
        ClaveGrupoRequest req,
        @HeaderParam("Authorization") String token
    ) {
        try { tokenService.validarToken(token); }
        catch (Exception e) { return Response.status(Response.Status.UNAUTHORIZED).entity("Token inválido").build(); }
        try {
            if (req == null || req.envueltas == null || req.envueltas.isEmpty())
                return Response.status(400).entity("Datos inválidos").build();
            for (ClaveGrupoRequest.Envuelta e : req.envueltas) {
                claveGrupoDAO.guardarOActualizar(
                    chatId, e.miembroId, req.distribuidorId, e.claveEnvuelta, req.version);
            }
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(500).entity("Error interno").build();
        }
    }

    @GET
    public Response obtener(
        @PathParam("chatId") int chatId,
        @HeaderParam("Authorization") String token
    ) {
        Long userId;
        try { userId = tokenService.validarToken(token); }
        catch (Exception e) { return Response.status(Response.Status.UNAUTHORIZED).entity("Token inválido").build(); }
        try {
            ClaveGrupo cg = claveGrupoDAO.buscarPorMiembro(chatId, userId.intValue());
            if (cg == null) return Response.status(404).build();
            String json = String.format(
                "{\"claveEnvuelta\":\"%s\",\"version\":%d,\"distribuidorId\":%d}",
                cg.getClaveEnvuelta(), cg.getVersion(), cg.getDistribuidorId());
            return Response.ok(json).build();
        } catch (Exception e) {
            return Response.status(500).entity("Error interno").build();
        }
    }
}
