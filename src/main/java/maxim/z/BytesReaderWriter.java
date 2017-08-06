package maxim.z;

import java.io.Closeable;
import java.io.IOException;

public interface BytesReaderWriter extends Closeable {

    void write(byte[] bytes) throws IOException;

    void readBytes(byte[] data, int offset, int length) throws IOException;

    void seek(long pos) throws IOException;

}
