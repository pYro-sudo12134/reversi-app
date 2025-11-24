package com.example.entity;

import java.util.List;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.vertx.core.json.JsonObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

@MongoEntity(database = "lobbies", collection = "lobbies")
@Data
@EqualsAndHashCode(callSuper=false)
@NoArgsConstructor
public class LobbyDTO extends PanacheMongoEntityBase {
    @BsonId
    private ObjectId id;
    @BsonProperty("players")
    private List<ObjectId> players;
    @BsonProperty("isAlive")
    private boolean alive;

    @Override
    public String toString() {
        return String.valueOf(new JsonObject()
                .put("_id", id)
                .put("players", players)
                .put("isAlive", alive));
    }
}
