package com.example.controller;

import com.example.entity.LobbyDTO;
import com.example.service.LobbyDTOService;
import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Authenticated
@Path("/lobbies")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LobbyController {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    @Inject
    LobbyDTOService lobbyDTOService;

    @GET
    @Path("/all-lobbies")
    @RolesAllowed({"USER", "ADMIN"})
    public Uni<Response> getLobbies() {
        return lobbyDTOService.findAll()
                .onItem().transform(lobbies ->
                        Response.ok(lobbies).build()
                )
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Failed to fetch lobbies", failure);
                    return Response.serverError().build();
                });
    }

    @GET
    @Path("/by-lobby-id/{objectId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Uni<Response> getLobbiesByLobbyId(@PathParam("objectId") ObjectId objectId) {
        return lobbyDTOService.findByLobbyId(objectId)
                .onItem().ifNotNull().transform(lobby ->
                        Response.ok(lobby.toString()).build()
                )
                .onItem().ifNull().continueWith(() ->
                        Response.status(404).entity("Lobby not found by its id: " + objectId).build()
                )
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Failed to fetch lobby", failure);
                    return Response.serverError().build();
                });
    }


    @GET
    @Path("/by-user-id/{objectId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Uni<Response> getLobbiesByUserId(@PathParam("objectId") ObjectId objectId) {
        return lobbyDTOService.findByUserId(objectId)
                .onItem().ifNotNull().transform(lobby ->
                        Response.ok(lobby.toString()).build()
                )
                .onItem().ifNull().continueWith(() ->
                        Response.status(404).entity("Lobby not found by user id: " + objectId).build()
                )
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Failed to fetch lobby", failure);
                    return Response.serverError().build();
                });
    }

    @GET
    @Path("/by-size-not-equal-to-2")
    @RolesAllowed({"USER", "ADMIN"})
    public Uni<Response> getLobbiesBySizeNotEqualToTwo() {
        return lobbyDTOService.findByUserAmount()
                .onItem().ifNotNull().transform(lobby ->
                        Response.ok(lobby.toString()).build()
                )
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Failed to fetch lobby", failure);
                    return Response.serverError().build();
                });
    }

    @GET
    @Path("/alive-lobbies")
    @RolesAllowed({"USER", "ADMIN"})
    public Uni<Response> getAliveLobbies() {
        return lobbyDTOService.findByLiveness()
                .onItem().ifNotNull().transform(lobby ->
                        Response.ok(lobby.toString()).build()
                )
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Failed to fetch lobby", failure);
                    return Response.serverError().build();
                });
    }

    @DELETE
    @Path("/by-lobby-id/{objectId}")
    @RolesAllowed({"ADMIN"})
    public Uni<Response> deleteLobbyByLobbyId(@PathParam("objectId") ObjectId objectId) {
        return lobbyDTOService.deleteById(objectId)
                .onItem().transform(ignore -> Response.ok("Deleted lobby" + objectId).build())
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Delete failed", failure);
                    if (failure instanceof NotFoundException) {
                        return Response.status(404).entity("User not found by id: "+objectId).build();
                    }
                    return Response.serverError().build();
                });
    }

    @DELETE
    @Path("/by-user-id/{objectId}")
    @RolesAllowed({"ADMIN"})
    public Uni<Response> deleteLobbyByUserId(@PathParam("objectId") ObjectId objectId) {
        return lobbyDTOService.deleteByUserId(objectId)
                .onItem().transform(ignore -> Response.ok("Deleted lobbies user id: " + objectId).build())
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Delete failed", failure);
                    if (failure instanceof NotFoundException) {
                        return Response.status(404).entity("User not found by id: "+objectId).build();
                    }
                    return Response.serverError().build();
                });
    }

    @DELETE
    @Path("/all-lobbies")
    @RolesAllowed({"ADMIN"})
    public Uni<Response> deleteLobbies() {
        return lobbyDTOService.deleteAll()
                .onItem().transform(ignore -> Response.ok("Deleted lobbies amount: " + ignore.toString()).build())
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Delete failed", failure);
                    if (failure instanceof NotFoundException) {
                        return Response.status(404).entity("Lobby not found by id: ").build();
                    }
                    return Response.serverError().build();
                });
    }

    @DELETE
    @Path("/unalive-lobbies")
    @RolesAllowed({"ADMIN"})
    public Uni<Response> deleteUnaliveLobbies() {
        return lobbyDTOService.deleteUnaliveLobbies()
                .onItem().transform(ignore -> Response.ok("Deleted lobbies amount: " + ignore.toString()).build())
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Delete failed", failure);
                    if (failure instanceof NotFoundException) {
                        return Response.status(404).entity("Lobby not found by id: ").build();
                    }
                    return Response.serverError().build();
                });
    }

    @POST
    @Path("/insert")
    @RolesAllowed({"USER", "ADMIN"})
    public Uni<Response> insert(List<LobbyDTO> userDTOList){
        return lobbyDTOService.insertLobbies(userDTOList).onItem().transform(ignore -> {
            StringBuilder stringBuilder = new StringBuilder();
            userDTOList.forEach(x -> stringBuilder.append(x.toString()).append("\n"));
            logger.info("Inserted lobbies : " + stringBuilder);
            return Response.status(201).entity(stringBuilder.toString()).build();
        });
    }

    @PATCH
    @Path("/insert-into-lobby")
    @RolesAllowed({"USER", "ADMIN"})
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Uni<Response> insertIntoLobby(@FormParam("lobbyId") ObjectId lobbyId,
                                         @FormParam("userId") ObjectId userid) {
        return lobbyDTOService.insertIntoLobby(lobbyId, userid)
                .onItem().transform(count -> Response.ok("Inserted: " + count).build())
                .onFailure().recoverWithItem(failure ->{
                    logger.error("Insertion failed", failure);
                    if (failure instanceof NotFoundException) {
                        return Response.status(404).entity("Lobby not found").build();
                    }
                    return Response.serverError().build();
                });
    }

    @PATCH
    @Path("/delete-from-lobby")
    @RolesAllowed({"USER", "ADMIN"})
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Uni<Response> deleteFromLobby(@FormParam("lobbyId") ObjectId lobbyId,
                                         @FormParam("userId") ObjectId userid) {
        return lobbyDTOService.deleteFromLobby(lobbyId, userid)
                .onItem().transform(count -> Response.ok("Deleted: " + count).build())
                .onFailure().recoverWithItem(failure ->{
                    logger.error("Deletion failed", failure);
                    if (failure instanceof NotFoundException) {
                        return Response.status(404).entity("Lobby not found").build();
                    }
                    return Response.serverError().build();
                });
    }

    @PATCH
    @Path("/unalive-lobbies/{objectId}")
    @RolesAllowed({"USER", "ADMIN"})
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Uni<Response> unaliveLobbies(@PathParam("objectId") ObjectId objectId) {
        return lobbyDTOService.unaliveLobbies(objectId)
                .onItem().transform(count -> Response.ok("Unalived: " + count).build())
                .onFailure().recoverWithItem(failure ->{
                    logger.error("Update failed", failure);
                    if (failure instanceof NotFoundException) {
                        return Response.status(404).entity("Lobby not found").build();
                    }
                    return Response.serverError().build();
                });
    }
}
