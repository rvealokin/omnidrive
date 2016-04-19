package ru.spb.kupchinolabs.omnidrive;

import com.google.common.base.Preconditions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;

import java.io.File;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Downloader vertical which can download from some source and pump into some destination
 *
 * @author inikolaev
 */
public class DownloaderVertical extends AbstractVerticle {
    public static final int BUFFER_SIZE_1024K = 2 << 21;

    private static class LoggingWriteStreamAdapter extends WriteStreamWrapper<Buffer> {
        private int written = 0;

        public LoggingWriteStreamAdapter(WriteStream<Buffer> delegate) {
            super(delegate);
        }

        @Override
        public WriteStream<Buffer> write(Buffer t) {
            written += t.length();
            System.out.println("Writing " + t.length() + " / " + written + " bytes");
            return super.write(t);
        }
    }

    private static class LoggingReadStreamAdapter extends ReadStreamWrapper<Buffer> {
        private int read = 0;

        public LoggingReadStreamAdapter(ReadStream<Buffer> stream) {
            super(stream);
        }

        @Override
        public ReadStream<Buffer> handler(Handler<Buffer> handler) {
            return super.handler(buffer -> {
                read += buffer.length();
                System.out.println("Read " + buffer.length() + " / " + read + " bytes");
                handler.handle(buffer);
            });
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
                destination.start(read.getMetadata(), write -> {
                    read.resume();

                    System.out.println("Beginning transmission");

                    Pump.pump(new LoggingReadStreamAdapter(read), new LoggingWriteStreamAdapter(write), BUFFER_SIZE_1024K).start();

                    read.endHandler(v -> {
                        System.out.println("Transmission about to be completed");
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
                final String path = sourceName.substring(sourceName.indexOf("://") + 3).trim();
                vertx.fileSystem().open(path, new OpenOptions().setWrite(false), result -> {
                    if (result.succeeded()) {
                        final File file = Paths.get(path).toFile();
                        handler.handle(new SourceStream(new FileMetadata(file.getName(), file.length()), result.result()));
                    }
                });
            };

            case "yandex": return new YandexSource(vertx, sourceName.substring(sourceName.indexOf("://") + 3).trim());
            case "google": return new GoogleSource(vertx, sourceName.substring(sourceName.indexOf("://") + 3).trim());
            case "dropbox": return new DropboxSource(vertx, sourceName.substring(sourceName.indexOf("://") + 3).trim());

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
            case "file": return (metadata, handler) -> {
                String path = destinationName.substring(destinationName.indexOf("://") + 3).trim();

                if (Paths.get(path).toFile().isDirectory()) {
                    path = path + File.separator + metadata.getName();
                }

                vertx.fileSystem().open(path, new OpenOptions().setRead(false).setTruncateExisting(true), result -> {
                    if (result.succeeded()) {
                        handler.handle(result.result());
                    }
                });
            };

            case "yandex": return new YandexDestination(vertx, destinationName.substring(destinationName.indexOf("://") + 3).trim());
            case "google": return new GoogleDestination(vertx, destinationName.substring(destinationName.indexOf("://") + 3).trim());
            case "dropbox": return new DropboxDestination(vertx, destinationName.substring(destinationName.indexOf("://") + 3).trim());

            default:
                throw new IllegalArgumentException("Unknown destination type: " + destinationType);
        }
    }
}
