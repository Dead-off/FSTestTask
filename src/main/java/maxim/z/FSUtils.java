package maxim.z;

import maxim.z.exceptions.IncorrectNameException;

import java.nio.ByteBuffer;
import java.util.regex.Pattern;

public class FSUtils {

    private final static Pattern NAME_PATTERN = Pattern.compile(String.format("^[A-Za-z0-9-_]{1,%s}$", FSConstants.FILE_NAME_LENGTH));

    public static byte[] intAsFourBytes(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    // TODO: 07.08.2017 So far it is just comment. Refactor it.
    /**
     * Return byte array, that contains meta information for file. This byte array
     * have next structure:
     * first FSConstants.FILE_NAME_LENGTH contains file name.
     * If file name length is less, that FSConstants.FILE_NAME_LENGTH,
     * then append spaces to right if name
     * next 2 bytes contains file attributes. Now used only first bit (1 - is directory, 0 - is file)
     * next 4 bytes contains file size
     * next 4 bytes contains first cluster of file (in this cluster must be write returning header)
     * last 2 bytes are reserved and do not used now
     *
     * @param name
     * @param clusterNumber
     * @param size
     * @param isDirectory
     * @return
     */
    public static byte[] getFileHeaderBytes(String name, int clusterNumber, int size, boolean isDirectory) {
        if (!isCorrectName(name)) {
            throw new IncorrectNameException();
        }
        byte[] result = new byte[FSConstants.FILE_HEADER_LENGTH];

        String nameWithSpaces = getNameWithSpaces(name);
        System.arraycopy(nameWithSpaces.getBytes(FSConstants.CHARSET), 0, result, 0, FSConstants.FILE_HEADER_LENGTH);
        result[FSConstants.FILE_NAME_LENGTH + 1] = (byte) (isDirectory ? 1 : 0);
        writeIntAsBytesToArray(result, FSConstants.FileHeaderOffsets.FILE_CLUSTER, clusterNumber);
        writeIntAsBytesToArray(result, FSConstants.FileHeaderOffsets.FILE_SIZE, size);
        return result;
    }

    public static void writeIntAsBytesToArray(byte[] destArr, int destPosition, int value) {
        byte[] bytes = intAsFourBytes(value);
        System.arraycopy(bytes, 0, destArr, destPosition, bytes.length);
    }

    public static String getNameWithSpaces(String name) {
        StringBuilder nameWithSpaces = new StringBuilder(name);
        while (nameWithSpaces.length() < FSConstants.FILE_NAME_LENGTH) {
            nameWithSpaces.append(" ");
        }
        return nameWithSpaces.toString();
    }

    public static boolean isCorrectName(String name) {
        return NAME_PATTERN.matcher(name).matches();
    }

}
