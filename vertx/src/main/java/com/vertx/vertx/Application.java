package com.vertx.vertx;

import io.vertx.core.Vertx;

public class Application {

	public static void main(String[] args) {
		Vertx vertx = Vertx.vertx();
		vertx.deployVerticle(new MainVerticle());
	}
}
