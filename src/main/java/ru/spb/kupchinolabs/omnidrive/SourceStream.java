package ru.spb.kupchinolabs.omnidrive;

import io.vertx.core.streams.ReadStream;

/**
 * Created by inikolaev on 18/04/16.
 */
public class SourceStream<T> extends ReadStreamWrapper<T> {
    private final FileMetadata metadata;

    public SourceStream(FileMetadata metadata, ReadStream<T> stream) {
        super(stream);
        this.metadata = metadata;
    }

    public FileMetadata getMetadata() {
        return metadata;
    }
}
