package ru.spb.kupchinolabs.omnidrive;

import com.google.api.client.auth.oauth2.Credential;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.WriteStream;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/**
 * Created by inikolaev on 18/04/16.
 */
public class GoogleDestination implements Destination {

    private final Vertx vertx;
    private final String path;

    public GoogleDestination(final Vertx vertx, final String path) {
        this.vertx = vertx;
        this.path = path;
    }

    @Override
    public void start(Handler<WriteStream<Buffer>> handler) {
        final JsonObject json = new JsonObject();
        json.put("name", "IgorTest");
        json.put("type", "image/jpeg");
        json.put("size", 1762478);

        submitFileMultipart(json, handler);
    }

    private void submitFileMultipart(JsonObject json, Handler<WriteStream<Buffer>> handler) {
        System.out.println("Submitting file");

        try {
            Credential credential = GoogleDrive.authorize();

            System.out.println("Credential: " + credential.getAccessToken());

            final String multipartBoundary = UUID.randomUUID().toString();

            String metadataContentType = "Content-Type: application/json; charset=UTF-8\r\n\r\n";
            String metadata = "{\"name\": \"" + path + "\"}\r\n";
//            String metadata = "{\"name\": \"" + json.getString("name") + "\"}\r\n";
            String fileContentType = "Content-Type: application/octet-stream\r\n\r\n";
//            String fileContentType = "Content-Type: " + json.getString("type") + "\r\n\r\n";

//            int contentSize = json.getInteger("size")
//                    + 3 * multipartBoundary.length() + 14
//                    + metadataContentType.length()
//                    + metadata.length()
//                    + fileContentType.length();

//            System.out.println("File length:                  " + json.getInteger("size"));
            System.out.println("Boundary length:              " + multipartBoundary.length());
            System.out.println("Metadata content type length: " + metadataContentType.length());
            System.out.println("Metadata length:              " + metadata.length());
//            System.out.println("File content type length:     " + fileContentType.length());
//            System.out.println("Content-length:               " + contentSize);

            HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true));
            final HttpClientRequest request = httpClient.post(443, "www.googleapis.com", "/upload/drive/v3/files?uploadType=multipart")
                    .setChunked(true)
                    .putHeader("Authorization", "Bearer " + credential.getAccessToken())
                    .putHeader("Content-type", "multipart/related; boundary=\"" + multipartBoundary + "\"")
                    //.putHeader("Content-size", String.valueOf(contentSize))
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

            handler.handle(new WriteStreamWrapper<Buffer>(request) {
                @Override
                public void end() {
                    System.out.println("Closing request...");
                    request.write("\r\n--").write(multipartBoundary).write("--");
                    request.end();
                    System.out.println("Request closed");
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
