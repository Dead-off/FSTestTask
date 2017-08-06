package maxim.z;

public class File {

    public final String path;
    public final boolean isDirectory;

    private File(String path, boolean isDirectory) {
        this.path = path;
        this.isDirectory = isDirectory;
    }

    public static File getFile(String path) {
        return new File(path, false);
    }

    public static File getDirectory(String path) {
        return new File(path, true);
    }
}
