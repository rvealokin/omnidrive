package ru.spb.kupchinolabs.omnidrive;

import io.vertx.core.Handler;
import io.vertx.core.streams.WriteStream;

/**
 * Created by inikolaev on 18/04/16.
 */
public class WriteStreamWrapper<T> implements WriteStream<T> {
    private final WriteStream<T> delegate;

    public WriteStreamWrapper(WriteStream<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public WriteStream<T> exceptionHandler(Handler<Throwable> handler) {
        return delegate.exceptionHandler(handler);
    }

    @Override
    public WriteStream<T> write(T data) {
        return delegate.write(data);
    }

    @Override
    public void end() {
        delegate.end();
    }

    @Override
    public void end(T t) {
        delegate.end(t);
    }

    @Override
    public WriteStream<T> setWriteQueueMaxSize(int maxSize) {
        return delegate.setWriteQueueMaxSize(maxSize);
    }

    @Override
    public boolean writeQueueFull() {
        return delegate.writeQueueFull();
    }

    @Override
    public WriteStream<T> drainHandler(Handler<Void> handler) {
        return delegate.drainHandler(handler);
    }
}
