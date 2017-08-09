package maxim.z;

import maxim.z.exceptions.FileNotFoundException;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileSystem implements Closeable {

    private final BytesReaderWriter readerWriter;
    private final int clusterCount;
    private final int clusterSize;

    // TODO: 09.08.2017 need hard refactoring

    public FileSystem(BytesReaderWriter readerWriter, int clusterCount, int clusterSize) {
        this.readerWriter = readerWriter;
        this.clusterCount = clusterCount;
        this.clusterSize = clusterSize;
    }

    public static FileSystem getFileSystem(String pathToFile) throws IOException {
        java.io.File fsFile = new java.io.File(pathToFile);
        if (fsFile.exists()) {
            return getFileSystem(new RAFAdapter(fsFile));
        }
        createFSFile(fsFile);
        FileSystem fs = new FileSystem(new RAFAdapter(fsFile), FSConstants.DEFAULT_CLUSTER_COUNT, FSConstants.DEFAULT_CLUSTER_SIZE);
        fs.init();
        return fs;
    }

    private static void createFSFile(java.io.File fsFile) throws IOException {
        boolean res = fsFile.createNewFile();
        if (!res) {
            throw new IOException("Unable to create a file system. File system with this name already exist");
        }
    }

    static FileSystem getFileSystem(BytesReaderWriter readerWriter) throws IOException {
        int clusterCount = readClusterCount(readerWriter);
        int clusterSize = readClusterSize(readerWriter);
        return new FileSystem(readerWriter, clusterCount, clusterSize);
    }

    private static int readClusterSize(BytesReaderWriter readerWriter) throws IOException {
        return FSUtils.readIntFromFsOnOffset(readerWriter, FSConstants.Offsets.CLUSTERS_COUNT);
    }

    private static int readClusterCount(BytesReaderWriter readerWriter) throws IOException {
        return FSUtils.readIntFromFsOnOffset(readerWriter, FSConstants.Offsets.CLUSTER_SIZE);
    }

    void init() throws IOException {
        seekAndWrite(FSConstants.Offsets.CLUSTERS_COUNT, FSConstants.DEFAULT_CLUSTER_COUNT);
        seekAndWrite(FSConstants.Offsets.LAST_USED_CLUSTER, 0);
        seekAndWrite(FSConstants.Offsets.CLUSTER_SIZE, FSConstants.DEFAULT_CLUSTER_SIZE);
        seekAndWrite(FSConstants.Offsets.FAT_TABLE, FSConstants.END_OF_CHAIN);
        seekAndWrite(getClusterOffset(FSConstants.DEFAULT_CLUSTER_COUNT), FSFileEntry.EMPTY_ROOT.toByteArray());
    }

    private int getClusterOffset(int clusterIndex) {
        return FSConstants.Offsets.FAT_TABLE + clusterIndex * FSConstants.BYTE_DEPTH;
    }

    private void seekAndWrite(long pos, byte[] writeBytes) throws IOException {
        readerWriter.seek(pos);
        readerWriter.write(writeBytes);
    }

    private void seekAndWrite(long pos, int writeInt) throws IOException {
        seekAndWrite(pos, FSUtils.intAsFourBytes(writeInt));
    }


    public void write(File file, String content) {
        write(file, content.getBytes(FSConstants.CHARSET));
    }

    public void write(File file, byte[] content) {

    }

    public byte[] read(File file) {
        return new byte[0];
    }

    public String readAsString(File file) {
        return new String(read(file), FSConstants.CHARSET);
    }

    public void createFile(File file) {
//        String[] dirNames = file.
    }

    public void createDirectory(File directory) {
//        String[] dirNames = file.
    }

    public void removeFile(File file) {

    }

    private int findFileCluster(File file) throws IOException {
        String[] dirNames = file.parseFileNames();
        int rootCluster = 0;
        if (dirNames.length == 0) {
            return rootCluster;
        }
        return findFileCluster(rootCluster, dirNames, 0);

    }

    private int findFileCluster(int clusterOfCurrentFile, String[] fileNames, int currentNameIdx) throws IOException {
        if (currentNameIdx >= fileNames.length) {
            return clusterOfCurrentFile;
        }
        String currentName = fileNames[currentNameIdx];
        int clusterOffset = getClusterOffset(clusterOfCurrentFile);
        int nextClusterInChain = FSUtils.readIntFromFsOnOffset(readerWriter, clusterOffset);
        readerWriter.seek(getClusterDataOffset(clusterOfCurrentFile));
        // TODO: 09.08.2017 проверять, что файл не удален
        byte[] currentClusterData = new byte[clusterSize];
        readerWriter.readBytes(currentClusterData);
        FSFileEntry currentFile = FSFileEntry.fromByteArray(currentClusterData);
        byte[] content = getFileContent(currentFile, nextClusterInChain, currentClusterData);
        List<Integer> childFilesClusters = new ArrayList<>();
        for (int i = 0; i < content.length; i+=4) {
            childFilesClusters.add(FSUtils.intFromFourBytes(new byte[]{content[i],content[i+1],content[i+2],content[i+3]}));
        }
        for (int clusterNum : childFilesClusters) {
            readerWriter.seek(getClusterDataOffset(clusterNum));
            // TODO: 09.08.2017 проверять, что файл не удален
            currentClusterData = new byte[clusterSize];
            readerWriter.readBytes(currentClusterData);
            FSFileEntry childFile = FSFileEntry.fromByteArray(currentClusterData);
            if (childFile.name.equals(currentName)) {
                findFileCluster(clusterNum, fileNames, currentNameIdx + 1);
            }
        }
        throw new FileNotFoundException();
    }

    private byte[] getFileContent(FSFileEntry file, int nextCluster, byte[] clusterData) throws IOException {
        byte[] result = new byte[file.size];
        if (file.isRemoved) {
            throw new FileNotFoundException();
        }
        int clusterOffset = getClusterOffset(file.clusterNumber);
        int firstClusterSize = (clusterSize - FSConstants.FILE_HEADER_LENGTH);
        boolean allContentInFirstCluster = firstClusterSize > file.size;
        System.arraycopy(clusterData, 0, result, 0,
                allContentInFirstCluster ? result.length : firstClusterSize);
        if (allContentInFirstCluster) {
            return result;
        }
        int readBytesCount = firstClusterSize;
        while (readBytesCount != file.size) {
            if (nextCluster == FSConstants.END_OF_CHAIN) {
                //todo error???
            }
            int nextClusterOffset = getClusterDataOffset(nextCluster);
            int fatClusterOffset = getClusterOffset(nextCluster);
            nextCluster = FSUtils.readIntFromFsOnOffset(readerWriter, fatClusterOffset);
            readerWriter.seek(nextClusterOffset);
            byte[] currentClusterData = new byte[clusterSize];
            readerWriter.readBytes(currentClusterData);
            int bytesToRead = readBytesCount + clusterSize > file.size ? (file.size - readBytesCount) : clusterSize;
            System.arraycopy(currentClusterData, 0, result, readBytesCount, bytesToRead);
            readBytesCount += bytesToRead;
        }
        return result;
    }

    private byte[] getDataBytesWithoutHeader(byte[] dataBytes) {
        return Arrays.copyOfRange(dataBytes, FSConstants.FILE_HEADER_LENGTH, dataBytes.length);
    }

    private int getClusterDataOffset(int clusterNumber) {
        return FSConstants.Offsets.FAT_TABLE + FSConstants.BYTE_DEPTH * clusterCount + clusterNumber * clusterSize;
    }

    @Override
    public void close() throws IOException {
        readerWriter.close();
    }
}
