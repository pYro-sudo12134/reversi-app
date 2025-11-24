package com.example.repository;

import com.example.entity.UserDTO;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.types.ObjectId;

import java.util.List;

@ApplicationScoped
public class UserRepository implements ReactivePanacheMongoRepository<UserDTO> {
    public Uni<List<UserDTO>> findAllUsers(){
        return listAll();
    }
    public Uni<List<UserDTO>> findByUsername(String username){
        return find("username", username).list();
    }
    public Uni<List<UserDTO>> findByWins(Long wins){
        return find("wins", wins).list();
    }
    public Uni<List<UserDTO>> findByLosses(Long losses){
        return find("losses", losses).list();
    }
    @Override
    public Uni<Boolean> deleteById(ObjectId objectId) {
        return delete("_id", objectId).map(count -> count > 0);
    }
    public Uni<Long> deleteByUsername(String username){
        return delete("{'username': {$regex: ?1, $options: 'i'}}", username);
    }
    public Uni<Long> deleteByWins(Long wins){
        return delete("wins", wins);
    }
    public Uni<Long> deleteByLosses(Long losses){
        return delete("losses", losses);
    }
    public Uni<Long> deleteAllUsers(){
        return deleteAll();
    }
    public Uni<Void> bulkInsertAsync(List<UserDTO> userDTOS) {
        return persist(userDTOS).replaceWithVoid();
    }
    public Uni<Long> updateWinsById(ObjectId objectId){
        return update("{ $inc: { wins: ?1 } }", 1)
            .where("_id = ?2", objectId);}
    public Uni<Long> updateLossesById(ObjectId objectId){
        return update("{ $inc: { losses: ?1 } }", 1)
                .where("_id = ?2", objectId);}
}
