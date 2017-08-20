package maxim.z;

import maxim.z.exceptions.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface IFileSystem extends Closeable {

    /**
     * Overrides data of specified file by specified content.
     *
     * @param file    file for writing data
     * @param content string (encoding specified in {@link FSConstants}), that must written to file
     * @throws IOException           on any default IO error
     * @throws FileNotFoundException if specified file was not found
     * @throws WriteException        if specified file is not available for writing (e.g. file is a directory)
     */
    void write(IFile file, String content) throws IOException;

    /**
     * Overrides data of specified file by specified content.
     *
     * @param file    file for writing data
     * @param content bytes, that must written to file
     * @throws IOException           on any default IO error
     * @throws FileNotFoundException if specified file was not found
     * @throws WriteException        if specified file is not available for writing (e.g. file is a directory)
     */
    void write(IFile file, byte[] content) throws IOException;

    /**
     * Reads file content and return it
     *
     * @param file file for reading data
     * @return data of specified file
     * @throws IOException           on any default IO error
     * @throws FileNotFoundException if specified file was not found
     * @throws ReadException         if specified file is not available for reading (e.g. file is a directory)
     */
    byte[] read(IFile file) throws IOException;

    /**
     * Reads file content and return it
     *
     * @param file file for reading data
     * @return data of specified file as string (encoding specified in {@link FSConstants})
     * @throws IOException           on any default IO error
     * @throws FileNotFoundException if specified file was not found
     * @throws ReadException         if specified file is not available for reading (e.g. file is a directory)
     */
    String readAsString(IFile file) throws IOException;

    /**
     * creates a new directory in file system and return it
     *
     * @param parent           directory for creating file
     * @param newDirectoryName name of new directory
     * @return File object of created file
     * @throws IOException            on any default IO error
     * @throws IncorrectNameException if name contains forbidden symbols
     * @throws FileNotFoundException  if parent directory was not found
     * @throws CreateFileException    if parent object is not directory
     */
    IFile createDirectory(IFile parent, String newDirectoryName) throws IOException;

    /**
     * creates a new file in file system and return it
     *
     * @param parent      directory for creating file
     * @param newFileName name of new file
     * @return File object of created file
     * @throws IOException            on any default IO error
     * @throws IncorrectNameException if name contains forbidden symbols
     * @throws FileNotFoundException  if parent directory was not found
     * @throws CreateFileException    if parent object is not directory
     */
    IFile createFile(IFile parent, String newFileName) throws IOException;

    /**
     * removes a specified file
     *
     * @param file file for removing
     * @throws IOException           on any default IO error
     * @throws FileNotFoundException if specified file was not found
     */
    void removeFile(IFile file) throws IOException;

    /**
     * return a list of files and directories in specify directory
     *
     * @param directory directory for get files list
     * @return list of files and directories in specify directory
     * @throws FileNotFoundException if directory was not found
     * @throws IOException           on any default IO error
     */
    List<String> getFilesList(IFile directory) throws IOException;

}
