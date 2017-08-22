package maxim.z;

import java.io.Closeable;
import java.io.IOException;

/**
 * Interface for reading and writing bytes to any storage
 */
public interface BytesReaderWriter extends Closeable {

    /**
     * Write byte array to storage on offset, specify by seek method
     *
     * @param bytes - bytes for write
     * @throws IOException on any default IO error
     */
    void write(byte[] bytes) throws IOException;

    /**
     * Read data from storage on offset, specify by seek method into array. Read bytes count coincides with data array length
     * @param data - array to which data will be read
     * @throws IOException on any default IO error
     */
    void readBytes(byte[] data) throws IOException;

    /**
     * Combines methods read and write (seek invoke before write)
     * @param data - array to which data will be read
     * @param pos - offset position in storage
     * @throws IOException on any default IO error
     */
    void seekAndRead(byte[] data, long pos) throws IOException;

    /**
     * Combines methods seek and write (seek invoke before write)
     * @param bytes - bytes for write
     * @param pos - offset position in storage
     * @throws IOException on any default IO error
     */
    void seekAndWrite(byte[] bytes, long pos) throws IOException;

    /**
     * Set offset position in storage for write/read methods
     * @param pos - offset position in storage
     * @throws IOException on any default IO error
     */
    void seek(long pos) throws IOException;

}
