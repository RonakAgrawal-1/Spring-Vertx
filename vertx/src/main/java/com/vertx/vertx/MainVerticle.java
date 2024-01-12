package com.vertx.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start() {
        Router router = createRouter();

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8081, result -> {
                    if (result.succeeded()) {
                        System.out.println("Server started on port 8081");
                    } else {
                        System.err.println("Server failed to start");
                        result.cause().printStackTrace();
                    }
                });
    }

    private Router createRouter() {
        Router router = Router.router(vertx);
        router.get("/api/hello").handler(routingContext -> {
            routingContext.response()
                    .putHeader("content-type", "text/plain")
                    .end("Hello, I am Vert.x!");
        });
        return router;
    }
}
