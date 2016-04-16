package ru.spb.kupchinolabs.omnidrive;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.logging.Logger;

import static ru.spb.kupchinolabs.omnidrive.Constants.INBOUD_QUEUE;

/**
 * Created by inikolaev on 16/04/16.
 */
public class ClientVertical extends AbstractVerticle {
    private final static Logger log = Logger.getLogger(ClientVertical.class.getName());

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        router.post("/copy").handler(this::copyFile);
        vertx.createHttpServer().requestHandler(router::accept).listen(9090);
    }

    private void copyFile(RoutingContext routingContext) {
        routingContext.request().bodyHandler(body -> {
            log.info("Copying file: " + body);

            JsonObject json = new JsonObject(body.toString());

            vertx.eventBus().publish(INBOUD_QUEUE, json);
            routingContext.request().response()
                    .putHeader("Content-length", "2")
                    .write("OK")
                    .end();
        });
    }
}
