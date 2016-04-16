package ru.spb.kupchinolabs.omnidrive;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;

import java.util.UUID;

public class YandexVertical extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        vertx.eventBus().consumer(Constants.QUEUE_REQUEST_URL, mes -> {
            final JsonObject body = (JsonObject) mes.body();
            addFile(body);
        });
    }

    private void addFile(JsonObject json) {
        System.out.println("Getting file URL");
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));

        HttpClientRequest request = httpClient.get(443, "cloud-api.yandex.net", "/v1/disk/resources/download?path="+json.getString("from"), res -> {
            System.out.println("Received URL");
            System.out.println(res);
            res.bodyHandler(body -> {
                System.out.println("Got data " + body.toString());
                final String href = new JsonObject(body.toString()).getString("href");

                final String uuid = UUID.randomUUID().toString();
                vertx.sharedData().getLocalMap("urls").put(uuid, href);
                vertx.eventBus().publish(Constants.QUEUE_REQUEST_FILE, json.copy().put("uuid", uuid));
            });
        });

        request.putHeader("Authorization", "OAuth c602f505135842bbbdcc2cbfa91ac776");
        request.end();
    }

}
