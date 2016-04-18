package ru.spb.kupchinolabs.omnidrive;

/**
 * Created by inikolaev on 18/04/16.
 */
public class FileMetadata {
    private final String name;
    private final String type;
    private final long size;

    public FileMetadata(String name) {
        this(name, "application/octet-stream");
    }

    public FileMetadata(String name, long size) {
        this(name, "application/octet-stream", size);
    }

    public FileMetadata(String name, String type) {
        this(name, type, -1);
    }

    public FileMetadata(String name, String type, long size) {
        this.name = name;
        this.type = type;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public long getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", size=" + size +
                '}';
    }
}
