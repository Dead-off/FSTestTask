package maxim.z;

/**
 * Support object for create file pointers for file system method {@link IFileSystem}
 */
public interface IFile {

    /**
     * create new file instance for child file in current file with specified name
     *
     * @param name name of child file
     * @return new file instance
     */
    IFile child(String name);

    /**
     * create new file instance for parent of current file. If it is root file, return new root instance
     *
     * @return new file instance
     */
    IFile parent();

    /**
     * @return absolute path for current file.
     */
    String getPath();

    /**
     * @return names of files/directories for current file
     */
    String[] parseFileNames();

}
