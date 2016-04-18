package ru.spb.kupchinolabs.omnidrive;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;

/**
 * Created by inikolaev on 18/04/16.
 */
public interface Source {
    void start(Handler<ReadStream<Buffer>> handler);
}
