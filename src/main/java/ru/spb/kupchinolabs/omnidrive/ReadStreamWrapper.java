package ru.spb.kupchinolabs.omnidrive;

import io.vertx.core.Handler;
import io.vertx.core.streams.ReadStream;

/**
 * Created by inikolaev on 18/04/16.
 */
public class ReadStreamWrapper<T> implements ReadStream<T> {
    private final ReadStream<T> delegate;

    public ReadStreamWrapper(final ReadStream<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public ReadStream<T> exceptionHandler(Handler<Throwable> handler) {
        return delegate.exceptionHandler(handler);
    }

    @Override
    public ReadStream<T> handler(Handler<T> handler) {
        return delegate.handler(handler);
    }

    @Override
    public ReadStream<T> pause() {
        return delegate.pause();
    }

    @Override
    public ReadStream<T> resume() {
        return delegate.resume();
    }

    @Override
    public ReadStream<T> endHandler(Handler<Void> endHandler) {
        return delegate.endHandler(endHandler);
    }
}
