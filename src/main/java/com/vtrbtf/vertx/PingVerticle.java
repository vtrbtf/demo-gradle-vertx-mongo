/*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 *
 */
package com.vtrbtf.vertx;


import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class PingVerticle extends Verticle {

  public void start() {
    // load the general config object, loaded by using -config on command line
    JsonObject appConfig = container.config();

    // deploy the mongo-persistor module, which we'll use for persistence
    container.deployModule(appConfig.getObject("mongo-persistor").getString("mod"), appConfig.getObject("mongo-persistor"));

    // create and run the server
    vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
      @Override
      public void handle(final HttpServerRequest httpServerRequest) {

        // we send the response from the mongo query back to the client.
        // first create the query
        JsonObject matcher = new JsonObject().putString("name", "Vitao");
        JsonObject json = new JsonObject().putString("collection", "zips")
                .putString("action", "save")
                .putObject("document", matcher);

        // send it over the bus
        vertx.eventBus().send("mongodb-persistor", json, new Handler<Message<JsonObject>>() {

          @Override
          public void handle(Message<JsonObject> message) {
            // send the response back, encoded as string
            httpServerRequest.response().end(message.body().encodePrettily());
          }
        });
      }
    }).listen(8888);

    // output that the server is started
    container.logger().info("Webserver started, listening on port: 8888");
  }
}