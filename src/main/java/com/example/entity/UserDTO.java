package com.example.entity;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.vertx.core.json.JsonObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

@MongoEntity(database = "users", collection = "users")
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
public class UserDTO extends PanacheMongoEntityBase {
    @BsonId
    private ObjectId id;
    @BsonProperty("username")
    private String username;
    @BsonProperty("password")
    private String password;
    @BsonProperty("wins")
    private Long wins;
    @BsonProperty("role")
    private String role;
    @BsonProperty("losses")
    private Long losses;
    @Override
    public String toString() {
        return String.valueOf(new JsonObject()
                        .put("_id", id)
                        .put("username", username)
                        .put("wins", wins)
                        .put("losses", losses));
    }
}
