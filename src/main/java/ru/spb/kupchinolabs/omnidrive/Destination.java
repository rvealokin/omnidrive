package ru.spb.kupchinolabs.omnidrive;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

/**
 * Created by inikolaev on 18/04/16.
 */
public interface Destination {
    void start(FileMetadata metadata, Handler<WriteStream<Buffer>> handler);
}
