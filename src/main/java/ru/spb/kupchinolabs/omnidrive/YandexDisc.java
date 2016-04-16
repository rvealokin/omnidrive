package ru.spb.kupchinolabs.omnidrive;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.streams.Pump;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.io.File;
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

}