package com.example;

import com.example.verticles.ParentVerticle;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@QuarkusMain
public class MainApplication implements QuarkusApplication {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public int run(String... args) {
        logger.info(System.getProperty("user.dir"));
        DeploymentOptions deploymentOptions = new DeploymentOptions()
                .setInstances(10)
                .setWorkerPoolSize(20);
        Vertx.vertx().deployVerticle(ParentVerticle.class, deploymentOptions)
                .onSuccess(id -> logger.info("OK"))
                .onFailure(err -> {
                    logger.error("Deployment failed: {}", err.getMessage());
                    Quarkus.asyncExit(1);
                });

        Quarkus.waitForExit();
        return 0;
    }

    public static void main(String... args) {
        Quarkus.run(MainApplication.class, args);
    }
}
