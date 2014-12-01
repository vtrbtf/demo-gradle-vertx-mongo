package com.vtrbtf.vertx

import org.vertx.groovy.core.http.RouteMatcher
import org.vertx.groovy.platform.Verticle
import org.vertx.java.core.json.JsonObject

class MongoVerticle extends Verticle{

    def start() {
        JsonObject appConfig = container.config;

        container.deployModule(appConfig.getObject("mongo-persistor").getString("module"), appConfig.getObject("mongo-persistor").toMap());

        RouteMatcher rm = new RouteMatcher();

        rm.get("/person" , { req ->
            sendToMongo( mongoMsg(["action" : "find"]), { msg ->
                req.response.end(new JsonObject(msg.body()).toString());
            })
        })

        rm.get("/person/:id" , { req ->
            sendToMongo( mongoMsg(["action" : "find" , "matcher" : ["_id" : req.params.get("id")]]), { msg ->
                req.response.end(new JsonObject(msg.body()).toString());
            })
        })

        rm.post("/person" , { req ->
            sendToMongo( mongoMsg(["action" : "save" , "document" : req.params.names.collect({ k -> [ k : req.params.get(k)]}).inject([:]){ result , c -> result + c }]) , { msg ->
                req.response.end(new JsonObject(msg.body()).toString()  );
            })
        })

        rm.delete("/person/:id" , { req ->
            sendToMongo( mongoMsg(["action" : "delete" , "matcher" : ["_id" : req.params.get("id")]]), { msg ->
                req.response.end(new JsonObject(msg.body()).toString()  );
            })
        })

        vertx.createHttpServer().requestHandler( rm.asClosure() ).listen(8080);
        container.logger.info("Verticle started, listening on port: 8080");
    }

    def mongoMsg = { params ->
        return new JsonObject(["collection" : "person"] + params)
    }

    private sendToMongo = { json , messageCls ->
        vertx.eventBus.send("mongodb-persistor", json) { message ->
            messageCls(message)
        }
    }
}
