package maxim.z;

import java.io.IOException;

public class FileSystemFactory {

    public static VirtualFileSystem getFileSystem(String pathToFile) throws IOException {
        java.io.File fsFile = new java.io.File(pathToFile);
        if (!fsFile.exists()) {
            createFSFile(fsFile);
        }
        BytesReaderWriter readerWriter = new RAFWrapper(fsFile);
        return new FileSystemImpl(readerWriter);
    }

    public static VirtualFileSystem getFileSystem(BytesReaderWriter readerWriter) throws IOException {
        return new FileSystemImpl(readerWriter);
    }

    private static void createFSFile(java.io.File fsFile) throws IOException {
        boolean res = fsFile.createNewFile();
        if (!res) {
            throw new IOException("Unable to create a file system. File system with this name already exist");
        }
    }
}
