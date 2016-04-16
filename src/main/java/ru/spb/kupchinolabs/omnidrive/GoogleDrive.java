package ru.spb.kupchinolabs.omnidrive;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.streams.Pump;

import java.io.File;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class GoogleDrive extends AbstractVerticle {

    private final static Logger log = Logger.getLogger(GoogleDrive.class.getName());

    @Override
    public void start() throws Exception {
        HttpClientRequest request = vertx.createHttpClient(new HttpClientOptions()).get(8080, "localhost", "/file/abcd1234", resp -> {
            System.out.println("Got response " + resp.statusCode());
//            resp.bodyHandler(body -> {
//                final String fileName = "abcd1234_" + new Date().getTime();
//                vertx.fileSystem().writeFile(System.getProperty("user.home") + File.pathSeparator + fileName, body, new Handler<AsyncResult<Void>>() {
//                    @Override
//                    public void handle(AsyncResult<Void> event) {
//                        if (event.succeeded()) {
//                            log.log(Level.INFO, "file created - " + fileName);
//                        } else {
//                            log.log(Level.SEVERE, "file " + fileName + " NOT created - " + event.cause().getMessage());
//                        }
//                    }
//                });
//            });

            String filename = System.getProperty("user.home") + File.separator + UUID.randomUUID() + ".uploaded";
            resp.pause();
            vertx.fileSystem().open(filename, new OpenOptions(), ares -> {
                AsyncFile file = ares.result();
                Pump pump = Pump.pump(resp, file);
                resp.endHandler(v1 -> file.close(v2 -> {
                    System.out.println("Uploaded to " + filename);
                }));
                resp.resume();
                pump.start();
            });

        });
        request.end();
    }
}
