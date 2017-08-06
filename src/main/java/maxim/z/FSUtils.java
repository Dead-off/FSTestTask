package maxim.z;

import java.nio.ByteBuffer;

public class FSUtils {

    public static byte[] intAsFourBytes(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

}
