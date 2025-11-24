package com.example.service;

import com.example.entity.UserDTO;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import com.example.repository.UserRepository;

import java.util.List;

@ApplicationScoped
public class UserDTOService {
    @Inject
    UserRepository userRepository;
    @CacheResult(cacheName = "user-cache-all")
    public Uni<List<UserDTO>> findAll(){
        return userRepository.findAllUsers();
    }
    @CacheResult(cacheName = "user-cache-by-id")
    public Uni<UserDTO> findById(ObjectId objectId){
        return userRepository.findById(objectId);
    }
    @CacheResult(cacheName = "user-cache-by-username")
    public Uni<List<UserDTO>> findByUsername(String username){
        return userRepository.findByUsername(username);
    }
    @CacheResult(cacheName = "user-cache-by-wins")
    public Uni<List<UserDTO>> findByWins(Long wins){
        return userRepository.findByWins(wins);
    }
    @CacheResult(cacheName = "user-cache-by-losses")
    public Uni<List<UserDTO>> findByLosses(Long losses){
        return userRepository.findByLosses(losses);
    }
    @CacheInvalidate(cacheName = "user-cache-all")
    @CacheInvalidate(cacheName = "user-cache-by-losses")
    @CacheInvalidate(cacheName = "user-cache-by-wins")
    @CacheInvalidate(cacheName = "user-cache-by-username")
    @CacheInvalidate(cacheName = "user-cache-by-id")
    public Uni<Boolean> deleteById(ObjectId objectId){
        return userRepository.deleteById(objectId);
    }
    @CacheInvalidate(cacheName = "user-cache-all")
    @CacheInvalidate(cacheName = "user-cache-by-losses")
    @CacheInvalidate(cacheName = "user-cache-by-wins")
    @CacheInvalidate(cacheName = "user-cache-by-username")
    @CacheInvalidate(cacheName = "user-cache-by-id")
    public Uni<Long> deleteByUsername(String username){
        return userRepository.deleteByUsername(username);
    }
    @CacheInvalidate(cacheName = "user-cache-all")
    @CacheInvalidate(cacheName = "user-cache-by-losses")
    @CacheInvalidate(cacheName = "user-cache-by-wins")
    @CacheInvalidate(cacheName = "user-cache-by-username")
    @CacheInvalidate(cacheName = "user-cache-by-id")
    public Uni<Long> deleteByWins(Long wins){
        return userRepository.deleteByWins(wins);
    }
    @CacheInvalidate(cacheName = "user-cache-all")
    @CacheInvalidate(cacheName = "user-cache-by-losses")
    @CacheInvalidate(cacheName = "user-cache-by-wins")
    @CacheInvalidate(cacheName = "user-cache-by-username")
    @CacheInvalidate(cacheName = "user-cache-by-id")
    public Uni<Long> deleteByLosses(Long losses){
        return userRepository.deleteByLosses(losses);
    }
    @CacheInvalidate(cacheName = "user-cache-all")
    @CacheInvalidate(cacheName = "user-cache-by-losses")
    @CacheInvalidate(cacheName = "user-cache-by-wins")
    @CacheInvalidate(cacheName = "user-cache-by-username")
    @CacheInvalidate(cacheName = "user-cache-by-id")
    public Uni<Long> deleteAll(){
        return userRepository.deleteAllUsers();
    }
    @CacheInvalidate(cacheName = "user-cache-all")
    @CacheInvalidate(cacheName = "user-cache-by-losses")
    @CacheInvalidate(cacheName = "user-cache-by-wins")
    @CacheInvalidate(cacheName = "user-cache-by-username")
    @CacheInvalidate(cacheName = "user-cache-by-id")
    public Uni<Void> insert(List<UserDTO> userDTOS){
        return userRepository.bulkInsertAsync(userDTOS);
    }

    @CacheInvalidate(cacheName = "user-cache-all")
    @CacheInvalidate(cacheName = "user-cache-by-losses")
    @CacheInvalidate(cacheName = "user-cache-by-wins")
    @CacheInvalidate(cacheName = "user-cache-by-username")
    @CacheInvalidate(cacheName = "user-cache-by-id")
    public Uni<Long> updateWinsById(ObjectId objectId){return userRepository.updateWinsById(objectId);}

    @CacheInvalidate(cacheName = "user-cache-all")
    @CacheInvalidate(cacheName = "user-cache-by-losses")
    @CacheInvalidate(cacheName = "user-cache-by-wins")
    @CacheInvalidate(cacheName = "user-cache-by-username")
    @CacheInvalidate(cacheName = "user-cache-by-id")
    public Uni<Long> updateLossesById(ObjectId objectId){return userRepository.updateLossesById(objectId);}
}
