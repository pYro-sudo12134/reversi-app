package by.losik.reversi_player.verticles;

import by.losik.reversi_player.entity.UserData;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(ClientVerticle.class);
    private static final int RECONNECT_DELAY_MS = 3000;
    private static final int MESSAGE_TIMEOUT_MS = 15000;
    private static final int MAX_RETRIES = 5;

    private UserData userData;
    private WebClient webClient;
    private WebSocketClient webSocketClient;
    private WebSocket activeWebSocket;
    private String connectedChildId;
    private int port;
    private long heartbeatTimer;
    private final Set<String> messageTypes = Set.of("join", "move", "gameEnd");
    private Promise<JsonObject> pendingMessagePromise;
    private AtomicBoolean connectionActive = new AtomicBoolean(false);
    private int retryCount = 0;

    @Override
    public void start() {
        this.webClient = WebClient.create(vertx, new WebClientOptions()
                .setDefaultHost("localhost")
                .setDefaultPort(8081)
                .setConnectTimeout(5000));

        this.webSocketClient = vertx.createWebSocketClient();

        logger.info("ClientVerticle started and ready to connect to children");
    }

    public Future<Void> connectToChild(String childId) {
        return findChild(childId)
                .compose(childInfo -> {
                    this.connectedChildId = childId;
                    this.port = childInfo.getInteger("port");
                    return attemptWebSocketConnection();
                })
                .recover(err -> {
                    logger.warn("Initial connection failed: {}", err.getMessage());
                    return scheduleReconnect();
                });
    }

    private Future<Void> attemptWebSocketConnection() {
        Promise<Void> connectionPromise = Promise.promise();

        webSocketClient.connect(port, "localhost", "/join-child/" + connectedChildId)
                .onSuccess(webSocket -> {
                    this.activeWebSocket = webSocket;
                    this.connectionActive.set(true);
                    this.retryCount = 0; // Reset retry counter on successful connection

                    startHeartbeat();
                    setupMessageHandler(webSocket);
                    sendJoinMessage(webSocket)
                            .onFailure(err -> {
                                logger.error("Failed to send join message: {}", err.getMessage());
                                connectionPromise.fail(err);
                            });

                    webSocket.closeHandler(v -> handleConnectionClose());
                    webSocket.exceptionHandler(this::handleConnectionError);

                    connectionPromise.complete();
                })
                .onFailure(err -> {
                    logger.error("WebSocket connection failed: {}", err.getMessage());
                    connectionPromise.fail(err);
                });

        return connectionPromise.future();
    }

    private Future<Void> scheduleReconnect() {
        if (retryCount >= MAX_RETRIES) {
            logger.error("Max reconnection attempts reached");
            return Future.failedFuture("Max reconnection attempts reached");
        }

        retryCount++;
        long delay = RECONNECT_DELAY_MS * retryCount; // Exponential backoff

        Promise<Void> promise = Promise.promise();
        logger.info("Scheduling reconnection attempt {} in {}ms", retryCount, delay);

        vertx.setTimer(delay, timer -> {
            connectToChild(connectedChildId)
                    .onComplete(promise);
        });

        return promise.future();
    }

    private void handleConnectionClose() {
        logger.warn("WebSocket connection to child closed");
        connectionActive.set(false);
        cancelPendingMessage("Connection closed");
        if (retryCount < MAX_RETRIES) {
            scheduleReconnect();
        }
    }

    private void handleConnectionError(Throwable err) {
        logger.error("WebSocket error: {}", err.getMessage());
        connectionActive.set(false);
        cancelPendingMessage("Connection error: " + err.getMessage());
        if (activeWebSocket != null) {
            activeWebSocket.close();
        }
        if (retryCount < MAX_RETRIES) {
            scheduleReconnect();
        }
    }

    private void setupMessageHandler(WebSocket webSocket) {
        webSocket.textMessageHandler(msg -> {
            try {
                JsonObject message = new JsonObject(msg);
                String type = message.getString("type");

                if (type == null || !messageTypes.contains(type)) {
                    logger.error("Invalid message type: {}", msg);
                    return;
                }

                if (pendingMessagePromise != null && !pendingMessagePromise.future().isComplete()) {
                    pendingMessagePromise.complete(message);
                    pendingMessagePromise = null;
                } else {
                    logger.warn("Received unexpected message: {}", msg);
                }
            } catch (Exception e) {
                logger.error("Failed to parse message: {}", e.getMessage());
                cancelPendingMessage("Invalid message format");
            }
        });
    }

    public Future<JsonObject> waitForNextMessage() {
        if (pendingMessagePromise != null && !pendingMessagePromise.future().isComplete()) {
            return Future.failedFuture("Already waiting for a message");
        }

        pendingMessagePromise = Promise.promise();

        // Set timeout for message waiting
        vertx.setTimer(MESSAGE_TIMEOUT_MS, timer -> {
            if (!pendingMessagePromise.future().isComplete()) {
                cancelPendingMessage("Message timeout reached");
            }
        });

        return pendingMessagePromise.future();
    }

    private void cancelPendingMessage(String reason) {
        if (pendingMessagePromise != null && !pendingMessagePromise.future().isComplete()) {
            pendingMessagePromise.fail(reason);
            pendingMessagePromise = null;
        }
    }

    private Future<Void> sendJoinMessage(WebSocket webSocket) {
        JsonObject joinMessage = new JsonObject()
                .put("type", "join")
                .put("userId", userData.getId());
        return sendWebSocketMessage(joinMessage);
    }

    public Future<JsonObject> findChild(String childId) {
        return webClient.get("/find-child/" + childId)
                .putHeader("Authorization", "Bearer " + userData.getBearer())
                .timeout(5000)
                .send()
                .compose(response -> {
                    if (response.statusCode() == 200) {
                        return Future.succeededFuture(response.bodyAsJsonObject());
                    } else {
                        logger.error("Failed to find child (HTTP {}): {}",
                                response.statusCode(), response.bodyAsString());
                        return Future.failedFuture("HTTP " + response.statusCode());
                    }
                });
    }

    public Future<JsonObject> createChild() {
        return webClient.post("/deploy-child")
                .putHeader("Authorization", "Bearer " + userData.getBearer())
                .timeout(10000)
                .send()
                .compose(response -> {
                    if (response.statusCode() == 200) {
                        return Future.succeededFuture(response.bodyAsJsonObject());
                    } else {
                        logger.error("Failed to create child: {}", response.bodyAsString());
                        return Future.failedFuture("HTTP " + response.statusCode());
                    }
                });
    }

    private void startHeartbeat() {
        heartbeatTimer = vertx.setPeriodic(1000, handler -> {
            if (!connectionActive.get()) return;

            webClient.get(port, "localhost", "/child-health/" + connectedChildId)
                    .timeout(3000)
                    .send()
                    .onSuccess(response -> logger.debug("Heartbeat OK"))
                    .onFailure(throwable -> {
                        logger.error("Heartbeat failed: {}", throwable.getMessage());
                        handleConnectionError(throwable);
                    });
        });
    }

    public Future<HttpResponse<Buffer>> sendToChild(JsonObject message) {
        if (!connectionActive.get()) {
            return Future.failedFuture("Not connected to child");
        }

        return webClient.post("/message-child/" + connectedChildId)
                .putHeader("Authorization", "Bearer " + userData.getBearer())
                .timeout(5000)
                .sendJson(message);
    }

    public Future<Void> sendWebSocketMessage(JsonObject message) {
        if (activeWebSocket == null || activeWebSocket.isClosed()) {
            return Future.failedFuture("WebSocket not connected");
        }

        Promise<Void> promise = Promise.promise();

        activeWebSocket.writeTextMessage(message.encode(), result -> {
            if (result.succeeded()) {
                logger.debug("Message sent successfully: {}", message);
                promise.complete();
            } else {
                logger.error("Failed to send message: {}", result.cause().getMessage());
                handleConnectionError(result.cause());
                promise.fail(result.cause());
            }
        });

        return promise.future();
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        logger.info("Stopping ClientVerticle...");

        if (activeWebSocket != null) {
            activeWebSocket.close();
        }

        vertx.cancelTimer(heartbeatTimer);
        cancelPendingMessage("Client stopped");

        if (webClient != null) {
            webClient.close();
        }

        stopPromise.complete();
    }

    // Getters and Setters
    public UserData getUserData() {
        return userData;
    }

    public void setUserData(UserData userData) {
        this.userData = userData;
    }

    public String getConnectedChildId() {
        return connectedChildId;
    }

    public void setConnectedChildId(String connectedChildId) {
        this.connectedChildId = connectedChildId;
    }

    public boolean isConnectionActive() {
        return connectionActive.get();
    }
}