package maxim.z;

import maxim.z.exceptions.FSFormatException;
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
            BytesReaderWriter readerWriter = new RAFAdapter(fsFile);
            int clusterCount = readClusterCount(readerWriter);
            int clusterSize = readClusterSize(readerWriter);
            return getFileSystem(readerWriter, clusterCount, clusterSize);
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

    static FileSystem getFileSystem(BytesReaderWriter readerWriter, int clusterCount, int clusterSize) throws IOException {
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


    public void write(File file, String content) throws IOException {
        write(file, content.getBytes(FSConstants.CHARSET));
    }

    public void write(File file, byte[] content) throws IOException {
        int fileCluster = findFileCluster(file);
        int clusterOffset = getClusterOffset(fileCluster);
        int nextClusterInChain = FSUtils.readIntFromFsOnOffset(readerWriter, clusterOffset);
        readerWriter.seek(getClusterDataOffset(fileCluster));
        byte[] currentClusterData = new byte[clusterSize];
        readerWriter.readBytes(currentClusterData);
        FSFileEntry currentFile = FSFileEntry.fromByteArray(currentClusterData);
        currentFile.size = content.length;
        readerWriter.seek(getClusterDataOffset(fileCluster));
        readerWriter.write(currentFile.toByteArray());
        readerWriter.seek(getClusterDataOffset(fileCluster) + FSConstants.FILE_HEADER_LENGTH);
        if (content.length <= clusterSize - FSConstants.FILE_HEADER_LENGTH) {
            readerWriter.write(content);
        } else {
            readerWriter.write(Arrays.copyOfRange(content, 0, clusterSize - FSConstants.FILE_HEADER_LENGTH));
            if (nextClusterInChain == FSConstants.END_OF_CHAIN) {
                int freeCluster = getFirstFreeCluster();
                readerWriter.seek(clusterOffset);
                readerWriter.write(FSUtils.intAsFourBytes(freeCluster));
                nextClusterInChain = freeCluster;
            }
            int writeBytes = clusterSize - FSConstants.FILE_HEADER_LENGTH;
            while (true) {
                readerWriter.seek(getClusterDataOffset(nextClusterInChain));
                int bytesForWrite = (content.length - writeBytes > clusterSize) ? clusterSize : content.length - writeBytes;
                byte[] toWrite = Arrays.copyOfRange(content, writeBytes, bytesForWrite);
                readerWriter.write(toWrite);
                writeBytes += bytesForWrite;
                if (writeBytes == content.length) {
                    break;
                }
                if (nextClusterInChain == FSConstants.END_OF_CHAIN) {
                    readerWriter.seek(getClusterOffset(nextClusterInChain));
                    nextClusterInChain = getFirstFreeCluster();
                    readerWriter.write(FSUtils.intAsFourBytes(nextClusterInChain));
                } else {
                    nextClusterInChain = FSUtils.readIntFromFsOnOffset(readerWriter, getClusterOffset(nextClusterInChain));
                }
            }
        }
        if (nextClusterInChain != FSConstants.END_OF_CHAIN) {
            readerWriter.seek(getClusterOffset(nextClusterInChain));
            nextClusterInChain = FSUtils.readIntFromFsOnOffset(readerWriter, getClusterOffset(nextClusterInChain));
            readerWriter.seek(getClusterOffset(nextClusterInChain));
            readerWriter.write(FSUtils.intAsFourBytes(FSConstants.END_OF_CHAIN));
            nextClusterInChain = FSUtils.readIntFromFsOnOffset(readerWriter, getClusterOffset(nextClusterInChain));
            while (nextClusterInChain != FSConstants.END_OF_CHAIN) {
                readerWriter.seek(getClusterOffset(nextClusterInChain));
                readerWriter.write(FSUtils.intAsFourBytes(0));
                nextClusterInChain = FSUtils.readIntFromFsOnOffset(readerWriter, getClusterOffset(nextClusterInChain));
            }
            readerWriter.seek(getClusterOffset(nextClusterInChain));
            readerWriter.write(FSUtils.intAsFourBytes(0));
        }
    }

    public byte[] read(File file) throws IOException {
        int fileCluster = findFileCluster(file);
        return getFileContent(fileCluster);
    }

    public String readAsString(File file) throws IOException {
        return new String(read(file), FSConstants.CHARSET);
    }

    public void createFile(File parent, String newFileName) throws IOException {
        int freeCluster = getFirstFreeCluster();
        setFATClusterValue(freeCluster, FSConstants.END_OF_CHAIN);
        FSFileEntry newFile = FSFileEntry.from(newFileName, false, freeCluster);
        int clusterDataOffset = getClusterDataOffset(freeCluster);
        readerWriter.seek(clusterDataOffset);
        readerWriter.write(newFile.toByteArray());
        byte[] currentBytes = read(parent);
        byte[] newContent = Arrays.copyOf(currentBytes, currentBytes.length + 4);
        byte[] pointerToNewFile = FSUtils.intAsFourBytes(freeCluster);
        System.arraycopy(pointerToNewFile, 0, newContent, currentBytes.length, pointerToNewFile.length);
        write(parent, newContent);
    }

    private void setFATClusterValue(int clusterIndex, int clusterValue) throws IOException {
        readerWriter.seek(getClusterOffset(clusterIndex));
        readerWriter.write(FSUtils.intAsFourBytes(clusterValue));
    }

    public void createDirectory(File parent, String newDirectoryName) throws IOException {
        int freeCluster = getFirstFreeCluster();
        setFATClusterValue(freeCluster, FSConstants.END_OF_CHAIN);
        FSFileEntry newFile = FSFileEntry.from(newDirectoryName, true, freeCluster);
        readerWriter.seek(getClusterDataOffset(freeCluster));
        readerWriter.write(newFile.toByteArray());
        byte[] currentBytes = read(parent);
        byte[] newContent = Arrays.copyOf(currentBytes, currentBytes.length + 4);
        byte[] pointerToNewFile = FSUtils.intAsFourBytes(freeCluster);
        System.arraycopy(pointerToNewFile, 0, newContent, currentBytes.length, pointerToNewFile.length);
        write(parent, newContent);
    }

    private int getFirstFreeCluster() throws IOException {
        // TODO: 09.08.2017 use header value
        for (int i = 0; i < clusterCount; i++) {
            int clusterOffset = getClusterOffset(i);
            int nextClusterInChain = FSUtils.readIntFromFsOnOffset(readerWriter, clusterOffset);
            if (nextClusterInChain == 0) {
                return i;
            }
        }
        throw new IOException("DONT FOUND FREE CLUSTER");
    }

    public void removeFile(File file) throws IOException {
        int fileCluster = findFileCluster(file);
        int clusterOffset = getClusterOffset(fileCluster);

        readerWriter.seek(getClusterDataOffset(fileCluster));
        byte[] currentClusterData = new byte[clusterSize];
        readerWriter.readBytes(currentClusterData);
        FSFileEntry currentFile = FSFileEntry.fromByteArray(currentClusterData);
        currentFile.remove();
        readerWriter.seek(getClusterDataOffset(fileCluster));
        readerWriter.write(currentFile.toByteArray());
        String[] fileNames = file.parseFileNames();
        File parentFile = File.getFile(Arrays.stream(fileNames).limit(fileNames.length - 1).reduce((s1, s2) -> s1 + "/" + s2).orElse("/"));
        byte[] currentContent = read(parentFile);
        int idxLink = -1;
        for (int i = 0; i < currentContent.length; i += 4) {
            int clusterChildLink = FSUtils.intFromFourBytes(Arrays.copyOfRange(currentContent, i, i + FSConstants.BYTE_DEPTH));
            if (clusterChildLink == fileCluster) {
                idxLink = i;
                break;
            }
        }
        if (idxLink == -1) {
            throw new FSFormatException();
        }
        byte[] newContent = new byte[currentContent.length - FSConstants.BYTE_DEPTH];
        System.arraycopy(currentContent, 0, newContent, 0, idxLink);
        System.arraycopy(currentContent, idxLink + FSConstants.BYTE_DEPTH, newContent, idxLink, newContent.length - idxLink);
        write(parentFile, newContent);
        while (true) {
            int nextClusterInChain = FSUtils.readIntFromFsOnOffset(readerWriter, clusterOffset);
            readerWriter.seek(clusterOffset);
            readerWriter.write(FSUtils.intAsFourBytes(0));
            if (nextClusterInChain == FSConstants.END_OF_CHAIN) {
                break;
            }
        }
    }

    public List<String> getFilesList(File directory) throws IOException {
        int clusterNumber = findFileCluster(directory);
        int nextClusterInChain = FSUtils.readIntFromFsOnOffset(readerWriter, getClusterOffset(clusterNumber));
        readerWriter.seek(getClusterDataOffset(clusterNumber));
        byte[] currentClusterData = new byte[clusterSize];
        readerWriter.readBytes(currentClusterData);
        FSFileEntry currentFile = FSFileEntry.fromByteArray(currentClusterData);
        byte[] content = getFileContent(currentFile, nextClusterInChain, currentClusterData);
        List<Integer> childFilesClusters = new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (int i = 0; i < content.length; i += 4) {
            int childClusterNumber = FSUtils.intFromFourBytes(new byte[]{content[i], content[i + 1], content[i + 2], content[i + 3]});
            readerWriter.seek(getClusterDataOffset(childClusterNumber));
            currentClusterData = new byte[clusterSize];
            readerWriter.readBytes(currentClusterData);
            FSFileEntry childFile = FSFileEntry.fromByteArray(currentClusterData);
            if (!childFile.isRemoved()) {
                result.add(childFile.name);
            }
        }
        return result;
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
        for (int i = 0; i < content.length; i += 4) {
            childFilesClusters.add(FSUtils.intFromFourBytes(new byte[]{content[i], content[i + 1], content[i + 2], content[i + 3]}));
        }
        for (int clusterNum : childFilesClusters) {
            readerWriter.seek(getClusterDataOffset(clusterNum));
            // TODO: 09.08.2017 проверять, что файл не удален
            currentClusterData = new byte[clusterSize];
            readerWriter.readBytes(currentClusterData);
            FSFileEntry childFile = FSFileEntry.fromByteArray(currentClusterData);
            if (childFile.name.equals(currentName)) {
                return findFileCluster(clusterNum, fileNames, currentNameIdx + 1);
            }
        }
        throw new FileNotFoundException();
    }

    private byte[] getFileContent(int clusterNumber) throws IOException {
        int clusterOffset = getClusterOffset(clusterNumber);
        int nextClusterInChain = FSUtils.readIntFromFsOnOffset(readerWriter, clusterOffset);
        readerWriter.seek(getClusterDataOffset(clusterNumber));
        byte[] currentClusterData = new byte[clusterSize];
        readerWriter.readBytes(currentClusterData);
        FSFileEntry currentFile = FSFileEntry.fromByteArray(currentClusterData);
        return getFileContent(currentFile, nextClusterInChain, currentClusterData);
    }

    private byte[] getFileContent(FSFileEntry file, int nextCluster, byte[] clusterData) throws IOException {
        byte[] result = new byte[file.size];
        if (file.isRemoved()) {
            throw new FileNotFoundException();
        }
        int clusterOffset = getClusterOffset(file.clusterNumber);
        int firstClusterSize = (clusterSize - FSConstants.FILE_HEADER_LENGTH);
        boolean allContentInFirstCluster = firstClusterSize > file.size;
        System.arraycopy(clusterData, FSConstants.FILE_HEADER_LENGTH, result, 0,
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
