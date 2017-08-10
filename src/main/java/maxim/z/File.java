package maxim.z;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class File {

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
                .filter(s->!s.isEmpty()).collect(Collectors.toList()));
        return pathFile;
    }

    public File child(String name) {
        File child = new File();
        child.directories.addAll(directories);
        child.directories.add(name);
        return child;
    }

    public String[] parseFileNames() {
        return directories.toArray(new String[directories.size()]);
    }
}
