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
    public void parseFileNamesTest() {
        VirtualFile file = FileImpl.fromPath("test/from", null);
        String[] actual = FSUtils.parseFileNames(file);
        String[] expected = new String[]{"test", "from"};
        assertArrayEquals(expected, actual);

        file = FileImpl.fromPath("test/from/test1", null);
        actual = FSUtils.parseFileNames(file);
        expected = new String[]{"test", "from", "test1"};
        assertArrayEquals(expected, actual);

        file = FileImpl.fromPath("", null);
        actual = FSUtils.parseFileNames(file);
        expected = new String[]{};
        assertArrayEquals(expected, actual);

        file = FileImpl.fromPath("abcd", null);
        actual = FSUtils.parseFileNames(file);
        expected = new String[]{"abcd"};
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
        assertEquals(true, FSUtils.isCorrectName("a b"));
        assertEquals(false, FSUtils.isCorrectName("  "));
        assertEquals(false, FSUtils.isCorrectName("abcdeabcdttteabcdeabcde"));
        assertEquals(true, FSUtils.isCorrectName("abcdeabcdeabcdeabcde"));
        assertEquals(true, FSUtils.isCorrectName("abcdeabcd abcdeabcde"));
        assertEquals(true, FSUtils.isCorrectName("abcdeabcdabcdeabcde "));
        assertEquals(false, FSUtils.isCorrectName(" bcdeabcdabcdeabcde "));
        assertEquals(false, FSUtils.isCorrectName("abcdeabcda bcdeabcde "));
    }
}
