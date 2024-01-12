package com.vertx.vertx;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HelloController implements Handler<RoutingContext> {

    @Override
    public void handle(RoutingContext routingContext) {
        routingContext.response()
                .putHeader("content-type", "text/plain")
                .end("Hello, World!");
    }

    public static void mountSubRouter(Router parentRouter, String path) {
        Router subRouter = Router.router(Vertx.vertx());
        HelloController helloController = new HelloController();

        subRouter.get("/hello").handler(helloController);

        parentRouter.mountSubRouter(path, subRouter);
    }
}
