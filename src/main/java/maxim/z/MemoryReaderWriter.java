package maxim.z;

import java.io.IOException;

class MemoryReaderWriter implements BytesReaderWriter {

    private final byte[] storage;
    private int position = 0;

    public MemoryReaderWriter(int length) {
        this.storage = new byte[length];
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        System.arraycopy(bytes, 0, storage, position, bytes.length);
    }

    @Override
    public void readBytes(byte[] data) throws IOException {
        System.arraycopy(storage, position, data, 0, data.length);
    }

    @Override
    public void seek(long pos) throws IOException {
        this.position = (int) pos;
    }

    @Override
    public void close() throws IOException {

    }
}
