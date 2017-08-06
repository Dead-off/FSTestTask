package maxim.z;

import java.nio.ByteBuffer;

public class FileSystemHelper {

    public static byte[] intAsFourBytes(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

}
