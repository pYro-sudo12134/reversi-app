package com.example.service;

import com.example.entity.LobbyDTO;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import com.example.repository.LobbyRepository;

import java.util.List;

@ApplicationScoped
public class LobbyDTOService {
    @Inject
    LobbyRepository lobbyRepository;
    @CacheResult(cacheName = "lobby-cache-all")
    public Uni<List<LobbyDTO>> findAll(){
        return lobbyRepository.findAllLobbies();
    }
    @CacheResult(cacheName = "lobby-cache-by-lobby-id")
    public Uni<LobbyDTO> findByLobbyId(ObjectId objectId){
        return lobbyRepository.findByLobbyId(objectId);
    }
    @CacheResult(cacheName = "lobby-cache-by-user-id")
    public Uni<List<LobbyDTO>> findByUserId(ObjectId objectId){
        return lobbyRepository.findByUserId(objectId);
    }
    @CacheResult(cacheName = "lobby-size-not-equals-2")
    public Uni<List<LobbyDTO>> findByUserAmount(){return lobbyRepository.findByUserAmount();}
    @CacheResult(cacheName = "lobby-cache-by-liveness")
    public Uni<List<LobbyDTO>> findByLiveness(){return lobbyRepository.findByLiveness();}
    @CacheInvalidate(cacheName = "lobby-cache-all")
    @CacheInvalidate(cacheName = "lobby-cache-by-user-id")
    @CacheInvalidate(cacheName = "lobby-cache-by-lobby-id")
    @CacheInvalidate(cacheName = "lobby-size-not-equals-2")
    @CacheInvalidate(cacheName = "lobby-cache-by-liveness")
    public Uni<Boolean> deleteById(ObjectId objectId){
        return lobbyRepository.deleteLobbyById(objectId);
    }
    @CacheInvalidate(cacheName = "lobby-cache-all")
    @CacheInvalidate(cacheName = "lobby-cache-by-user-id")
    @CacheInvalidate(cacheName = "lobby-cache-by-lobby-id")
    @CacheInvalidate(cacheName = "lobby-size-not-equals-2")
    @CacheInvalidate(cacheName = "lobby-cache-by-liveness")
    public Uni<Long> deleteByUserId(ObjectId objectId){
        return lobbyRepository.deleteByUserId(objectId);
    }
    @CacheInvalidate(cacheName = "lobby-cache-all")
    @CacheInvalidate(cacheName = "lobby-cache-by-user-id")
    @CacheInvalidate(cacheName = "lobby-cache-by-lobby-id")
    @CacheInvalidate(cacheName = "lobby-size-not-equals-2")
    @CacheInvalidate(cacheName = "lobby-cache-by-liveness")
    public Uni<Long> deleteAll(){
        return lobbyRepository.deleteAllLobbies();
    }

    @CacheInvalidate(cacheName = "lobby-cache-all")
    @CacheInvalidate(cacheName = "lobby-cache-by-user-id")
    @CacheInvalidate(cacheName = "lobby-cache-by-lobby-id")
    @CacheInvalidate(cacheName = "lobby-size-not-equals-2")
    @CacheInvalidate(cacheName = "lobby-cache-by-liveness")
    public Uni<Long> deleteUnaliveLobbies() {return lobbyRepository.deleteUnaliveLobbies();}
    @CacheInvalidate(cacheName = "lobby-cache-all")
    @CacheInvalidate(cacheName = "lobby-cache-by-user-id")
    @CacheInvalidate(cacheName = "lobby-cache-by-lobby-id")
    @CacheInvalidate(cacheName = "lobby-size-not-equals-2")
    @CacheInvalidate(cacheName = "lobby-cache-by-liveness")
    public Uni<Void> insertLobbies(List<LobbyDTO> lobbyDTOS){
        return lobbyRepository.bulkInsertAsync(lobbyDTOS);
    }
    @CacheInvalidate(cacheName = "lobby-cache-all")
    @CacheInvalidate(cacheName = "lobby-cache-by-user-id")
    @CacheInvalidate(cacheName = "lobby-cache-by-lobby-id")
    @CacheInvalidate(cacheName = "lobby-size-not-equals-2")
    @CacheInvalidate(cacheName = "lobby-cache-by-liveness")
    public Uni<Long> insertIntoLobby(ObjectId lobbyId, ObjectId userId){
        return lobbyRepository.insertInLobby(lobbyId, userId);
    }
    @CacheInvalidate(cacheName = "lobby-cache-all")
    @CacheInvalidate(cacheName = "lobby-cache-by-user-id")
    @CacheInvalidate(cacheName = "lobby-cache-by-lobby-id")
    @CacheInvalidate(cacheName = "lobby-size-not-equals-2")
    @CacheInvalidate(cacheName = "lobby-cache-by-liveness")
    public Uni<Long> deleteFromLobby(ObjectId lobbyId, ObjectId userId){
        return lobbyRepository.removeFromLobby(lobbyId, userId);
    }
    @CacheInvalidate(cacheName = "lobby-cache-all")
    @CacheInvalidate(cacheName = "lobby-cache-by-user-id")
    @CacheInvalidate(cacheName = "lobby-cache-by-lobby-id")
    @CacheInvalidate(cacheName = "lobby-size-not-equals-2")
    @CacheInvalidate(cacheName = "lobby-cache-by-liveness")
    public Uni<Long> unaliveLobbies(ObjectId objectId){
        return lobbyRepository.unaliveLobbies(objectId);
    }
}
