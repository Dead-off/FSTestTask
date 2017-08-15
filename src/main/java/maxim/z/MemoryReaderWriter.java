package maxim.z;

import java.io.IOException;

class MemoryReaderWriter implements BytesReaderWriter {

    private byte[] storage;
    private int position = 0;

    public MemoryReaderWriter(int initialCapacity) {
        this.storage = new byte[initialCapacity];
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        ensureCapacityForLength(bytes.length);
        System.arraycopy(bytes, 0, storage, position, bytes.length);
    }

    private void ensureCapacityForLength(int length) {
        if (storage.length >= this.position + length) {
            return;
        }
        byte[] oldBytes = storage;
        storage = new byte[(this.position + length) * 2];
        System.arraycopy(oldBytes, 0, storage, 0, oldBytes.length);
    }

    @Override
    public void readBytes(byte[] data) throws IOException {
        ensureCapacityForLength(data.length);
        System.arraycopy(storage, position, data, 0, data.length);
    }

    @Override
    public void seek(long pos) throws IOException {
        this.position = (int) pos;
    }

    @Override
    public void seekAndRead(byte[] data, long pos) throws IOException {
        seek(pos);
        readBytes(data);
    }

    @Override
    public void seekAndWrite(byte[] bytes, long pos) throws IOException {
        seek(pos);
        write(bytes);
    }

    @Override
    public void close() throws IOException {

    }
}
