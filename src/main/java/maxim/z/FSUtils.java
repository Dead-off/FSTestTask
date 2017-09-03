package maxim.z;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.regex.Pattern;

class FSUtils {

    private final static Pattern NAME_PATTERN = Pattern.compile(String.format("^[A-Za-z0-9]{1}[A-Za-z0-9-_\\s]{0,%s}$", FSConstants.FILE_NAME_LENGTH - 1));

    static byte[] intAsFourBytes(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    static int intFromFourBytes(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    static void writeIntAsBytesToArray(byte[] destArr, int destPosition, int value) {
        byte[] bytes = intAsFourBytes(value);
        System.arraycopy(bytes, 0, destArr, destPosition, bytes.length);
    }

    static String getNameWithSpaces(String name) {
        StringBuilder nameWithSpaces = new StringBuilder(name);
        while (nameWithSpaces.length() < FSConstants.FILE_NAME_LENGTH) {
            nameWithSpaces.append(" ");
        }
        return nameWithSpaces.toString();
    }

    static boolean isCorrectName(String name) {
        return NAME_PATTERN.matcher(name).matches();
    }

    static String[] parseFileNames(VirtualFile file) {
        return Arrays.stream(file.getPath().split(FSConstants.DIRECTORIES_SEPARATOR)).filter(s -> !s.isEmpty()).toArray(String[]::new);
    }

}
