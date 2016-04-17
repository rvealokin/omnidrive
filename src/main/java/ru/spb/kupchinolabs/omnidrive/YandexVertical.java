package ru.spb.kupchinolabs.omnidrive;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;

import java.util.UUID;

import static ru.spb.kupchinolabs.omnidrive.Constants.YANDEX_OAUTH_KEY;

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
        requestFileMetadata(json);
    }

    private void requestFileMetadata(JsonObject json) {
        makeYandexRequest("/v1/disk/resources?path=" + json.getString("from") + "&fields=mime_type%2Csize%2Cname", metadataResponse -> {
            if (metadataResponse.statusCode() == 200) {
                metadataResponse.bodyHandler(metadataBody -> {
                    final JsonObject metadata = new JsonObject(metadataBody.toString());
                    final Integer size = metadata.getInteger("size");
                    final String mimeType = metadata.getString("mime_type");
                    final String name = metadata.getString("name");

                    requestFileDownloadLink(json.copy().put("name", name).put("type", mimeType).put("size", size));
                });
            }
        });
    }

    private void requestFileDownloadLink(JsonObject json) {
        System.out.println("Requesting download link for file: " + json);

        makeYandexRequest("/v1/disk/resources/download?path="+json.getString("from"), res -> {
            if (res.statusCode() == 200) {
                res.bodyHandler(body -> {
                    System.out.println("Got data " + body.toString());
                    final String href = new JsonObject(body.toString()).getString("href");

                    final String uuid = UUID.randomUUID().toString();
                    vertx.sharedData().getLocalMap("urls").put(uuid, href);
                    vertx.eventBus().publish(Constants.QUEUE_REQUEST_FILE, json.copy().put("uuid", uuid));
                });
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
