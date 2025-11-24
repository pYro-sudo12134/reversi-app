package by.losik.reversi_player.verticles;

import by.losik.reversi_player.entity.UserData;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginVerticle extends AbstractVerticle {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    Vertx vertx = Vertx.vertx();
    WebClient client = WebClient.create(vertx);
    private UserData userData;

    public Future<Void> login() {
        MultiMap formData = MultiMap.caseInsensitiveMultiMap()
                .add("username", userData.getUsername())
                .add("password", userData.getPassword());

        return client.post(8080, "localhost", "/auth/login")
                .putHeader("Content-Type", "application/x-www-form-urlencoded")
                .sendForm(formData)
                .compose(response -> {
                    if (response.statusCode() == 200) {
                        String token = response.bodyAsString();
                        userData.setBearer(token);
                        return setRole();
                    } else {
                        logger.error("Auth failed (HTTP {}): {}", response.statusCode(), response.bodyAsString());
                        return Future.failedFuture("Auth failed (HTTP " + response.statusCode() + ")");
                    }
                });
    }

    private Future<Void> setRole() {
        return client.get(8080, "localhost", "/auth/me")
                .putHeader("Authorization", "Bearer " + userData.getBearer())
                .send()
                .compose(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject roleBody = response.bodyAsJsonObject();
                        JsonArray jsonArray = roleBody.getJsonArray("roles");
                        userData.setRole(jsonArray.stream().noneMatch(x -> x.equals("ADMIN")) ? "USER" : "ADMIN");
                        logger.info("Login successful. Role stored in UserData.");
                        logger.debug(userData.getRole());
                        return Future.succeededFuture();
                    } else {
                        logger.error("Auth failed (HTTP {}): {}", response.statusCode(), response.bodyAsString());
                        return Future.failedFuture("Auth failed (HTTP " + response.statusCode() + ")");
                    }
                });
    }

    public UserData getUserData() {
        return userData;
    }

    public void setUserData(UserData userData) {
        this.userData = userData;
    }
}
