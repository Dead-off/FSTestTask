package maxim.z;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface IFileSystem extends Closeable {

    void write(IFile file, String content) throws IOException;

    void write(IFile file, byte[] content) throws IOException;

    byte[] read(IFile file) throws IOException;

    String readAsString(IFile file) throws IOException;

    IFile createDirectory(IFile parent, String newDirectoryName) throws IOException;

    IFile createFile(IFile parent, String newFileName) throws IOException;

    void removeFile(IFile file) throws IOException;

    List<String> getFilesList(IFile directory) throws IOException;

}
