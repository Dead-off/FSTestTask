package maxim.z;

import java.util.Arrays;

class FSFileEntry {

    private final static int FILE_SIZE_OFFSET = 28;
    private final static int FILE_ATTRIBUTES_OFFSET = 20;
    private final static int FILE_CLUSTER_OFFSET = 24;

    private final static int DIRECTORY_ATTRIBUTE_BIT = 0B00000001;
    private final static int REMOVED_ATTRIBUTE_BIT = 0B00000010;

    String name;
    final boolean isDirectory;
    int size;
    final int clusterNumber;
    private boolean isRemoved;

    static final FSFileEntry EMPTY_ROOT = new FSFileEntry("", true, 0, 0, false);

    private FSFileEntry(String name, boolean isDirectory, int size, int clusterNumber, boolean isRemoved) {
        this.name = name;
        this.isDirectory = isDirectory;
        this.size = size;
        this.clusterNumber = clusterNumber;
        this.isRemoved = isRemoved;
    }

    void remove() {
        isRemoved=true;
    }

    boolean isRemoved() {
        return isRemoved;
    }

    // TODO: 09.08.2017 tests

    static FSFileEntry from(String name, boolean isDirectory, int clusterNumber) {
        return new FSFileEntry(name, isDirectory, 0, clusterNumber, false);
    }

    static FSFileEntry fromByteArray(byte[] array) {
        byte[] fileNameBytes = Arrays.copyOfRange(array, 0, FSConstants.FILE_NAME_LENGTH);
        String name = new String(fileNameBytes, FSConstants.CHARSET).trim();
        byte attributeByte = array[FILE_ATTRIBUTES_OFFSET];
        int clusterNumber = FSUtils.intFromFourBytes(Arrays.copyOfRange(array, FILE_CLUSTER_OFFSET, FILE_CLUSTER_OFFSET + FSConstants.BYTE_DEPTH));
        int fileSize = FSUtils.intFromFourBytes(Arrays.copyOfRange(array, FILE_SIZE_OFFSET, FILE_SIZE_OFFSET + FSConstants.BYTE_DEPTH));
        return new FSFileEntry(name, getDirectoryBoolean(attributeByte), fileSize, clusterNumber, getRemovedAttribute(attributeByte));
    }

    private static boolean getDirectoryBoolean(byte attributeByte) {
        return attribyteByMask(attributeByte, DIRECTORY_ATTRIBUTE_BIT);
    }

    private static boolean attribyteByMask(byte attributeByte, int mask) {
        return (attributeByte & mask) != 0;
    }

    private static boolean getRemovedAttribute(byte attributeByte) {
        return attribyteByMask(attributeByte, REMOVED_ATTRIBUTE_BIT);
    }

    byte[] toByteArray() {
        byte[] nameBytes = FSUtils.getNameWithSpaces(name).getBytes(FSConstants.CHARSET);
        byte attributes = getAttributeByte();
        byte[] result = new byte[FSConstants.FILE_HEADER_LENGTH];
        System.arraycopy(nameBytes, 0, result, 0, nameBytes.length);
        result[FILE_ATTRIBUTES_OFFSET] = attributes;
        System.arraycopy(FSUtils.intAsFourBytes(clusterNumber), 0, result, FILE_CLUSTER_OFFSET, FSConstants.BYTE_DEPTH);
        System.arraycopy(FSUtils.intAsFourBytes(size), 0, result, FILE_SIZE_OFFSET, FSConstants.BYTE_DEPTH);
        return result;
    }

    private byte getAttributeByte() {
        return (byte) ((isDirectory ? DIRECTORY_ATTRIBUTE_BIT : 0) + (isRemoved ? REMOVED_ATTRIBUTE_BIT : 0));
    }
}
