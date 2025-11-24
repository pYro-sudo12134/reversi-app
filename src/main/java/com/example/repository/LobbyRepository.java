package com.example.repository;

import com.example.entity.LobbyDTO;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.List;

@ApplicationScoped
public class LobbyRepository implements ReactivePanacheMongoRepository<LobbyDTO> {
    public Uni<List<LobbyDTO>> findAllLobbies(){
        return listAll();
    }
    public Uni<LobbyDTO> findByLobbyId(ObjectId objectId){
        return findById(objectId);
    }
    public Uni<List<LobbyDTO>> findByUserId(ObjectId objectId){
        return list("players: ?1", objectId);
    }
    public Uni<List<LobbyDTO>> findByUserAmount() {
        return list("{'$and': [{'players': {'$not': {'$size': 2}}}, {'isAlive': true}]}");
    }
    public Uni<List<LobbyDTO>> findByLiveness() {return list("{isAlive: ?1}", true);}
    public Uni<Boolean> deleteLobbyById(ObjectId objectId) {
        return deleteById(objectId);
    }
    public Uni<Long> deleteAllLobbies(){
        return deleteAll();
    }
    public Uni<Long> deleteByUserId(ObjectId objectId){
        return delete("players", objectId);
    }
    public Uni<Long> deleteUnaliveLobbies(){return delete("isAlive",false);}
    public Uni<Void> bulkInsertAsync(List<LobbyDTO> lobbyDTOS) {
        return persist(lobbyDTOS);
    }
    public Uni<Long> insertInLobby(ObjectId lobbyId, ObjectId userId){
        return update("{$addToSet: {players: ?1}}", userId)
                .where("_id = ?2", lobbyId);
    }
    public Uni<Long> removeFromLobby(ObjectId lobbyId, ObjectId userId){
        return update("{$pull: {players: {$in: ?1}}}", userId)
                .where("_id = ?2", lobbyId);
    }
    public Uni<Long> unaliveLobbies(ObjectId objectId){
        return update("{isAlive: false}").where("_id = ?1", objectId);
    }
}

