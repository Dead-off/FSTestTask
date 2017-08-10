package maxim.z;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
        FileSystem fs = FileSystem.getFileSystem(brw, FSConstants.DEFAULT_CLUSTER_COUNT, FSConstants.DEFAULT_CLUSTER_SIZE);
        fs.init();
        byte[] actual = new byte[FSConstants.Offsets.FAT_TABLE + FSConstants.DEFAULT_CLUSTER_COUNT * FSConstants.BYTE_DEPTH + FSConstants.DEFAULT_CLUSTER_SIZE];
        byte[] expected = getCopyOfEmptyRootArray();
        brw.seek(0);
        brw.readBytes(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void fsTest() throws IOException {
        BytesReaderWriter brw = new MemoryReaderWriter(0);
        FileSystem fs = FileSystem.getFileSystem(brw, FSConstants.DEFAULT_CLUSTER_COUNT, FSConstants.DEFAULT_CLUSTER_SIZE);
        fs.init();
        File root = File.rootInstance();











        assertEquals(0, fs.getFilesList(root).size());
        File testFile = fs.createFile(root, "testFile");
        assertEquals(1, fs.getFilesList(root).size());
        File testDirectory = fs.createDirectory(root, "testDirectory");
        assertEquals(2, fs.getFilesList(root).size());
        String contentString = "abcd";
        fs.write(testFile, contentString);
        assertEquals(contentString, fs.readAsString(testFile));
        fs.removeFile(testFile);
        assertEquals(1, fs.getFilesList(root).size());
        assertEquals(0, fs.getFilesList(testDirectory).size());
        File dirLevel2 = fs.createDirectory(testDirectory, "dir");
        assertEquals(1, fs.getFilesList(root).size());
        assertEquals(1, fs.getFilesList(testDirectory).size());
        File fileLevel2 = fs.createFile(dirLevel2, "testFile");
        assertArrayEquals(new byte[]{}, fs.read(fileLevel2));

        byte[] largeByteContent = new byte[FSConstants.DEFAULT_CLUSTER_SIZE * 5];
        fs.write(fileLevel2, largeByteContent);
        assertArrayEquals(largeByteContent, fs.read(fileLevel2));

        largeByteContent = new byte[FSConstants.DEFAULT_CLUSTER_SIZE * 2];
        fs.write(fileLevel2, largeByteContent);
        assertArrayEquals(largeByteContent, fs.read(fileLevel2));

        largeByteContent = new byte[FSConstants.DEFAULT_CLUSTER_SIZE * 7];
        fs.write(fileLevel2, largeByteContent);
        assertArrayEquals(largeByteContent, fs.read(fileLevel2));


        // TODO: 10.08.2017 Более серьезные теасты. Попробовать записывать контент, который не ввлезает в один кластер
        // попробовать записывать контент поверх существующего с 3мя случаями: существующий влезате в 3 кластера, а прерыдущий был в 4+, обратный  вариант (4 против 3
        //и равное кол-во
        //осздание и удаление директорий более серьезное с большей вложенностью, тесты на считывание контента и т.п.

        //отрицательные тесты (попытка прочитать данные с директории, зайти в файл, записать в директорию, создать файл в файле и т.п.


    }


    private byte[] getCopyOfEmptyRootArray() {
        byte[] result = new byte[emptyRootDirFS.length];
        System.arraycopy(emptyRootDirFS, 0, result, 0, result.length);
        return result;
    }
}
