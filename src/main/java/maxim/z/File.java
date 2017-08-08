package maxim.z;

import maxim.z.exceptions.EmptyFileNameException;
import maxim.z.exceptions.IncorrectFilePath;
import maxim.z.exceptions.IncorrectNameException;

import java.util.Arrays;

public class File {

    public final String path;

    private File(String path) {
        this.path = path;
    }

    public static File getFile(String path) {
        return new File(path);
    }

    public String[] parseFileNames() {
        if (path.isEmpty()) {
            throw new EmptyFileNameException();
        }
        if (!path.startsWith(FSConstants.DIRECTORIES_SEPARATOR)) {
            throw new IncorrectFilePath();
        }
        String pathWithoutFirstSeparator = path.substring(FSConstants.DIRECTORIES_SEPARATOR.length());
        if (pathWithoutFirstSeparator.isEmpty()) {
            return new String[0];
        }
        String[] names = pathWithoutFirstSeparator.split(FSConstants.DIRECTORIES_SEPARATOR);
        boolean existIncorrectNames = Arrays.stream(names).filter(s -> !FSUtils.isCorrectName(s)).count() > 0;
        if (existIncorrectNames) {
            throw new IncorrectNameException();
        }
        return names;
    }
    
}
