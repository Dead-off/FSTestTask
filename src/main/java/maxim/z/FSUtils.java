package maxim.z;

import maxim.z.exceptions.EmptyFileNameException;
import maxim.z.exceptions.IncorrectFilePath;
import maxim.z.exceptions.IncorrectNameException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class FSUtils {

    private final static Pattern NAME_PATTERN = Pattern.compile(String.format("^[A-Za-z0-9-_]{1,%s}$", FSConstants.FILE_NAME_LENGTH));

    public static byte[] intAsFourBytes(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static int intFromFourBytes(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    public static int readIntFromFsOnOffset(BytesReaderWriter readerWriter, int offset) throws IOException {
        readerWriter.seek(offset);
        byte[] bytes = new byte[FSConstants.BYTE_DEPTH];
        readerWriter.readBytes(bytes);
        return intFromFourBytes(bytes);
    }

//    public static byte[] getFileHeaderBytes(String name, int clusterNumber, int size, boolean isDirectory) {
//        if (!isCorrectName(name)) {
//            throw new IncorrectNameException();
//        }
//        byte[] result = new byte[FSConstants.FILE_HEADER_LENGTH];
//
//        String nameWithSpaces = getNameWithSpaces(name);
//        System.arraycopy(nameWithSpaces.getBytes(FSConstants.CHARSET), 0, result, 0, FSConstants.FILE_NAME_LENGTH);
//        result[FSConstants.FILE_NAME_LENGTH + 1] = (byte) (isDirectory ? 1 : 0);
//        writeIntAsBytesToArray(result, FSConstants.FileHeaderOffsets.FILE_CLUSTER, clusterNumber);
//        if (!isDirectory) {
//            writeIntAsBytesToArray(result, FSConstants.FileHeaderOffsets.FILE_SIZE, size);
//        }
//        return result;
//    }

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
