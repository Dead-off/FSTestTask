package maxim.z;

import maxim.z.exceptions.CreateFileException;
import maxim.z.exceptions.FileNotFoundException;
import maxim.z.exceptions.IncorrectNameException;

import java.io.IOException;
import java.util.List;

/**
 * Support object for create file pointers for file system method {@link VirtualFileSystem}
 */
public interface VirtualFile {

    /**
     * create new file instance for child file in current file with specified name
     *
     * @param name name of child file
     * @return new file instance
     */
    VirtualFile child(String name);

    /**
     * create new file instance for parent of current file. If it is root file, return new root instance
     *
     * @return new file instance
     */
    VirtualFile parent();

    /**
     * @return absolute path for current file.
     */
    String getPath();

    /**
     * @return name of current file
     */
    String getName();

    /**
     * create new file in a file system
     *
     * @throws IncorrectNameException if name contains forbidden symbols
     * @throws FileNotFoundException  if parent directory was not found
     * @throws CreateFileException    if parent object is not directory
     * @throws IOException            on any default IO error
     *
     */
    void createFile() throws IOException;

    /**
     * create new directory in a file system
     *
     * @throws IncorrectNameException if name contains forbidden symbols
     * @throws FileNotFoundException  if parent directory was not found
     * @throws CreateFileException    if parent object is not directory
     * @throws IOException            on any default IO error
     *
     */
    void createDirectory() throws IOException;

    /**
     * remove a file
     *
     * @throws IOException           on any default IO error
     * @throws FileNotFoundException if specified file was not found
     */
    void remove() throws IOException;

    /**
     * @return new input stream instance for reading data
     */
    VirtualInputStream getInputStream();

    /**
     * @return new output stream instance for writing data
     */
    VirtualOutputStream getOutputStream();

    /**
     * @return true, if this file exist, otherwise false
     * @throws IOException   on any default IO error
     */
    boolean exist() throws IOException;

    /**
     * @return true, if this file exist, otherwise false
     * @throws IOException   on any default IO error
     */
    List<VirtualFile> children() throws IOException;

}
