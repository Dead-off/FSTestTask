package maxim.z;

import java.io.IOException;

public class OutputStreamImpl implements VirtualOutputStream {

    private final VirtualFileSystem fs;
    private final VirtualFile file;
    private int offset = 0;

    public OutputStreamImpl(VirtualFileSystem fs, VirtualFile file) {
        this.fs = fs;
        this.file = file;
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        fs.write(file, offset, bytes);
        offset += bytes.length;
    }
}
