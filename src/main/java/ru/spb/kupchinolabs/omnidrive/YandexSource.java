package ru.spb.kupchinolabs.omnidrive;

import com.google.common.collect.ImmutableMap;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import static ru.spb.kupchinolabs.omnidrive.Constants.YANDEX_OAUTH_KEY;

/**
 * Created by inikolaev on 18/04/16.
 */
public class YandexSource implements Source {

    private final Vertx vertx;
    private final String path;

    public YandexSource(final Vertx vertx, final String path) {
        this.vertx = vertx;
        this.path = path;
    }

    @Override
    public void start(Handler<SourceStream<Buffer>> handler) {
        requestFileMetadata(handler);
    }

    private void requestFileMetadata(Handler<SourceStream<Buffer>> handler) {
        makeYandexRequest("/v1/disk/resources?path=" + path + "&fields=mime_type%2Csize%2Cname", metadataResponse -> {
            if (metadataResponse.statusCode() == 200) {
                metadataResponse.bodyHandler(metadataBody -> {
                    final JsonObject metadata = new JsonObject(metadataBody.toString());

                    requestFileDownloadLink(new FileMetadata(metadata.getString("name"), metadata.getString("type"), metadata.getLong("size")), handler);
                });
            }
        });
    }

    private void requestFileDownloadLink(FileMetadata metadata, Handler<SourceStream<Buffer>> handler) {
        System.out.println("Requesting download link for file: " + metadata);

        makeYandexRequest("/v1/disk/resources/download?path=" + path, res -> {
            if (res.statusCode() == 200) {
                res.bodyHandler(body -> {
                    System.out.println("Got data " + body.toString());
                    final String href = new JsonObject(body.toString()).getString("href");

                    executeGetRequest(href, ImmutableMap.of("Authorization", "OAuth " + YANDEX_OAUTH_KEY), file -> {
                        System.out.println("Downloading file of size: " + file.headers().get("Content-length"));
                        handler.handle(new SourceStream(metadata, file.pause()));
                    });

                    //final String uuid = UUID.randomUUID().toString();
                    //vertx.sharedData().getLocalMap("urls").put(uuid, href);
                    //vertx.eventBus().publish(Constants.QUEUE_REQUEST_FILE, json.copy().put("uuid", uuid));
                });
            }
        });
    }

    private void makeYandexRequest(String uri, Handler< HttpClientResponse > handler) {
        vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true))
                .get(443, "cloud-api.yandex.net", uri, handler)
                .putHeader("Authorization", "OAuth " + YANDEX_OAUTH_KEY)
                .end();
    }

    private void executeGetRequest(String href, Map<String, String> headers, Handler<HttpClientResponse> handler) {
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));

        try {
            final URL url = new URL(href);
            final String query = url.getQuery() != null ? "?" + url.getQuery() : "";

            HttpClientRequest request = httpClient.get(url.getDefaultPort(), url.getHost(), url.getPath() + query, res -> {
                switch (res.statusCode()) {
                    case 301:
                    case 302:
                        System.out.println("Received HTTP redirect... redirecting");
                        String location = res.headers().get("location");
                        executeGetRequest(location, headers, handler);
                        break;
                    default:
                        System.out.println("Received response with status " + res.statusCode());
                        handler.handle(res);
                        break;
                }
            });

            request.headers().addAll(headers);
            request.end();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
