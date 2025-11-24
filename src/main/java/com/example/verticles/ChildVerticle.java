package com.example.verticles;

import com.example.configuration.KeyUtils;
import com.example.configuration.VertxRateLimiterHandler;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

public class ChildVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(ChildVerticle.class);
    private static final int MAX_PLAYERS = 2;
    private static final int CLEANUP_DELAY_MS = 5000;
    private static final int LOBBY_CHECK_INTERVAL = 15000;
    private volatile boolean undeploy = false;
    private long lobbyCheckTaskId;
    private final String childId;
    private ObjectId lobbyId = new ObjectId();
    private AsyncFile logFile;
    private final ConcurrentHashMap<ServerWebSocket, String> connectedUsers = new ConcurrentHashMap<>();
    private final int port;
    private JWTAuth authProvider;
    private WebClient webClient;
    private String jwtToken;
    private final RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .limitForPeriod(10)
            .timeoutDuration(Duration.ofMillis(100))
            .build();
    private final RateLimiterRegistry registry = RateLimiterRegistry.of(config);
    private final RateLimiter rateLimiter = registry.rateLimiter("apiLimiter");

    public ChildVerticle(String childId, int port) {
        this.childId = childId;
        this.port = port;
    }

    @Override
    public void start(Promise<Void> startPromise) {
        KeyUtils.readPemFile(vertx, "src/main/resources/publicKey.pem")
                .thenAccept(publicKey -> {
                    authProvider = JWTAuth.create(vertx, new JWTAuthOptions()
                            .addPubSecKey(new PubSecKeyOptions()
                                    .setAlgorithm("RS256")
                                    .setBuffer(publicKey)));

                    webClient = WebClient.create(vertx, new WebClientOptions()
                            .setDefaultHost("localhost")
                            .setDefaultPort(8080));

                    generateToken().compose(token -> {
                                this.jwtToken = token;

                                JsonArray lobbyDataArray = new JsonArray().add(new JsonObject()
                                        .put("players", new JsonArray())
                                        .put("isAlive", true));

                                return webClient.post("/lobbies/insert")
                                        .putHeader("Content-Type", "application/json")
                                        .putHeader("Authorization", "Bearer " + jwtToken)
                                        .sendJson(lobbyDataArray);
                            }).compose(insertionResponse -> {
                                if (insertionResponse.statusCode() != 201) {
                                    return Future.failedFuture("Failed to create lobby: " +
                                            insertionResponse.statusCode() + " - " +
                                            insertionResponse.bodyAsString());
                                }
                                JsonObject responseBody = insertionResponse.bodyAsJsonObject();
                                lobbyId = new ObjectId(responseBody.getString("_id"));
                                logger.info("Successfully created lobby {}", lobbyId);
                                return vertx.fileSystem().open("movement-log-" + childId + ".txt",
                                        new OpenOptions().setAppend(true).setCreate(true));
                            }).onSuccess(file -> {
                                this.logFile = file;
                                setupServer();
                                startPeriodicTasks();
                                startPromise.complete();
                                logger.info("Child verticle {} initialized successfully on port {}", childId, port);
                            })
                            .onFailure(err -> {
                                logger.error("Initialization failed for child {}: {}", childId, err.getMessage());
                                startPromise.fail(err);
                            });
                })
                .exceptionally(ex -> {
                    logger.error("Generation failed for child {}: {}", childId, ex.getMessage());
                    startPromise.fail("Deployment finished with exception");
                    return null;
                });
    }


    private Future<String> generateToken() {
        if( System.getenv("name") == null || System.getenv("password") == null){
            throw new RuntimeException("Environment variables are not preset");
        }

        Promise<String> tokenPromise = Promise.promise();

        MultiMap formData = MultiMap.caseInsensitiveMultiMap()
                .add("username", System.getenv("name"))
                .add("password", System.getenv("password"));

        webClient.post(8080, "localhost", "/auth/login")
                .putHeader("Content-Type", "application/x-www-form-urlencoded")
                .sendForm(formData)
                .onSuccess(response -> {
                    if (response.statusCode() == 200) {
                        String token = response.bodyAsString();
                        tokenPromise.complete(token);
                        logger.info("Login successful. Token stored.");
                    } else {
                        logger.error("Auth failed (HTTP {}): {}", response.statusCode(), response.bodyAsString());
                        tokenPromise.fail("Auth failed (HTTP " + response.statusCode() + ")");
                    }
                })
                .onFailure(err -> {
                    logger.error("Request failed: {}", err.getMessage());
                    tokenPromise.fail(err);
                });

        return tokenPromise.future();
    }

    private void setupServer() {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route().handler(VertxRateLimiterHandler.of(rateLimiter));

        HttpServer httpServer = vertx.createHttpServer()
                .requestHandler(router)
                .webSocketHandler(this::handleWebSocketConnection);

        router.post("/message-child/" + childId)
                .produces("application/json")
                .handler(JWTAuthHandler.create(authProvider))
                .handler(this::handleMessageToChild);
        router.get("/child-health/"+childId).handler(ctx -> ctx.response().end("OK"));

        httpServer.listen(port, result -> {
            if (result.succeeded()) {
                logger.info("Child {} server listening on port {}", childId, port);
            } else {
                logger.error("Child {} failed to start server: {}", childId, result.cause().getMessage());
                performUndeploy();
            }
        });
    }

    private void handleWebSocketConnection(ServerWebSocket ws) {
        if (!("/join-child/"+childId).equals(ws.path())) {
            ws.reject((short) 404);
            return;
        }

        ws.textMessageHandler(msg -> handleWebSocketMessage(ws, msg));
        ws.closeHandler(v -> handleWebSocketClose(ws));
    }

    private void handleWebSocketMessage(ServerWebSocket ws, String msg) {
        try {
            JsonObject userObject = new JsonObject(msg);
            if (!"join".equals(userObject.getString("type"))) {
                throw new RuntimeException("Invalid message type: expected 'join'");
            }

            String userId = userObject.getString("userId");
            if (userId == null || userId.isEmpty()) {
                throw new RuntimeException("Missing or empty userId");
            }

            if (connectedUsers.size() >= MAX_PLAYERS) {
                throw new RuntimeException("Lobby is full (max " + MAX_PLAYERS + " players)");
            }
            if (connectedUsers.containsValue(userId)) {
                throw new RuntimeException("User " + userId + " already connected");
            }

            if (connectedUsers.putIfAbsent(ws, userId) != null) {
                throw new RuntimeException("User " + userId + " already connected");
            }
            logger.info("User {} joined child {}", userId, childId);

            MultiMap formData = MultiMap.caseInsensitiveMultiMap()
                    .add("lobbyId", lobbyId.toString())
                    .add("userId", userId);

            webClient.patch("/lobbies/insert-into-lobby")
                    .putHeader("Content-Type", "application/x-www-form-urlencoded")
                    .putHeader("Authorization", "Bearer "+jwtToken)
                    .sendForm(formData)
                    .onSuccess(insertionResponse -> {
                        if (insertionResponse.statusCode() == 200) {
                            logger.info("User {} added to lobby {}", userId, lobbyId);
                        } else {
                            logger.error("Failed to add user {} to lobby {}: {}",
                                    userId, lobbyId, insertionResponse.bodyAsString());
                            ws.close((short) 500, "Server error");
                        }
                    })
                    .onFailure(err -> {
                        logger.error("Error adding user to lobby: {}", err.getMessage());
                        ws.close((short) 503, "Service unavailable");
                    });
        } catch (Exception e) {
            logger.error("WebSocket error for child {}: {}", childId, e.getMessage());
            ws.close((short) 1008, e.getMessage());
        }
    }

    private void handleWebSocketClose(ServerWebSocket ws) {
        String userId = connectedUsers.remove(ws);
        if (userId != null) {
            logger.info("User {} disconnected from child {}", userId, childId);

            JsonObject disconnectMessage = new JsonObject()
                    .put("type", "disconnect")
                    .put("userId", userId);
            connectedUsers.forEach((remainingWs, remainingUserId) -> {
                if (!remainingWs.isClosed()) {
                    remainingWs.writeTextMessage(disconnectMessage.encode());
                }
            });

            vertx.setTimer(CLEANUP_DELAY_MS, tid -> {
                if (connectedUsers.isEmpty()) {
                    logger.info("No users left in child {}, initiating undeploy", childId);
                    performUndeploy();
                }
            });
        }
    }


    private void handleMessageToChild(RoutingContext ctx) {
        JsonObject message = ctx.body().asJsonObject();
        String messageType = message.getString("type");

        connectedUsers.forEach((ws, userId) -> {
            if (!ws.isClosed()) {
                ws.writeTextMessage(message.toString());
            }
        });

        logFile.write(Buffer.buffer("Activity from: " + childId + " at " + System.currentTimeMillis() + " " + message))
                .onSuccess(succ -> logger.info("Write succeeded for {}: {}", childId, message))
                .onFailure(err -> logger.error("Write failed for {}: {}", childId, err.getMessage()));

        ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("status", "delivered").encode());

        if ("gameEnd".equals(messageType)) {
            updateStatistics(message)
                    .onComplete(v -> performUndeploy());
        }
    }

    private Future<Void> updateStatistics(JsonObject statObject) {
        Promise<Void> promise = Promise.promise();
        String winner = statObject.getString("winner");
        String loser = statObject.getString("loser");

        webClient.patch("/users/update-wins/" + winner)
                .putHeader("Authorization", "Bearer " + jwtToken)
                .send()
                .compose(updateResponse -> {
                    if (updateResponse.statusCode() == 200) {
                        logger.info("Updated wins for {}", winner);
                    } else {
                        logger.error("Failed to update wins for {}: {}", winner, updateResponse.bodyAsString());
                    }
                    return webClient.patch("/users/update-losses/" + loser)
                            .putHeader("Authorization", "Bearer " + jwtToken)
                            .send();
                })
                .compose(updateResponse -> {
                    if (updateResponse.statusCode() == 200) {
                        logger.info("Updated losses for {}", loser);
                    } else {
                        logger.error("Failed to update losses for {}: {}", loser, updateResponse.bodyAsString());
                    }
                    return unaliveLobby();
                })
                .onComplete(promise);

        return promise.future();
    }

    private void startPeriodicTasks() {
        lobbyCheckTaskId = vertx.setPeriodic(LOBBY_CHECK_INTERVAL, id -> {
            connectedUsers.entrySet().removeIf(entry -> entry.getKey().isClosed());
            if (connectedUsers.isEmpty() && !undeploy) {
                performUndeploy();
            }
        });
    }

    private void performUndeploy() {
        if (undeploy) {
            return;
        }
        undeploy = true;

        logger.info("Starting undeploy process for child {}", childId);

        vertx.cancelTimer(lobbyCheckTaskId);

        Promise<Void> fileClosePromise = Promise.promise();
        if (logFile != null) {
            logFile.close(fileClosePromise);
        } else {
            fileClosePromise.complete();
        }

        fileClosePromise.future()
                .compose(v -> unaliveLobby())
                .compose(v -> {
                    Promise<Void> mapPromise = Promise.promise();
                    vertx.sharedData().<String, JsonObject>getAsyncMap("children", res -> {
                        if (res.succeeded()) {
                            res.result().remove(childId);
                            mapPromise.complete();
                        } else {
                            mapPromise.fail(res.cause());
                        }
                    });
                    return mapPromise.future();
                })
                .compose(v -> {
                    Promise<Void> undeployPromise = Promise.promise();
                    vertx.undeploy(deploymentID(), undeployPromise);
                    return undeployPromise.future();
                })
                .onSuccess(v -> logger.info("Child {} undeployed successfully", childId))
                .onFailure(err -> logger.error("Failed to undeploy child {}: {}", childId, err.getMessage()));
    }

    private Future<Void> unaliveLobby() {
        Promise<Void> promise = Promise.promise();

        webClient.patch("/lobbies/unalive-lobbies/" + lobbyId)
                .putHeader("Authorization", "Bearer " + jwtToken)
                .send()
                .onSuccess(updateResponse -> {
                    if (updateResponse.statusCode() == 200) {
                        logger.info("Marked lobby {} as inactive", lobbyId);
                    } else {
                        logger.error("Failed to mark lobby {} as inactive: {}", lobbyId, updateResponse.bodyAsString());
                    }
                    promise.complete();
                })
                .onFailure(err -> {
                    logger.error("Error marking lobby as inactive: {}", err.getMessage());
                    promise.fail(err);
                });

        return promise.future();
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        logger.info("Child {} stopping", childId);
        stopPromise.complete();
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return new JsonObject()
                .put("port", port)
                .put("childId", childId)
                .put("lobbyId", lobbyId)
                .put("deploymentId", deploymentID())
                .toString();
    }
}