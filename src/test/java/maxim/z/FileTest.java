package maxim.z;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FileTest {

    @Test
    public void getFileTest() {
        File firstFile = File.getFile("/test");
        assertEquals(false, firstFile.isDirectory);
        assertEquals("/test", firstFile.path);

        File secondFile = File.getFile("test///file");
        assertEquals(false, secondFile.isDirectory);
        assertEquals("test///file", secondFile.path);
    }

    @Test
    public void getDirectoryTest() {
        File firstDir = File.getDirectory("/test///");
        assertEquals(true, firstDir.isDirectory);
        assertEquals("/test///", firstDir.path);

        File secondDirectory = File.getDirectory("test//directory");
        assertEquals(true, secondDirectory.isDirectory);
        assertEquals("test//directory", secondDirectory.path);
    }
}
