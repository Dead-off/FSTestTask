package maxim.z;

import java.nio.ByteBuffer;
import java.util.regex.Pattern;

class FSUtils {

    private final static Pattern NAME_PATTERN = Pattern.compile(String.format("^[A-Za-z0-9-_]{1,%s}$", FSConstants.FILE_NAME_LENGTH));

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

}
