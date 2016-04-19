package ru.spb.kupchinolabs.omnidrive;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

import java.util.Map;

import static ru.spb.kupchinolabs.omnidrive.Constants.DROPBOX_KEY;

/**
 * Created by inikolaev on 18/04/16.
 */
public class DropboxSource implements Source {

    private final Vertx vertx;
    private final String path;

    public DropboxSource(Vertx vertx, final String path) {
        this.vertx = vertx;
        this.path = path;
    }

    @Override
    public void start(Handler<SourceStream<Buffer>> handler) {
        makeDropboxRequest("/2/files/download", handler);
    }

    private void makeDropboxRequest(String uri, Handler<SourceStream<Buffer>> handler) {
        System.out.println("Downloading from path: " + path);
        HttpClientRequest request = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true))
                .post(443, "content.dropboxapi.com", uri, res -> {
                    System.out.println("Dropbox status code: " + res.statusCode());

                    System.out.println("Headers:");
                    for (Map.Entry<String, String> header : res.headers().entries()) {
                        System.out.println("    " + header.getKey() + ": " + header.getValue());
                    }

                    if (res.statusCode() == 200) {
                        final JsonObject json = new JsonObject(res.headers().get("Dropbox-API-Result"));
                        final String name = json.getString("name");

                        handler.handle(new SourceStream(new FileMetadata(name), res.pause()));
                    }
                })
                .setChunked(true)
                .putHeader("Authorization", "Bearer " + DROPBOX_KEY)
                .putHeader("Dropbox-API-Arg", "{\"path\": \"" + path + "\"}");

        request.end();
    }
}
