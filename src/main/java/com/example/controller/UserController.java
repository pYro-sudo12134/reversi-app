package com.example.controller;

import com.example.entity.UserDTO;
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
import com.example.service.UserDTOService;

import java.util.List;

@Authenticated
@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserController {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    @Inject
    UserDTOService userDTOService;

    @GET
    @Path("/all-users")
    @RolesAllowed({"USER", "ADMIN"})
    public Uni<Response> getUsers() {
        return userDTOService.findAll()
                .onItem().ifNotNull().transform(users ->
                        Response.ok(users).build()
                )
                .onItem().ifNull().continueWith(() ->
                        Response.status(404).entity("User not found").build()
                )
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Failed to fetch user", failure);
                    return Response.serverError().build();
                });
    }

    @GET
    @Path("/by-user-id/{objectId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Uni<Response> getUserById(@PathParam("objectId") ObjectId objectId) {
        return userDTOService.findById(objectId)
                .onItem().ifNotNull().transform(users ->
                        Response.ok(users).build()
                )
                .onItem().ifNull().continueWith(() ->
                        Response.status(404).entity("User not found by id: " + objectId).build()
                )
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Failed to fetch user", failure);
                    return Response.serverError().build();
                });
    }

    @GET
    @Path("/by-user-username/{username}")
    @RolesAllowed({"USER", "ADMIN"})
    public Uni<Response> getUserByUsername(@PathParam("username") String username) {
        return userDTOService.findByUsername(username)
                .onItem().ifNotNull().transform(users ->
                        Response.ok(users).build()
                )
                .onItem().ifNull().continueWith(() ->
                        Response.status(404).entity("User not found by username: " + username).build()
                )
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Failed to fetch user", failure);
                    return Response.serverError().build();
                });
    }

    @GET
    @Path("/by-user-wins/{wins}")
    @RolesAllowed({"USER", "ADMIN"})
    public Uni<Response> getUserByWins(@PathParam("wins") Long wins) {
        return userDTOService.findByWins(wins)
                .onItem().ifNotNull().transform(users ->
                        Response.ok(users).build()
                )
                .onItem().ifNull().continueWith(() ->
                        Response.status(404).entity("User not found by win amount: " + wins).build()
                )
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Failed to fetch user", failure);
                    return Response.serverError().build();
                });
    }

    @GET
    @Path("/by-user-losses/{losses}")
    @RolesAllowed({"USER", "ADMIN"})
    public Uni<Response> getUserByLosses(@PathParam("losses") Long losses) {
        return userDTOService.findByLosses(losses)
                .onItem().ifNotNull().transform(users ->
                        Response.ok(users).build()
                )
                .onItem().ifNull().continueWith(() ->
                        Response.status(404).entity("User not found by loss amount: " + losses).build()
                )
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Failed to fetch user", failure);
                    return Response.serverError().build();
                });
    }

    @DELETE
    @Path("/by-user-id/{objectId}")
    @RolesAllowed("ADMIN")
    public Uni<Response> deleteByUserId(@PathParam("objectId")ObjectId objectId) {
        return userDTOService.deleteById(objectId)
                .onItem().transform(ignore -> Response.ok("deleted user" + objectId).build())
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Delete failed", failure);
                    if (failure instanceof NotFoundException) {
                        return Response.status(404).entity("User not found by id: "+objectId).build();
                    }
                    return Response.serverError().build();
                });
    }

    @DELETE
    @Path("/by-user-username/{username}")
    @RolesAllowed("ADMIN")
    public Uni<Response> deleteUserByUsername(@PathParam("username") String username) {
        return userDTOService.deleteByUsername(username)
                .onItem().ifNotNull().transform(number ->
                        Response.ok("Deleted amount: " + number.toString()).build()
                )
                .onItem().ifNull().continueWith(() ->
                        Response.status(404).entity("User not found by username: " + username).build()
                )
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Failed to delete user", failure);
                    return Response.serverError().build();
                });
    }

    @DELETE
    @Path("/by-user-wins/{wins}")
    @RolesAllowed("ADMIN")
    public Uni<Response> deleteUserByWins(@PathParam("wins") Long wins) {
        return userDTOService.deleteByWins(wins)
                .onItem().ifNotNull().transform(number ->
                        Response.ok("Deleted amount:" + number.toString()).build()
                )
                .onItem().ifNull().continueWith(() ->
                        Response.status(404).entity("User not found by win amount: " + wins).build()
                )
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Failed to fetch user", failure);
                    return Response.serverError().build();
                });
    }

    @DELETE
    @Path("/by-user-losses/{losses}")
    @RolesAllowed("ADMIN")
    public Uni<Response> deleteUserByLosses(@PathParam("losses") Long losses) {
        return userDTOService.deleteByLosses(losses)
                .onItem().ifNotNull().transform(number ->
                        Response.ok("Deleted amount: " + number.toString()).build()
                )
                .onItem().ifNull().continueWith(() ->
                        Response.status(404).entity("User not found by loss amount: " + losses).build()
                )
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Failed to fetch user", failure);
                    return Response.serverError().build();
                });
    }

    @DELETE
    @Path("/all-users")
    @RolesAllowed("ADMIN")
    public Uni<Response> deleteAllUsers(){
        return userDTOService.deleteAll()
                .onItem().ifNotNull().transform(number ->
                        Response.ok("Deleted amount: " + number.toString()).build()
                )
                .onItem().ifNull().continueWith(() ->
                        Response.status(404).entity("User not found").build()
                )
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Failed to fetch user", failure);
                    return Response.serverError().build();
                });
    }

    @POST
    @Path("/insert")
    @RolesAllowed({"USER", "ADMIN"})
    public Uni<Response> insert(List<UserDTO> userDTOList){
        return userDTOService.insert(userDTOList)
                .onItem().transform(ignore -> {
                    logger.info("Inserted users successfully");
                    return Response.status(201).build();
                })
                .onFailure().recoverWithItem(failure -> {
                    logger.error("Insert failed", failure);
                    return Response.serverError().entity(failure.getMessage()).build();
                });
    }

    @PATCH
    @Path("/update-losses/{objectId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Uni<Response> updateLossesById(@PathParam("objectId") ObjectId objectId){
        return userDTOService.updateLossesById(objectId)
                .onItem().transform(count -> Response.ok("Updated: " + count).build())
                .onFailure().recoverWithItem(failure ->{
                    logger.error("Update failed", failure);
                    if (failure instanceof NotFoundException) {
                        return Response.status(404).entity("User not found").build();
                    }
                    return Response.serverError().build();
                });
    }

    @PATCH
    @Path("/update-wins/{objectId}")
    @RolesAllowed({"USER", "ADMIN"})
    public Uni<Response> updateWinsById(@PathParam("objectId") ObjectId objectId){
        return userDTOService.updateWinsById(objectId)
                .onItem().transform(count -> Response.ok("Updated: " + count).build())
                .onFailure().recoverWithItem(failure ->{
                    logger.error("Update failed", failure);
                    if (failure instanceof NotFoundException) {
                        return Response.status(404).entity("User not found").build();
                    }
                    return Response.serverError().build();
                });
    }
}
