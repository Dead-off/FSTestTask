package maxim.z;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class RAFAdapter implements BytesReaderWriter {

    private final RandomAccessFile randomAccessFile;

    RAFAdapter(java.io.File fsFile) throws FileNotFoundException {
        this.randomAccessFile = new RandomAccessFile(fsFile, "rws");
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        randomAccessFile.write(bytes);
    }

    @Override
    public void readBytes(byte[] data) throws IOException {
        randomAccessFile.read(data, 0, data.length);
    }

    @Override
    public void seek(long pos) throws IOException {
        randomAccessFile.seek(pos);
    }

    @Override
    public void close() throws IOException {
        randomAccessFile.close();
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
}
