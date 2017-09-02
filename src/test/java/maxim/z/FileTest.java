package maxim.z;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class FileTest {

    @Test
    public void parseFileNamesTest() {
        FileImpl file = FileImpl.fromPath("test/from");
        String[] actual = file.parseFileNames();
        String[] expected = new String[]{"test", "from"};
        assertArrayEquals(expected, actual);

        file = FileImpl.fromPath("test/from/test1");
        actual = file.parseFileNames();
        expected = new String[]{"test", "from", "test1"};
        assertArrayEquals(expected, actual);

        file = FileImpl.fromPath("");
        actual = file.parseFileNames();
        expected = new String[]{};
        assertArrayEquals(expected, actual);

        file = FileImpl.fromPath("abcd");
        actual = file.parseFileNames();
        expected = new String[]{"abcd"};
        assertArrayEquals(expected, actual);
    }
}
