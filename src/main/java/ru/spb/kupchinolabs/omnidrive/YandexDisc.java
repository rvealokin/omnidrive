package ru.spb.kupchinolabs.omnidrive;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.Pump;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.io.File;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class YandexDisc extends AbstractVerticle {

    private final static Logger log = Logger.getLogger(YandexDisc.class.getName());

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);
        router.get("/file/:id").handler(this::pumpFileById);
        router.get("/file").handler(this::addFile);
        vertx.createHttpServer().requestHandler(router::accept).listen(8282);
    }

    private void pumpFileById(RoutingContext context) {
        final HttpServerRequest req = context.request();
        System.out.println("Got request " + req.uri());

        for (String name : req.headers().names()) {
            System.out.println(name + ": " + req.headers().get(name));
        }

//          req.handler(data -> System.out.println("Got data " + data.toString("ISO-8859-1")));

        req.pause();
        String filename = System.getProperty("user.home") + File.separator + "abcd1234";
        System.out.println(filename);
        vertx.fileSystem().open(filename, new OpenOptions(), ares -> {
            AsyncFile file = ares.result();
            if (ares.succeeded()) {
                Pump pump = Pump.pump(file, req.response());
                file.endHandler((v) -> {
                    req.response().end();
                });
//                req.endHandler(v1 -> file.close(v2 -> {
//                    System.out.println("Uploading from " + filename);
//                    req.response().end();
//                }));
                req.response().setChunked(true);
                pump.start();
            } else {
                log.log(Level.WARNING, ares.cause().getMessage());
                req.response().setStatusCode(404);
            }
            req.resume();
        });

    }

    private void addFile(RoutingContext routingContext) {
        System.out.println("Getting file URL");
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));

        HttpClientRequest request = httpClient.get(443, "cloud-api.yandex.net", "/v1/disk/resources/download?path=%D0%93%D0%BE%D1%80%D1%8B.jpg", res -> {
            System.out.println("Received URL");
            System.out.println(res);
            res.bodyHandler(body -> {
                System.out.println("Got data " + body.toString());
                final String href = new JsonObject(body.toString()).getString("href");

                final String uuid = UUID.randomUUID().toString();
                vertx.sharedData().getLocalMap("urls").put(uuid, href);
                routingContext.response().end();
            });
        });

        request.putHeader("Authorization", "OAuth c602f505135842bbbdcc2cbfa91ac776");
        request.end();
    }
}