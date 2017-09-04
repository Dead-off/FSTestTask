package maxim.z;

import java.io.IOException;

public interface VirtualOutputStream {

    void write(byte[] bytes) throws IOException;

}
