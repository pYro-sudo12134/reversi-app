package by.losik.reversi_player.verticles;

import by.losik.reversi_player.entity.UserData;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistrationVerticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Vertx vertx = Vertx.vertx();
    private UserData userData;
    private WebClient client;

    @Override
    public void start() {
        client = WebClient.create(vertx);
    }

    public Future<UserData> registerUser() {
        Promise<UserData> promise = Promise.promise();

        MultiMap formData = MultiMap.caseInsensitiveMultiMap()
                .add("username", userData.getUsername())
                .add("password", userData.getPassword());

        client.post(8080, "localhost", "/auth/register")
                .putHeader("Content-Type", "application/x-www-form-urlencoded")
                .sendForm(formData)
                .onSuccess(response -> {
                    if (response.statusCode() == 201) {
                        JsonObject credentials = new JsonObject(response.bodyAsString());
                        userData.setId(credentials.getString("id"));
                        userData.setUsername(credentials.getString("username"));
                        userData.setPassword(credentials.getString("password"));
                        userData.setWins(credentials.getLong("wins"));
                        userData.setLosses(credentials.getLong("losses"));
                        logger.info("Registration successful for user: {}", userData.getUsername());
                        promise.complete(userData);
                    } else {
                        logger.error("Registration failed (HTTP {}): {}", response.statusCode(), response.bodyAsString());
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
        if (client != null) {
            client.close();
        }
    }
}