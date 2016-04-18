package ru.spb.kupchinolabs.omnidrive;

import com.google.common.base.Preconditions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.util.Objects;

/**
 * Downloader vertical which can download from some source and pump into some destination
 *
 * @author inikolaev
 */
public class DownloaderVertical extends AbstractVerticle {
    public static final int BUFFER_SIZE_1024K = 2 << 21;

    private static class LoggingWriteStreamAdapter implements WriteStream<Buffer> {
        private final WriteStream<Buffer> stream;
        private int written = 0;

        public LoggingWriteStreamAdapter(WriteStream<Buffer> stream) {
            this.stream = stream;
        }

        @Override
        public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
            return stream.exceptionHandler(handler);
        }

        @Override
        public WriteStream<Buffer> write(Buffer t) {
            written += t.length();
            System.out.println("Writing " + t.length() + " / " + written + " bytes");
            return stream.write(t);
        }

        @Override
        public void end() {
            stream.end();
        }

        @Override
        public WriteStream<Buffer> setWriteQueueMaxSize(int i) {
            return stream.setWriteQueueMaxSize(i);
        }

        @Override
        public boolean writeQueueFull() {
            return stream.writeQueueFull();
        }

        @Override
        public WriteStream<Buffer> drainHandler(Handler<Void> handler) {
            return stream.drainHandler(handler);
        }
    }

    private static class LoggingReadStreamAdapter implements ReadStream<Buffer> {
        private final ReadStream<Buffer> stream;
        private int read = 0;

        public LoggingReadStreamAdapter(ReadStream<Buffer> stream) {
            this.stream = stream;
        }

        @Override
        public ReadStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
            return stream.exceptionHandler(handler);
        }

        @Override
        public ReadStream<Buffer> handler(Handler<Buffer> handler) {
            return stream.handler(buffer -> {
                read += buffer.length();
                System.out.println("Read " + buffer.length() + " / " + read + " bytes");
                handler.handle(buffer);
            });
        }

        @Override
        public ReadStream<Buffer> pause() {
            return stream.pause();
        }

        @Override
        public ReadStream<Buffer> resume() {
            return stream.resume();
        }

        @Override
        public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
            return stream.endHandler(endHandler);
        }


    }

    @Override
    public void start() throws Exception {
        vertx.eventBus().consumer(Constants.QUEUE_DOWNLOAD, mes -> {
            final JsonObject body = (JsonObject) mes.body();
            final String sourceName = body.getString("from");
            final String destinationName = body.getString("to");

            final Source source = createSource(sourceName);
            final Destination destination = createDestination(destinationName);

            source.start(read -> {
                destination.start(write -> {
                    read.resume();

                    System.out.println("Beginning transmission");

                    Pump.pump(new LoggingReadStreamAdapter(read), new LoggingWriteStreamAdapter(write), BUFFER_SIZE_1024K).start();

                    read.endHandler(v -> {
                        write.end();
                        System.out.println("Transmission completed");
                    });
                });
            });
        });
    }

    private Source createSource(final String sourceName) {
        Preconditions.checkArgument(Objects.nonNull(sourceName), "Source name cannot be null");
        Preconditions.checkArgument(sourceName.trim().length() >= 5, "Source name cannot be empty");
        Preconditions.checkArgument(sourceName.trim().indexOf("://") > 3, "Source name should start with source type prefix");

        final String sourceType = sourceName.substring(0, sourceName.indexOf("://")).trim();

        switch (sourceType) {
            case "file": return handler -> {
                vertx.fileSystem().open(sourceName.substring(sourceName.indexOf("://") + 3).trim(), new OpenOptions().setWrite(false), result -> {
                    if (result.succeeded()) {
                        handler.handle(result.result());
                    }
                });
            };

            case "yandex": return new YandexSource(vertx, sourceName.substring(sourceName.indexOf("://") + 3).trim());
            case "google": return new GoogleSource(vertx, sourceName.substring(sourceName.indexOf("://") + 3).trim());

            default:
                throw new IllegalArgumentException("Unknown source type: " + sourceType);
        }
    }

    private Destination createDestination(final String destinationName) {
        Preconditions.checkArgument(Objects.nonNull(destinationName), "Source name cannot be null");
        Preconditions.checkArgument(destinationName.trim().length() >= 5, "Source name cannot be empty");
        Preconditions.checkArgument(destinationName.trim().indexOf("://") > 3, "Source name should start with source type prefix");

        final String destinationType = destinationName.substring(0, destinationName.indexOf("://")).trim();

        switch (destinationType) {
            case "file": return handler -> {
                vertx.fileSystem().open(destinationName.substring(destinationName.indexOf("://") + 3).trim(), new OpenOptions().setRead(false).setTruncateExisting(true), result -> {
                    if (result.succeeded()) {
                        handler.handle(result.result());
                    }
                });
            };

            case "yandex": return new YandexDestination(vertx, destinationName.substring(destinationName.indexOf("://") + 3).trim());
            case "google": return new GoogleDestination(vertx, destinationName.substring(destinationName.indexOf("://") + 3).trim());

            default:
                throw new IllegalArgumentException("Unknown destination type: " + destinationType);
        }
    }
}
