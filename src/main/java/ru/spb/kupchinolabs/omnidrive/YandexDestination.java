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
import io.vertx.core.streams.WriteStream;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import static ru.spb.kupchinolabs.omnidrive.Constants.YANDEX_OAUTH_KEY;

/**
 * Created by inikolaev on 18/04/16.
 */
public class YandexDestination implements Destination {

    private final Vertx vertx;
    private final String path;

    public YandexDestination(final Vertx vertx, final String path) {
        this.vertx = vertx;
        this.path = path;
    }

    @Override
    public void start(FileMetadata metadata, Handler<WriteStream<Buffer>> handler) {
        requestFileUploadLink(handler);
    }

    private void requestFileUploadLink(Handler<WriteStream<Buffer>> handler) {
        System.out.println("Requesting upload link for file: " + path);

        makeYandexRequest("/v1/disk/resources/upload?path=" + path + "&overwrite=true", res -> {
            System.out.println("Received response with status " + res.statusCode());

            if (res.statusCode() == 200) {
                res.bodyHandler(body -> {
                    System.out.println("Got data " + body.toString());
                    final String href = new JsonObject(body.toString()).getString("href");

                    executePutRequest(href, Collections.emptyMap(), stream -> {
                        System.out.println("Uploading file");
                        handler.handle(stream);
                    });
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

    private void executePutRequest(String href, Map<String, String> headers, Handler<WriteStream> handler) {
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));

        try {
            final URL url = new URL(href);
            final String query = url.getQuery() != null ? "?" + url.getQuery() : "";

            HttpClientRequest request = httpClient.put(url.getDefaultPort(), url.getHost(), url.getPath() + query, res -> {
                System.out.println("Received response with status " + res.statusCode());
            });

            request.setChunked(true);
            request.headers().addAll(headers);

            handler.handle(request);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
