package com.example.configuration;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class VertxRateLimiterHandler implements Handler<RoutingContext> {

    private final RateLimiter rateLimiter;

    public VertxRateLimiterHandler(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    public static VertxRateLimiterHandler of(RateLimiter rateLimiter) {
        return new VertxRateLimiterHandler(rateLimiter);
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (rateLimiter.acquirePermission()) {
            ctx.next();
        } else {
            ctx.response()
                    .setStatusCode(429)
                    .end("Too many requests");
        }
    }
}