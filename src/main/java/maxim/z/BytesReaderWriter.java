package maxim.z;

import java.io.Closeable;
import java.io.IOException;

public interface BytesReaderWriter extends Closeable {

    void write(byte[] bytes) throws IOException;

    void readBytes(byte[] data) throws IOException;

    void seekAndRead(byte[] data, long pos) throws IOException;

    void seekAndWrite(byte[] bytes, long pos) throws IOException;

    void seek(long pos) throws IOException;

}
