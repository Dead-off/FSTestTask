package maxim.z;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class FSFileEntryTest {

    // TODO: 09.08.2017 tests
//
//    @Test
//    public void fromToByteArrayTest() {
//        String fileName = "test-file";
//        byte[] expected = new byte[FSConstants.FILE_HEADER_LENGTH];
//        FSFileEntry.fromByteArray()
//        byte[] actual = FSUtils.getFileHeaderBytes(fileName, 3, 15, true);
//        byte[] expected = new byte[FSConstants.FILE_HEADER_LENGTH];
//        System.arraycopy(fileName.getBytes(FSConstants.CHARSET), 0, expected, 0, fileName.length());
//        StringBuilder spaces = new StringBuilder();
//        while (spaces.length() + fileName.length() < FSConstants.FILE_NAME_LENGTH) {
//            spaces.append(" ");
//        }
//        System.arraycopy(spaces.toString().getBytes(FSConstants.CHARSET), 0,
//                expected, fileName.length(), FSConstants.FILE_NAME_LENGTH - fileName.length());
//        expected[FSConstants.FILE_NAME_LENGTH + 1] = 1;
//        System.arraycopy(FSUtils.intAsFourBytes(3), 0, expected, FSConstants.FileHeaderOffsets.FILE_CLUSTER, 4);
//        System.arraycopy(FSUtils.intAsFourBytes(0), 0, expected, FSConstants.FileHeaderOffsets.FILE_SIZE, 4);
//        assertArrayEquals(expected, actual);
//
//        actual = FSUtils.getFileHeaderBytes(fileName, 4, 16, false);
//        expected[FSConstants.FILE_NAME_LENGTH + 1] = 0;
//        System.arraycopy(FSUtils.intAsFourBytes(4), 0, expected, FSConstants.FileHeaderOffsets.FILE_CLUSTER, 4);
//        System.arraycopy(FSUtils.intAsFourBytes(16), 0, expected, FSConstants.FileHeaderOffsets.FILE_SIZE, 4);
//        assertArrayEquals(expected, actual);
//    }

}
