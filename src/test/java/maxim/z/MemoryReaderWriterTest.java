package maxim.z;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;

public class MemoryReaderWriterTest {

    @Test
    public void test() throws IOException {
        BytesReaderWriter brw = new MemoryReaderWriter(500);
        byte[] expected = new byte[]{1, 2, 3};
        brw.write(expected);
        byte[] actual = new byte[3];
        brw.readBytes(actual);
        assertArrayEquals(expected, actual);

        brw.seek(1);
        brw.readBytes(actual);
        expected = new byte[]{2, 3, 0};
        assertArrayEquals(expected, actual);

        brw.seek(2);
        brw.write(new byte[]{4, 5, 6});

        brw.seek(0);
        actual = new byte[6];
        brw.readBytes(actual);
        expected = new byte[]{1, 2, 4, 5, 6, 0};
        assertArrayEquals(expected, actual);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void arrayIndexOutOfBoundsExceptionTest() throws IOException {
        BytesReaderWriter brw = new MemoryReaderWriter(500);
        brw.seek(490);
        brw.readBytes(new byte[50]);
    }

}
