package maxim.z;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class FSUtilsTest {

    @Test
    public void intAsFourBytes() {
        byte[] actual = FSUtils.intAsFourBytes(0);
        byte[] expected = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 0};
        assertArrayEquals(expected, actual);

        actual = FSUtils.intAsFourBytes(1);
        expected = new byte[]{(byte) 0, (byte) 0, (byte) 0, (byte) 1};
        assertArrayEquals(expected, actual);

        actual = FSUtils.intAsFourBytes(257);
        expected = new byte[]{(byte) 0, (byte) 0, (byte) 1, (byte) 1};
        assertArrayEquals(expected, actual);

        actual = FSUtils.intAsFourBytes(655617);
        expected = new byte[]{(byte) 0, (byte) 0x0A, (byte) 1, (byte) 1};
        assertArrayEquals(expected, actual);

        actual = FSUtils.intAsFourBytes(-655361);
        expected = new byte[]{(byte) 0xFF, (byte) 0xF5, (byte) 0xFF, (byte) 0xFF};
        assertArrayEquals(expected, actual);

        actual = FSUtils.intAsFourBytes(Integer.MAX_VALUE);
        expected = new byte[]{(byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        assertArrayEquals(expected, actual);

        actual = FSUtils.intAsFourBytes(Integer.MIN_VALUE);
        expected = new byte[]{(byte) 0x80, (byte) 0, (byte) 0, (byte) 0};
        assertArrayEquals(expected, actual);
    }

}
