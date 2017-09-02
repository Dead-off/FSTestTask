package maxim.z;

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
     * @return names of files/directories for current file
     */
    String[] parseFileNames();

}
