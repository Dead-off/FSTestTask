package maxim.z;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Support class for create file pointers for file system method {@link IFileSystem}
 */
public class File implements IFile {

    private final List<String> directories;

    private File() {
        this.directories = new ArrayList<>();
    }

    public static File rootInstance() {
        return new File();
    }

    static File fromPath(String path) {
        File pathFile = new File();
        pathFile.directories.addAll(Arrays.stream(path.split(FSConstants.DIRECTORIES_SEPARATOR))
                .filter(s -> !s.isEmpty()).collect(Collectors.toList()));
        return pathFile;
    }

    @Override
    public File child(String name) {
        File child = new File();
        child.directories.addAll(directories);
        child.directories.add(name);
        return child;
    }

    @Override
    public File parent() {
        File result = new File();
        result.directories.addAll(this.directories);
        if (result.isRootFile()) {
            return result;
        }
        result.dropLastDirectory();
        return result;
    }

    @Override
    public String getPath() {
        return FSConstants.DIRECTORIES_SEPARATOR + String.join(FSConstants.DIRECTORIES_SEPARATOR, this.directories);
    }

    private void dropLastDirectory() {
        this.directories.remove(this.directories.size() - 1);
    }

    @Override
    public String[] parseFileNames() {
        return directories.toArray(new String[directories.size()]);
    }

    private boolean isRootFile() {
        return this.directories.size() == 0;
    }
}
