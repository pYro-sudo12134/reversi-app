package com.example.configuration;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class KeyUtils {
    public static CompletableFuture<String> readPemFile(Vertx vertx, String path) {
        CompletableFuture<String> future = new CompletableFuture<>();
        FileSystem fs = vertx.fileSystem();

        fs.readFile(path, res -> {
            if (res.succeeded()) {
                future.complete(res.result().toString());
            } else {
                future.completeExceptionally(res.cause());
            }
        });
        return future;
    }

    public static CompletionStage<PrivateKey> readPrivateKey(Vertx vertx, String path) {
        Promise<PrivateKey> promise = Promise.promise();

        vertx.fileSystem().readFile(path, ar -> {
            if (ar.failed()) {
                promise.fail(ar.cause());
                return;
            }

            try {
                String pemContent = ar.result().toString();
                PrivateKey privateKey = decodePrivateKey(pemContent);
                promise.complete(privateKey);
            } catch (Exception e) {
                promise.fail(e);
            }
        });

        return promise.future().toCompletionStage();
    }

    public static PrivateKey decodePrivateKey(String pemContent) throws Exception {
        pemContent = pemContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] pkcs8Encoded = Base64.getDecoder().decode(pemContent);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Encoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }
}
