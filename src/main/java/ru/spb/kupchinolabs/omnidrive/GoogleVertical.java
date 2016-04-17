package ru.spb.kupchinolabs.omnidrive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.common.collect.ImmutableMap;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.Pump;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
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
        final String uuid = json.getString("uuid");
        final String to = json.getString("to");
        final String href = String.valueOf(vertx.sharedData().getLocalMap("urls").get(uuid));

        executeGetRequest(href, ImmutableMap.of("Authorization", "OAuth c602f505135842bbbdcc2cbfa91ac776"), res -> {
            submitFileMultipart(json, res);
        });
    }

    private void submitFile(JsonObject json, HttpClientResponse yandexResponse) {
        System.out.println("Submitting file");
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));

        Credential credential = null;
        try {
            credential = GoogleDrive.authorize();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Credential: " + credential.getAccessToken());

        HttpClientRequest request = httpClient.post(443, "www.googleapis.com", "/upload/drive/v3/files?uploadType=media&name=IgorTest")
                .setChunked(true)
                .putHeader("Authorization", "Bearer " + credential.getAccessToken())
                .putHeader("Content-type", json.getString("type"))
                .putHeader("Content-size", Objects.toString(json.getInteger("size")))
                .handler(res -> {
                    res.bodyHandler(body -> {
                        System.out.println("Response from Google: ");
                        System.out.println("    Status code: " + res.statusCode());
                        System.out.println("           Body: " + body);
                    });
                });

        Pump.pump(yandexResponse, request).start();

        yandexResponse.endHandler(v -> request.end());
    }

    private void submitFileMultipart(JsonObject json, HttpClientResponse yandexResponse) {
        System.out.println("Submitting file");
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));

        Credential credential = null;
        try {
            credential = GoogleDrive.authorize();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Credential: " + credential.getAccessToken());

        String multipartBoundary = UUID.randomUUID().toString();

        String metadataContentType = "Content-Type: application/json; charset=UTF-8\r\n\r\n";
        String metadata = "{\"name\": \"" + json.getString("name") + "\"}\r\n";
        String fileContentType = "Content-Type: " + json.getString("type") + "\r\n\r\n";

        int contentSize = json.getInteger("size")
                + 3 * multipartBoundary.length() + 14
                + metadataContentType.length()
                + metadata.length()
                + fileContentType.length();

        System.out.println("File length:                  " + json.getInteger("size"));
        System.out.println("Boundary length:              " + multipartBoundary.length());
        System.out.println("Metadata content type length: " + metadataContentType.length());
        System.out.println("Metadata length:              " + metadata.length());
        System.out.println("File content type length:     " + fileContentType.length());
        System.out.println("Content-length:               " + contentSize);

        HttpClientRequest request = httpClient.post(443, "www.googleapis.com", "/upload/drive/v3/files?uploadType=multipart")
                .setChunked(true)
                .putHeader("Authorization", "Bearer " + credential.getAccessToken())
                .putHeader("Content-type", "multipart/related; boundary=\"" + multipartBoundary + "\"")
                .putHeader("Content-size", String.valueOf(contentSize))
                .handler(res -> {
                    res.bodyHandler(body -> {
                        System.out.println("Response from Google: ");
                        System.out.println("    Status code: " + res.statusCode());
                        System.out.println("           Body: " + body);
                    });
                });

        request.write("--").write(multipartBoundary).write("\r\n");
        request.write(metadataContentType);
        request.write(metadata);

        request.write("--").write(multipartBoundary).write("\r\n");
        request.write(fileContentType);

        //request.write("test");
        //request.write("\r\n--").write(multipartBoundary).write("--");
        //request.end();

        System.out.printf("%s\r\n%s%s%s\r\n%s%s--", multipartBoundary, metadataContentType, metadata, multipartBoundary, fileContentType, multipartBoundary);
        Pump.pump(yandexResponse, request).start();

        yandexResponse.endHandler(v -> {
            request.write("\r\n--").write(multipartBoundary).write("--");
            request.end();
        });
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

    public void executeGoogleRequest() {

    }
}
