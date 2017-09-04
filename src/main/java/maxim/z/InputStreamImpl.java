package maxim.z;

import java.io.IOException;

public class InputStreamImpl implements VirtualInputStream {

    private final VirtualFileSystem fs;
    private final VirtualFile file;
    private int offset = 0;

    public InputStreamImpl(VirtualFileSystem fs, VirtualFile file) {
        this.fs = fs;
        this.file = file;
    }

    @Override
    public int read(byte[] data) throws IOException {
        byte[] content = fs.read(file, offset, data.length);
        int countOfReadBytes = content.length;
        offset += content.length;
        System.arraycopy(content, 0, data, 0, countOfReadBytes);
        return countOfReadBytes;
    }
}
