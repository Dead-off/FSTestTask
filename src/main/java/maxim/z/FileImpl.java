package maxim.z;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FileImpl implements VirtualFile {

    private final List<String> directories;

    private FileImpl() {
        this.directories = new ArrayList<>();
    }

    public static FileImpl rootInstance() {
        return new FileImpl();
    }

    static FileImpl fromPath(String path) {
        FileImpl pathFile = new FileImpl();
        pathFile.directories.addAll(Arrays.stream(path.split(FSConstants.DIRECTORIES_SEPARATOR))
                .filter(s -> !s.isEmpty()).collect(Collectors.toList()));
        return pathFile;
    }

    @Override
    public FileImpl child(String name) {
        FileImpl child = new FileImpl();
        child.directories.addAll(directories);
        child.directories.add(name);
        return child;
    }

    @Override
    public FileImpl parent() {
        FileImpl result = new FileImpl();
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
