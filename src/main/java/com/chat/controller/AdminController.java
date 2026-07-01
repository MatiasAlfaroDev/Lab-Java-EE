package com.chat.controller;

import com.chat.datatype.CambiarStatusRequest;
import com.chat.security.TokenService;
import com.chat.service.UsuarioService;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminController {

    @Inject
    private UsuarioService usuarioService;

    @Inject
    private TokenService tokenService;

    @GET
    @Path("/usuarios")
    public Response listarUsuarios(@HeaderParam("Authorization") String token) {
        try {
            Long adminId = tokenService.validarToken(token);
            return Response.ok(usuarioService.listarUsuariosAdmin(adminId.intValue())).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Token inválido").build();
        }
    }

    @PATCH
    @Path("/usuarios/{id}/estado")
    public Response cambiarEstado(
            @PathParam("id") int id,
            CambiarStatusRequest request,
            @HeaderParam("Authorization") String token) {
        try {
            Long adminId = tokenService.validarToken(token);

            if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Datos inválidos").build();
            }

            usuarioService.cambiarEstadoUsuario(adminId.intValue(), id, request.getStatus());
            return Response.ok().build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Token inválido").build();
        }
    }
}
