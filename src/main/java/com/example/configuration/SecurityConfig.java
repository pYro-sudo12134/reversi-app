package com.example.configuration;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Path("/auth")
public class SecurityConfig {
    @Inject
    SecurityIdentity securityIdentity;

    @GET
    @Path("/me")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> me() {
        return Uni.createFrom().item(() -> {
            if (securityIdentity.isAnonymous()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Collections.singletonMap("error", "Not authenticated"))
                        .build();
            }

            String username = securityIdentity.getPrincipal().getName();
            Set<String> roles = new HashSet<>(securityIdentity.getRoles());

            Map<String, Object> response = Map.of(
                    "username", username,
                    "roles", roles
            );

            return Response.ok(response).build();
        });
    }
}