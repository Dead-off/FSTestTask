package maxim.z;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FileImpl implements VirtualFile {

    final VirtualFileSystem fs;
    private final List<String> directories;

    private FileImpl(VirtualFileSystem fs) {
        this.fs = fs;
        this.directories = new ArrayList<>();
    }

    static FileImpl rootInstance(VirtualFileSystem fs) {
        return new FileImpl(fs);
    }

    static FileImpl fromPath(String path, VirtualFileSystem fs) {
        FileImpl pathFile = new FileImpl(fs);
        pathFile.directories.addAll(Arrays.stream(path.split(FSConstants.DIRECTORIES_SEPARATOR))
                .filter(s -> !s.isEmpty()).collect(Collectors.toList()));
        return pathFile;
    }

    @Override
    public FileImpl child(String name) {
        FileImpl child = new FileImpl(fs);
        child.directories.addAll(directories);
        child.directories.add(name);
        return child;
    }

    @Override
    public FileImpl parent() {
        FileImpl result = new FileImpl(fs);
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

    @Override
    public String getName() {
        return directories.isEmpty() ? "" : directories.get(directories.size() - 1);
    }

    private void dropLastDirectory() {
        this.directories.remove(this.directories.size() - 1);
    }

    private boolean isRootFile() {
        return this.directories.size() == 0;
    }

    @Override
    public void createFile() throws IOException {
        VirtualFile parent = parent();
        String name = getName();
        fs.createFile(parent, name);
    }

    @Override
    public void createDirectory() throws IOException {
        VirtualFile parent = parent();
        String name = getName();
        fs.createDirectory(parent, name);
    }

    @Override
    public void remove() throws IOException {
        fs.removeFile(this);
    }

    @Override
    public VirtualInputStream getInputStream() {
        return new InputStreamImpl(fs, this);
    }

    @Override
    public VirtualOutputStream getOutputStream() {
        return new OutputStreamImpl(fs, this);
    }
}
