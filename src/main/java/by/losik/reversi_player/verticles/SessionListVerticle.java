package by.losik.reversi_player.verticles;

import by.losik.reversi_player.entity.LobbyData;
import by.losik.reversi_player.entity.UserData;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SessionListVerticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Vertx vertx = Vertx.vertx();
    private UserData userData;
    private final WebClient client = WebClient.create(vertx);;
    private List<LobbyData> obtainedData;

    public Future<List<LobbyData>> fetchAllLobbies() {
        Promise<List<LobbyData>> promise = Promise.promise();

        client.get(8081, "localhost", "/children")
                .putHeader("Authorization", "Bearer " + userData.getBearer())
                .send()
                .onSuccess(response -> {
                    if (response.statusCode() == 200) {
                        obtainedData = new CopyOnWriteArrayList<>();
                        JsonObject jsonObject = new JsonObject(response.bodyAsString());
                        jsonObject.getMap().keySet().parallelStream().forEach(lobbyId -> {
                            LobbyData lobbyData = new LobbyData();
                            lobbyData.setLobbyId(lobbyId);
                            obtainedData.add(lobbyData);
                        });
                        logger.info("Fetched {} lobbies", obtainedData.size());
                        promise.complete(obtainedData);
                    } else {
                        logger.error("Request failed (HTTP {}): {}", response.statusCode(), response.bodyAsString());
                        promise.fail("HTTP " + response.statusCode() + ": " + response.bodyAsString());
                    }
                })
                .onFailure(err -> {
                    logger.error("Request failed: {}", err.getMessage());
                    promise.fail(err);
                });

        return promise.future();
    }

    public UserData getUserData() {
        return userData;
    }

    public void setUserData(UserData userData) {
        this.userData = userData;
    }

    @Override
    public void stop() {
        client.close();
    }
}