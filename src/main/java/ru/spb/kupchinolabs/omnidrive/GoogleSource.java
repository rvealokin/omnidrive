package ru.spb.kupchinolabs.omnidrive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.common.collect.ImmutableMap;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.streams.ReadStream;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Created by inikolaev on 18/04/16.
 */
public class GoogleSource implements Source {

    private final Vertx vertx;
    private final String path;

    public GoogleSource(final Vertx vertx, final String path) {
        this.vertx = vertx;
        this.path = path;
    }

    @Override
    public void start(Handler<SourceStream<Buffer>> handler) {
        requestFileMetadata(handler);
    }

    private void requestFileMetadata(Handler<SourceStream<Buffer>> handler) {
        googleGetRequest("/drive/v3/files/" + path, res -> {
            res.bodyHandler(body -> System.out.println(body.toString()));
            requestFile(new FileMetadata("test"), handler);
        });
    }

    private void requestFile(FileMetadata metadata, Handler<SourceStream<Buffer>> handler) {
        googleGetRequest("/drive/v3/files/" + path + "?alt=media", res -> {
            handler.handle(new SourceStream(metadata, res.pause()));
        });
    }

    private void googleGetRequest(String href, Handler<HttpClientResponse> handler) {
        try {
            Credential credential = GoogleDrive.authorize();
            executeGetRequest("https://www.googleapis.com" + href, ImmutableMap.of("Authorization", "Bearer " + credential.getAccessToken()), handler);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
