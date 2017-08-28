package maxim.z;

import maxim.z.exceptions.*;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

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
        IFile testFile = fs.createFile(root, "testFile");
        assertEquals(1, fs.getFilesList(root).size());
        IFile testDirectory = fs.createDirectory(root, "testDirectory");
        assertEquals(2, fs.getFilesList(root).size());
        String contentString = "abcd";
        fs.write(testFile, contentString);
        assertEquals(contentString, fs.readAsString(testFile));
        fs.removeFile(testFile);
        assertEquals(1, fs.getFilesList(root).size());
        assertEquals(0, fs.getFilesList(testDirectory).size());
        IFile dirLevel2 = fs.createDirectory(testDirectory, "dir");
        assertEquals(1, fs.getFilesList(root).size());
        assertEquals(1, fs.getFilesList(testDirectory).size());
        IFile fileLevel2 = fs.createFile(dirLevel2, "testFile");
        assertArrayEquals(new byte[]{}, fs.read(fileLevel2));

        byte[] largeByteContent = new byte[FSConstants.DEFAULT_CLUSTER_SIZE * 5];
        fs.write(fileLevel2, largeByteContent);
        assertArrayEquals(largeByteContent, fs.read(fileLevel2));

        largeByteContent = new byte[FSConstants.DEFAULT_CLUSTER_SIZE * 2];
        fs.write(fileLevel2, largeByteContent);
        assertArrayEquals(largeByteContent, fs.read(fileLevel2));

        largeByteContent = new byte[FSConstants.DEFAULT_CLUSTER_SIZE * 7];
        largeByteContent[FSConstants.DEFAULT_CLUSTER_SIZE] = 1;
        fs.write(fileLevel2, largeByteContent);
        assertArrayEquals(largeByteContent, fs.read(fileLevel2));

        largeByteContent = new byte[FSConstants.DEFAULT_CLUSTER_SIZE * 7];
        largeByteContent[FSConstants.DEFAULT_CLUSTER_SIZE * 2] = 3;
        fs.write(fileLevel2, largeByteContent);
        assertArrayEquals(largeByteContent, fs.read(fileLevel2));

        try {
            fs.write(testDirectory, "abcd");
            fail();
        } catch (WriteException ignored) {
        }

        try {
            fs.read(testDirectory);
            fail();
        } catch (ReadException ignored) {
        }

        try {
            fs.createFile(fileLevel2, "abc");
            fail();
        } catch (CreateFileException ignored) {
        }

        try {
            fs.createFile(testDirectory, "incorrect/name");
            fail();
        } catch (IncorrectNameException ignored) {
        }

        try {
            fs.removeFile(fileLevel2.child("notExist"));
            fail();
        } catch (FileNotFoundException ignored) {
        }

        File notExistFile = root.child("notExistDir").child("notExistFile");
        try {
            fs.removeFile(notExistFile);
            fail();
        } catch (FileNotFoundException ignored) {
        }
        try {
            fs.write(notExistFile, "");
            fail();
        } catch (FileNotFoundException ignored) {
        }
        try {
            fs.read(notExistFile);
            fail();
        } catch (FileNotFoundException ignored) {
        }
    }

    @Test
    public void realFileTest() throws IOException {
        String fileName = "build/fs_test_file";
        java.io.File fsFile = new java.io.File(fileName);
        if (fsFile.exists()) {
            boolean deletedSuccessfully = fsFile.delete();
            if (!deletedSuccessfully) {
                fail(String.format("Can not remove file %s", fsFile.getAbsolutePath()));
            }
        }
        String absolutePathToFile = fsFile.getAbsolutePath();
        IFile root = File.rootInstance();
        String testFileNameInFS = "testfile";
        String testContent = "I am content!";
        IFile testFile;
        try (FileSystem fs = FileSystem.getFileSystem(absolutePathToFile)) {
            testFile = fs.createFile(root, testFileNameInFS);
            fs.write(testFile, testContent);
        }
        try (FileSystem fs = FileSystem.getFileSystem(absolutePathToFile)) {
            String actualContent = fs.readAsString(testFile);
            assertEquals(testContent, actualContent);
        }
    }

    private byte[] getCopyOfEmptyRootArray() {
        byte[] result = new byte[emptyRootDirFS.length];
        System.arraycopy(emptyRootDirFS, 0, result, 0, result.length);
        return result;
    }
}
