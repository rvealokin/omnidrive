package ru.spb.kupchinolabs.omnidrive;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.streams.WriteStream;

import static ru.spb.kupchinolabs.omnidrive.Constants.DROPBOX_KEY;

/**
 * Created by inikolaev on 18/04/16.
 */
public class DropboxDestination implements Destination {

    private final Vertx vertx;
    private final String path;

    public DropboxDestination(Vertx vertx, final String path) {
        this.vertx = vertx;
        this.path = path;
    }

    @Override
    public void start(FileMetadata metadata, Handler<WriteStream<Buffer>> handler) {
        makeDropboxRequest("/2/files/upload", handler);
    }

    private void makeDropboxRequest(String uri, Handler<WriteStream<Buffer>> handler) {
        System.out.println("Uploading to path: " + path);
        HttpClientRequest request = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true))
                .post(443, "content.dropboxapi.com", uri, res -> {
                    System.out.println("Dropbox status code: " + res.statusCode());

                    res.bodyHandler(body -> {
                        System.out.println(body);
                    });
                })
                .setChunked(true)
                .putHeader("Authorization", "Bearer " + DROPBOX_KEY)
                .putHeader("Content-type", "application/octet-stream")
                .putHeader("Dropbox-API-Tag", "{\"path\": \"" + path + "\"}");

        handler.handle(request);
    }
}
