package com.chat.config;

import com.chat.security.TokenService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.util.Set;

// Valida el token ANTES de que corra cualquier controller. Antes, cada controller
// tenía su propio try/catch alrededor de tokenService.validarToken() y la mayoría
// mezclaba esa excepción con la de su lógica de negocio, devolviendo 500 o 400 para
// un token vencido/inválido en vez de 401 (ClaveGrupoController era la única
// excepción, con un catch dedicado). El frontend solo detecta "sesión expirada"
// mirando el status 401 — cualquier otro código dejaba al usuario con la sesión rota
// en silencio. Con este filtro, un token inválido nunca llega al método del
// controller, así que no depende de que cada uno maneje la excepción bien.
@Provider
@Priority(Priorities.AUTHENTICATION)
public class TokenAuthFilter implements ContainerRequestFilter {

    private static final Set<String> RUTAS_PUBLICAS = Set.of(
        "usuarios/login",
        "usuarios/registro"
    );

    @Inject
    private TokenService tokenService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (HttpMethod.OPTIONS.equals(requestContext.getMethod())) return;

        String path = requestContext.getUriInfo().getPath();
        if (RUTAS_PUBLICAS.contains(path)) return;

        String token = requestContext.getHeaderString("Authorization");
        try {
            tokenService.validarToken(token);
        } catch (Exception e) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED).entity("Token inválido").build()
            );
        }
    }
}
