package com.example.controller;

import com.example.service.AuthService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class AuthController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Inject
    AuthService authService;

    @POST
    @PermitAll
    @Path("/login")
    public Uni<Response> login(@FormParam("username") String username,
                               @FormParam("password") String password) {
        logger.info("Login attempt for: {}", username);

        return authService.login(username, password)
                .onItem().transform(token -> {
                    logger.info("Login successful: {}", username);
                    return Response.ok(token).build();
                })
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Login failed: {}", failure.getMessage());
                    return Response.status(401)
                            .entity("{\"error\":\"" + failure.getMessage() + "\"}")
                            .build();
                });
    }

    @POST
    @Path("/register")
    @PermitAll
    public Uni<Response> register(@FormParam("username") String username,
                                  @FormParam("password") String password) {
        logger.info("Registration attempt for: {}", username);

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return Uni.createFrom().item(Response.status(400)
                    .entity("{\"error\":\"Username and password are required\"}")
                    .build());
        }

        return authService.registerUser(username, password)
                .onItem().transform(user -> {
                    logger.info("User registered successfully: {}", username);
                    return Response.status(Response.Status.CREATED)
                            .entity("{\"message\":\"User registered successfully\"}")
                            .build();
                })
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Registration failed: {}", failure.getMessage());
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("{\"error\":\"" + failure.getMessage() + "\"}")
                            .build();
                });
    }


    @POST
    @Path("/register-admin")
    @RolesAllowed("ADMIN")
    public Uni<Response> registerAdmin(@FormParam("username") String username,
                                       @FormParam("password") String password) {
        logger.info("Admin registration attempt has been occurred for: {}", username);
        return authService.registerAdmin(username, password)
                .onItem().transform(user -> Response.status(Response.Status.CREATED).entity(user).build())
                .onFailure().recoverWithItem(failure ->
                        Response.status(Response.Status.BAD_REQUEST)
                                .entity(failure.getMessage()).build()
                );
    }
}