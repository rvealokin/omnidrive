package ru.spb.kupchinolabs.omnidrive;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.streams.Pump;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/*
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class Proxy extends AbstractVerticle {

    @Override
    public void start() throws Exception {

        Router router = Router.router(vertx);
        router.get("/file/:id").handler(this::pumpFileById);
        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
    }

    private void pumpFileById(RoutingContext context) {
        final HttpServerRequest req = context.request();
        req.pause();
        String fileID = req.getParam("id");
        System.out.println("Proxying request: " + req.uri());
        HttpClient yandexClient = vertx.createHttpClient(new HttpClientOptions());
        HttpClientRequest c_req = yandexClient.request(req.method(), 8282, "localhost", req.uri(), c_res -> {
            System.out.println("Proxying response: " + c_res.statusCode());
            req.response().setChunked(true);
            req.response().setStatusCode(c_res.statusCode());
            req.response().headers().setAll(c_res.headers());
            final Pump pump = Pump.pump(c_res, req.response());
            c_res.endHandler((v) -> req.response().end());
            pump.start();
            req.resume();
        });
        c_req.setChunked(true);
        c_req.headers().setAll(req.headers());
        req.handler(data -> {
            System.out.println("Proxying request body " + data.toString("ISO-8859-1"));
            c_req.write(data);
        });
//        req.endHandler((v) -> c_req.end());
        c_req.end();
    }
}
