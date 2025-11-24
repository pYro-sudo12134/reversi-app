package com.example.controller;

import io.quarkus.runtime.Quarkus;
import io.quarkus.security.Authenticated;
import io.vertx.core.json.JsonObject;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

@Path("/shutdown")
@Authenticated
public class QuarkusShutdownController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Response shutdown() {
        CompletableFuture.runAsync(() -> {
            try {
                Quarkus.asyncExit();
            } catch (Exception e) {
                logger.error("Shutdown failed", e);
            }
        });

        return Response.accepted()
                .entity(new JsonObject().put("status", "shutdown_initiated"))
                .build();
    }
}
