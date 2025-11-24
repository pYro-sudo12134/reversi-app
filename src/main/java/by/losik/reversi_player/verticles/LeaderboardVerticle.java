package by.losik.reversi_player.verticles;

import by.losik.reversi_player.entity.UserData;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LeaderboardVerticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Vertx vertx = Vertx.vertx();
    private UserData userData;
    private final WebClient client = WebClient.create(vertx);;
    private List<UserData> obtainedData;

    public Future<List<UserData>> fetchAllUsers() {
        Promise<List<UserData>> promise = Promise.promise();

        client.get(8080, "localhost", "/users/all-users")
                .putHeader("Content-Type", "application/json")
                .putHeader("Authorization", "Bearer " + userData.getBearer())
                .send()
                .onSuccess(response -> {
                    if (response.statusCode() == 200) {
                        obtainedData = new CopyOnWriteArrayList<>();
                        JsonArray jsonArray = new JsonArray(response.bodyAsString());
                        jsonArray.stream().parallel().forEach(x -> {
                            JsonObject userObject = new JsonObject(x.toString());
                            UserData userToPush = new UserData();
                            userToPush.setUsername(userObject.getString("username"));
                            userToPush.setWins(userObject.getLong("wins"));
                            userToPush.setLosses(userObject.getLong("losses"));
                            obtainedData.add(userToPush);
                            logger.info("Added user: {}", userToPush);
                        });
                        promise.complete(obtainedData);
                    } else {
                        logger.error("Request failed (HTTP {}): {}", response.statusCode(), response.bodyAsString());
                        promise.fail("HTTP " + response.statusCode());
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
        if (client != null) {
            client.close();
        }
    }
}