package maxim.z;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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


    @Test
    public void getFileHeaderBytesTest() {
        String fileName = "test-file";
        byte[] actual = FSUtils.getFileHeaderBytes(fileName, 3, 15, true);
        byte[] expected = new byte[FSConstants.FILE_HEADER_LENGTH];
        System.arraycopy(fileName.getBytes(FSConstants.CHARSET), 0, expected, 0, fileName.length());
        StringBuilder spaces = new StringBuilder();
        while (spaces.length() + fileName.length() < FSConstants.FILE_NAME_LENGTH) {
            spaces.append(" ");
        }
        System.arraycopy(spaces.toString().getBytes(FSConstants.CHARSET), 0,
                expected, fileName.length(), FSConstants.FILE_NAME_LENGTH - fileName.length());
        expected[FSConstants.FILE_NAME_LENGTH + 1] = 1;
        System.arraycopy(FSUtils.intAsFourBytes(3), 0, expected, FSConstants.FileHeaderOffsets.FILE_CLUSTER, 4);
        System.arraycopy(FSUtils.intAsFourBytes(0), 0, expected, FSConstants.FileHeaderOffsets.FILE_SIZE, 4);
        assertArrayEquals(expected, actual);

        actual = FSUtils.getFileHeaderBytes(fileName, 4, 16, false);
        expected[FSConstants.FILE_NAME_LENGTH + 1] = 0;
        System.arraycopy(FSUtils.intAsFourBytes(4), 0, expected, FSConstants.FileHeaderOffsets.FILE_CLUSTER, 4);
        System.arraycopy(FSUtils.intAsFourBytes(16), 0, expected, FSConstants.FileHeaderOffsets.FILE_SIZE, 4);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void writeIntAsBytesToArrayTest() {
        byte[] actual = new byte[5];
        byte[] expected = new byte[]{0, 1, 0, 1, 0};
        FSUtils.writeIntAsBytesToArray(actual, 0, 65537);
        assertArrayEquals(expected, actual);

        actual = new byte[5];
        expected = new byte[]{0, 0, 1, 1, 4};
        FSUtils.writeIntAsBytesToArray(actual, 1, 65536 + 256 + 4);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void getNameWithSpacesTest() {
        assertEquals("                    ", FSUtils.getNameWithSpaces(""));
        assertEquals("123                 ", FSUtils.getNameWithSpaces("123"));
        assertEquals("abcQWERTY           ", FSUtils.getNameWithSpaces("abcQWERTY"));
        assertEquals("abcdeabcdeabcdeabcde", FSUtils.getNameWithSpaces("abcdeabcdeabcdeabcde"));
    }

    @Test
    public void isCorrectNameTest() {
        assertEquals(true, FSUtils.isCorrectName("a"));
        assertEquals(true, FSUtils.isCorrectName("abc"));
        assertEquals(true, FSUtils.isCorrectName("AAabc123-_-"));
        assertEquals(false, FSUtils.isCorrectName(""));
        assertEquals(false, FSUtils.isCorrectName("a b"));
        assertEquals(false, FSUtils.isCorrectName("abcdeabcdttteabcdeabcde"));
        assertEquals(true, FSUtils.isCorrectName("abcdeabcdeabcdeabcde"));
    }
}
