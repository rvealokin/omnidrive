package ru.spb.kupchinolabs.omnidrive;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.Pump;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

public class GoogleVertical extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        vertx.eventBus().consumer(Constants.QUEUE_REQUEST_FILE, mes -> {
            final JsonObject body = (JsonObject) mes.body();
            getFile(body);
        });
    }

    private void getFile(JsonObject json) {
        System.out.println("Getting file");
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));
        final String uuid = json.getString("uuid");
        final String to = json.getString("to");
        final String href = String.valueOf(vertx.sharedData().getLocalMap("urls").get(uuid));
        try {
            final URL url = new URL(href);
            final String query = url.getQuery() != null ? "?" + url.getQuery() : "";
            HttpClientRequest request = httpClient.get(url.getDefaultPort(), url.getHost(), url.getPath() + query, res -> {
                System.out.println("Received URL");
                System.out.println(res);
//                res.bodyHandler(body -> {
//                    System.out.println("Got data " + body.toString());
//                    final String href = new JsonObject(body.toString()).getString("href");
//
//                    final String uuid = UUID.randomUUID().toString();
//                    vertx.sharedData().getLocalMap("urls").put(uuid, href);
//                    vertx.eventBus().publish(Constants.QUEUE_REQUEST_FILE, json.copy().put("uuid", uuid));
//                });
                submitFile(json, res);
            });

            request.putHeader("Authorization", "OAuth c602f505135842bbbdcc2cbfa91ac776");
            request.end();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void submitFile(JsonObject json, HttpClientResponse yandexResponse) {
        System.out.println("Submitting file");
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));

        HttpClientRequest request = httpClient.post(443, "www.googleapis.com", "/upload/drive/v3/files?uploadType=media")
                .setChunked(true)
                .putHeader("Authorization", "Bearer test");

        Pump.pump(yandexResponse, request);

//                .end();

    }

}
