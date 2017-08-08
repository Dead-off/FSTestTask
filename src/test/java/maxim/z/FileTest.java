package maxim.z;

import maxim.z.exceptions.IncorrectFilePath;
import maxim.z.exceptions.IncorrectNameException;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class FileTest {

    @Test
    public void parseFileNamesTest() {
        File file = File.getFile("/test/from");
        String[] actual = file.parseFileNames();
        String[] expected = new String[]{"test", "from"};
        assertArrayEquals(expected, actual);

        file = File.getFile("/test/from/test1");
        actual = file.parseFileNames();
        expected = new String[]{"test", "from", "test1"};
        assertArrayEquals(expected, actual);

        file = File.getFile("/");
        actual = file.parseFileNames();
        expected = new String[]{};
        assertArrayEquals(expected, actual);

        file = File.getFile("/abcd");
        actual = file.parseFileNames();
        expected = new String[]{"abcd"};
        assertArrayEquals(expected, actual);
    }

    @Test(expected = IncorrectNameException.class)
    public void parseFileNamesIncorrectNameTest() {
        File.getFile("/?").parseFileNames();
    }

    @Test(expected = IncorrectFilePath.class)
    public void parseFileNamesIgnoreRootTest() {
        File.getFile("abc").parseFileNames();
    }


}
