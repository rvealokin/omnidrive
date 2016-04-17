package ru.spb.kupchinolabs.omnidrive;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.logging.Logger;

import static ru.spb.kupchinolabs.omnidrive.Constants.QUEUE_REQUEST_URL;
import static ru.spb.kupchinolabs.omnidrive.Constants.YANDEX_OAUTH_KEY;

/**
 * Created by inikolaev on 16/04/16.
 */
public class ClientVertical extends AbstractVerticle {
    private final static Logger log = Logger.getLogger(ClientVertical.class.getName());

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        router.post("/copy").handler(this::copyFile);
        router.get("/list").handler(this::listFiles);
        vertx.createHttpServer().requestHandler(router::accept).listen(9090);
    }

    private void copyFile(RoutingContext routingContext) {
        routingContext.response().putHeader("Access-Control-Allow-Origin", "*");

        routingContext.request().bodyHandler(body -> {
            log.info("Copying file: " + body);

            JsonObject json = new JsonObject(body.toString());

            vertx.eventBus().publish(QUEUE_REQUEST_URL, json);
            routingContext.request().response()
                    .putHeader("Content-length", "2")
                    .write("OK")
                    .end();
        });
    }

    private void listFiles(RoutingContext routingContext) {
        makeYandexRequest("/v1/disk/resources/files", res -> {
            routingContext.response().putHeader("Access-Control-Allow-Origin", "*");

            if (res.statusCode() == 200) {
                res.bodyHandler(body -> {
                    routingContext.response().end(body.toString());
                });
            } else {
                routingContext.response().end("{\"items\": []}");
            }
        });
    }

    private void makeYandexRequest(String uri, Handler<HttpClientResponse> handler) {
        vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true))
                .get(443, "cloud-api.yandex.net", uri, handler)
                .putHeader("Authorization", "OAuth " + YANDEX_OAUTH_KEY)
                .end();
    }
}
