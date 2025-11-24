package com.example.verticles;

import com.example.configuration.KeyUtils;
import com.example.configuration.VertxRateLimiterHandler;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;

public class ParentVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(ParentVerticle.class);
    private final int PORT = 8081;
    private final int CHILD_PORT = 8082;
    private AsyncMap<String, JsonObject> childMap;
    private JWTAuth authProvider;
    private final RateLimiterConfig config = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .limitForPeriod(1000)
            .timeoutDuration(Duration.ofMillis(100))
            .build();
    private final RateLimiterRegistry registry = RateLimiterRegistry.of(config);
    private final RateLimiter rateLimiter = registry.rateLimiter("apiLimiter");

    @Override
    public void start(Promise<Void> startPromise) {
        KeyUtils.readPemFile(vertx, "src/main/resources/publicKey.pem")
                .thenAccept(publicKey -> {
                    authProvider = JWTAuth.create(vertx, new JWTAuthOptions()
                            .addPubSecKey(new PubSecKeyOptions()
                                    .setAlgorithm("RS256")
                                    .setBuffer(publicKey)));
                    vertx.sharedData().<String, JsonObject>getAsyncMap("children", res -> {
                        if (res.failed()) {
                            startPromise.fail(res.cause());
                            return;
                        }
                        childMap = res.result();
                        initializeServer(startRoute -> {
                            if (startRoute.succeeded()) {
                                startPromise.complete();
                            } else {
                                startPromise.fail(startRoute.cause());
                            }
                        });
                    });
                }).exceptionally(ex -> {
                    startPromise.fail("Could not read public key file");
                    return null;
                });
    }

    private void initializeServer(Handler<AsyncResult<Void>> completionHandler) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route().handler(VertxRateLimiterHandler.of(rateLimiter));
        router.post("/deploy-child").handler(JWTAuthHandler.create(authProvider)).handler(this::handleDeployChild);
        router.delete("/undeploy-child/:id").handler(JWTAuthHandler.create(authProvider)).produces("application/json").handler(this::handleUndeployChild);
        router.delete("/undeploy-children").handler(JWTAuthHandler.create(authProvider)).handler(this::undeployAll);
        router.get("/children").handler(JWTAuthHandler.create(authProvider)).handler(this::handleListChildren);
        router.get("/find-child/:id").handler(JWTAuthHandler.create(authProvider)).produces("application/json").handler(this::handleFindChild);
        router.get("/health").handler(ctx -> ctx.response().end("OK"));

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(PORT)
                .onSuccess(server -> {
                    logger.info("Parent verticle is ready on port {}", PORT);
                    completionHandler.handle(Future.succeededFuture());
                })
                .onFailure(err -> {
                    logger.error("Failed to start server", err);
                    completionHandler.handle(Future.failedFuture(err));
                });
    }

    private void handleDeployChild(RoutingContext ctx) {
        String childId = UUID.randomUUID().toString();
        ChildVerticle childVerticle = new ChildVerticle(childId, CHILD_PORT);
        vertx.deployVerticle(childVerticle)
                .compose(deploymentId -> {
                    JsonObject childInfo = new JsonObject()
                            .put("deploymentId", deploymentId)
                            .put("port", childVerticle.getPort());
                    return childMap.put(childId, childInfo);
                })
                .onSuccess(v -> {
                    JsonObject response = new JsonObject()
                            .put("childId", childId)
                            .put("status", "deployed");
                    sendJsonResponse(ctx, 200, response);
                    logger.info("Child info: {}", childVerticle);
                })
                .onFailure(err -> {
                    logger.error("Failed to deploy child", err);
                    sendErrorResponse(ctx, 500, "Failed to deploy child");
                });
    }

    private void handleFindChild(RoutingContext ctx) {
        String childId = ctx.pathParam("id");
        childMap.get(childId)
                .compose(childInfo -> {
                    if (childInfo == null) {
                        return Future.failedFuture("Child not found");
                    }
                    String deploymentId = childInfo.getString("deploymentId");
                    if (vertx.deploymentIDs().contains(deploymentId)) {
                        int port = childInfo.getInteger("port");
                        JsonObject response = new JsonObject()
                                .put("port", port)
                                .put("childId", childId)
                                .put("deploymentId", deploymentId);
                        return Future.succeededFuture(response);
                    } else {
                        return childMap.remove(childId)
                                .flatMap(v -> Future.failedFuture("Child not found (stale entry)"));
                    }
                })
                .onSuccess(response -> sendJsonResponse(ctx, 200, response))
                .onFailure(err -> {
                    if (err.getMessage().equals("Child not found") || err.getMessage().contains("stale entry")) {
                        sendErrorResponse(ctx, 404, "Child not found");
                    } else {
                        logger.error("Error finding child", err);
                        sendErrorResponse(ctx, 500, "Internal server error");
                    }
                });
    }


    private void handleUndeployChild(RoutingContext ctx) {
        String childId = ctx.pathParam("id");
        childMap.get(childId)
                .compose(childInfo -> {
                    if (childInfo == null) {
                        return Future.failedFuture("Child not found");
                    }
                    String deploymentId = childInfo.getString("deploymentId");
                    return vertx.undeploy(deploymentId)
                            .recover(err -> {
                                if (err.getMessage().contains("Unknown deployment")) {
                                    return Future.succeededFuture();
                                } else {
                                    return Future.failedFuture(err);
                                }
                            })
                            .compose(v -> childMap.remove(childId));
                })
                .onSuccess(v -> {
                    JsonObject response = new JsonObject()
                            .put("childId", childId)
                            .put("status", "undeployed");
                    sendJsonResponse(ctx, 200, response);
                })
                .onFailure(err -> {
                    logger.error("Failed to undeploy child", err);
                    sendErrorResponse(ctx, 404, err.getMessage());
                });
    }

    private void undeployAll(RoutingContext ctx) {
        childMap.entries()
                .compose(entries -> {
                    Future<Integer> countFuture = Future.succeededFuture(entries.size());
                    entries.forEach((childId, childInfo) -> {
                        String deploymentId = childInfo.getString("deploymentId");
                        vertx.undeploy(deploymentId)
                                .compose(v -> childMap.remove(childId));
                    });
                    return countFuture;
                })
                .onSuccess(count -> {
                    JsonObject response = new JsonObject()
                            .put("status", "success")
                            .put("undeployedCount", count);
                    sendJsonResponse(ctx, 200, response);
                })
                .onFailure(err -> {
                    logger.error("Failed to undeploy all children", err);
                    sendErrorResponse(ctx, 500, "Internal server error");
                });
    }

    private void handleListChildren(RoutingContext ctx) {
        childMap.entries()
                .onSuccess(entries -> {
                    JsonObject response = new JsonObject();
                    entries.forEach(response::put);
                    sendJsonResponse(ctx, 200, response);
                })
                .onFailure(err -> {
                    logger.error("Failed to list children", err);
                    sendErrorResponse(ctx, 500, "Internal server error");
                });
    }

    private void sendJsonResponse(RoutingContext ctx, int statusCode, JsonObject response) {
        ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(response.encode());
    }

    private void sendErrorResponse(RoutingContext ctx, int statusCode, String message) {
        JsonObject errorResponse = new JsonObject()
                .put("error", message)
                .put("status", statusCode);
        sendJsonResponse(ctx, statusCode, errorResponse);
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        logger.info("Parent verticle stopping");
        stopPromise.complete();
    }
}
