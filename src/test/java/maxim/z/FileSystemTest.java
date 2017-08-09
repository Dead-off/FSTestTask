package maxim.z;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;

public class FileSystemTest {

    private final byte[] emptyRootDirFS
            = new byte[FSConstants.Offsets.FAT_TABLE + FSConstants.DEFAULT_CLUSTER_COUNT * FSConstants.BYTE_DEPTH + FSConstants.DEFAULT_CLUSTER_SIZE];

    @Before
    public void fillEmptyRootDirArray() {
        FSUtils.writeIntAsBytesToArray(emptyRootDirFS, FSConstants.Offsets.CLUSTERS_COUNT, FSConstants.DEFAULT_CLUSTER_COUNT);
        FSUtils.writeIntAsBytesToArray(emptyRootDirFS, FSConstants.Offsets.LAST_USED_CLUSTER, 0);
        FSUtils.writeIntAsBytesToArray(emptyRootDirFS, FSConstants.Offsets.CLUSTER_SIZE, FSConstants.DEFAULT_CLUSTER_SIZE);
        FSUtils.writeIntAsBytesToArray(emptyRootDirFS, FSConstants.Offsets.FAT_TABLE, FSConstants.END_OF_CHAIN);
        byte spaceByte = " ".getBytes(FSConstants.CHARSET)[0];
        byte[] rootDirectoryHeader = new byte[FSConstants.FILE_HEADER_LENGTH];
        for (int i = 0; i < FSConstants.FILE_NAME_LENGTH; i++) {
            rootDirectoryHeader[i] = spaceByte;
        }
        rootDirectoryHeader[FSConstants.FILE_NAME_LENGTH] = 1;
        System.arraycopy(rootDirectoryHeader, 0, emptyRootDirFS,
                FSConstants.Offsets.FAT_TABLE + FSConstants.DEFAULT_CLUSTER_COUNT * FSConstants.BYTE_DEPTH, rootDirectoryHeader.length);
    }

    @Test
    public void initTest() throws IOException {
        BytesReaderWriter brw = new MemoryReaderWriter(0);
        FileSystem fs = FileSystem.getFileSystem(brw);
        fs.init();
        byte[] actual = new byte[FSConstants.Offsets.FAT_TABLE + FSConstants.DEFAULT_CLUSTER_COUNT * FSConstants.BYTE_DEPTH + FSConstants.DEFAULT_CLUSTER_SIZE];
        byte[] expected = getCopyOfEmptyRootArray();
        brw.seek(0);
        brw.readBytes(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void fsTest() throws IOException {

    }


    private byte[] getCopyOfEmptyRootArray() {
        byte[] result = new byte[emptyRootDirFS.length];
        System.arraycopy(emptyRootDirFS, 0, result, 0, result.length);
        return result;
    }
}
