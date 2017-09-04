package maxim.z;

import maxim.z.exceptions.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public abstract class VirtualFileSystem implements Closeable {

    /**
     * Overrides data of specified file by specified content.
     *
     * @param file    file for writing data
     * @param content string (encoding specified in {@link FSConstants}), that must written to file
     * @throws IOException           on any default IO error
     * @throws FileNotFoundException if specified file was not found
     * @throws WriteException        if specified file is not available for writing (e.g. file is a directory)
     */
    abstract void write(VirtualFile file, String content) throws IOException;

    /**
     * Overrides data of specified file by specified content.
     *
     * @param file    file for writing data
     * @param content bytes, that must written to file
     * @throws IOException           on any default IO error
     * @throws FileNotFoundException if specified file was not found
     * @throws WriteException        if specified file is not available for writing (e.g. file is a directory)
     */
    abstract void write(VirtualFile file, byte[] content) throws IOException;

    // TODO: 03.09.2017 javadoc
    abstract void write(VirtualFile file, int offset, byte[] content) throws IOException;

    /**
     * Reads file content and return it
     *
     * @param file file for reading data
     * @return data of specified file
     * @throws IOException           on any default IO error
     * @throws FileNotFoundException if specified file was not found
     * @throws ReadException         if specified file is not available for reading (e.g. file is a directory)
     */
    abstract byte[] read(VirtualFile file) throws IOException;

    // TODO: 03.09.2017 javadoc
    /**
     * Reads file content part and return it. Если кол-во байтов (from+count) меньше размера файла, то размер результирующего буфера будет равен кол-ву прочитанных байт, не count
     *
     * @param file  file for reading data
     * @param from
     * @param count
     * @return data of specified file. Byte array have array equals count, but
     * @throws IOException
     */
    abstract byte[] read(VirtualFile file, int from, int count) throws IOException;

    /**
     * Reads file content and return it
     *
     * @param file file for reading data
     * @return data of specified file as string (encoding specified in {@link FSConstants})
     * @throws IOException           on any default IO error
     * @throws FileNotFoundException if specified file was not found
     * @throws ReadException         if specified file is not available for reading (e.g. file is a directory)
     */
    abstract String readAsString(VirtualFile file) throws IOException;

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
    abstract VirtualFile createDirectory(VirtualFile parent, String newDirectoryName) throws IOException;

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
    abstract VirtualFile createFile(VirtualFile parent, String newFileName) throws IOException;

    /**
     * removes a specified file
     *
     * @param file file for removing
     * @throws IOException           on any default IO error
     * @throws FileNotFoundException if specified file was not found
     */
    abstract void removeFile(VirtualFile file) throws IOException;

    /**
     * @param file specified directory
     * @return true, is specified directory exist. Otherwise return false
     * @throws IOException on any default IO error
     */
    abstract boolean isDirectoryExist(VirtualFile file) throws IOException;

    /**
     * return a list of files and directories in specify directory
     *
     * @param directory directory for get files list
     * @return list of files and directories in specify directory
     * @throws FileNotFoundException if directory was not found
     * @throws IOException           on any default IO error
     */
    abstract List<String> getFilesList(VirtualFile directory) throws IOException;

    /**
     * return root file instance for current file system
     *
     * @return root file instance
     */
    public abstract VirtualFile getRootFile();

}
