package maxim.z;

import maxim.z.exceptions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileSystem implements IFileSystem {

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

    private void clearFATChain(int firstCluster) throws IOException {
        int curCluster = firstCluster;
        int nextCluster;
        do {
            nextCluster = FSUtils.readIntFromFsOnOffset(readerWriter, getClusterOffset(curCluster));
            readerWriter.seek(getClusterOffset(curCluster));
            readerWriter.write(FSUtils.intAsFourBytes(0));
            curCluster = nextCluster;
        } while (nextCluster != FSConstants.END_OF_CHAIN);
    }

    private void createFATChain(List<Integer> clusterIndexes) throws IOException {
        for (int i = 0; i < clusterIndexes.size() - 1; i++) {
            int curClusterIndex = clusterIndexes.get(i);
            int nextClusterIndex = clusterIndexes.get(i + 1);
            readerWriter.seek(getClusterOffset(curClusterIndex));
            readerWriter.write(FSUtils.intAsFourBytes(nextClusterIndex));
        }
        int lastCluster = clusterIndexes.get(clusterIndexes.size() - 1);
        readerWriter.seek(getClusterOffset(lastCluster));
        readerWriter.write(FSUtils.intAsFourBytes(FSConstants.END_OF_CHAIN));
    }


    @Override
    public void write(IFile file, String content) throws IOException {
        write(file, content.getBytes(FSConstants.CHARSET));
    }

    @Override
    public void write(IFile file, byte[] content) throws IOException {
        int fileCluster = findFileCluster(file);
        readerWriter.seek(getClusterDataOffset(fileCluster));
        byte[] currentClusterData = new byte[clusterSize];
        readerWriter.readBytes(currentClusterData);
        FSFileEntry currentFile = FSFileEntry.fromByteArray(currentClusterData);
        if (currentFile.isDirectory) {
            throw new WriteException(String.format("file %s is a directory", file.getPath()));
        }
        write0(content, currentFile);
    }

    private void write0(byte[] content, FSFileEntry currentFile) throws IOException {
        int fileCluster = currentFile.clusterNumber;
        currentFile.size = content.length;
        byte[] contentWithHeader = getContentWithHeader(content, currentFile.toByteArray());

        clearFATChain(fileCluster);
        int writeBytes = 0;
        List<Integer> usedClusterIndexes = new ArrayList<>();
        int clusterForWrite = fileCluster;
        do {
            readerWriter.seek(getClusterDataOffset(clusterForWrite));
            int bytesToWrite = Math.min(clusterSize, contentWithHeader.length - writeBytes);
            readerWriter.write(Arrays.copyOfRange(contentWithHeader, writeBytes, writeBytes + bytesToWrite));
            usedClusterIndexes.add(clusterForWrite);
            clusterForWrite = getFirstFreeCluster(clusterForWrite + 1);//todo bad, better is set temp value eg -2, but try find with start from 0
            writeBytes += bytesToWrite;
        } while (writeBytes != contentWithHeader.length);

        createFATChain(usedClusterIndexes);
    }

    private byte[] getContentWithHeader(byte[] content, byte[] header) {
        byte[] result = new byte[content.length + FSConstants.FILE_HEADER_LENGTH];
        System.arraycopy(content, 0, result, FSConstants.FILE_HEADER_LENGTH, content.length);
        System.arraycopy(header, 0, result, 0, header.length);
        return result;
    }

    @Override
    public byte[] read(IFile file) throws IOException {
        int fileCluster = findFileCluster(file);
        FSFileEntry fileEntry = getFileEntryFromCluster(fileCluster);
        if (fileEntry.isDirectory) {
            throw new ReadException(String.format("file %s is a directory", file.getPath()));
        }
        return getFileContent(fileCluster);
    }

    @Override
    public String readAsString(IFile file) throws IOException {
        return new String(read(file), FSConstants.CHARSET);
    }

    @Override
    public IFile createFile(IFile parent, String newFileName) throws IOException {
        checkName(newFileName);
        int parentCluster = findFileCluster(parent);
        int clusterForNewFile = getFirstFreeCluster();
        FSFileEntry parentFile = getFileEntryFromCluster(parentCluster);
        if (!parentFile.isDirectory) {
            throw new CreateFileException(String.format("parent file %s is a file", parent.getPath()));
        }
        setFATClusterValue(clusterForNewFile, FSConstants.END_OF_CHAIN);
        FSFileEntry newFile = FSFileEntry.from(newFileName, false, clusterForNewFile);
        int clusterDataOffset = getClusterDataOffset(clusterForNewFile);
        readerWriter.seek(clusterDataOffset);
        readerWriter.write(newFile.toByteArray());
        appendClusterLinkToDirectory(parentCluster, clusterForNewFile);
        return parent.child(newFileName);
    }

    private void checkName(String name) {
        if (!FSUtils.isCorrectName(name)) {
            throw new IncorrectNameException();
        }
    }

    private void appendClusterLinkToDirectory(int directoryCluster, int fileCluster) throws IOException {
        byte[] currentBytes = getFileContent(directoryCluster);
        byte[] newContent = Arrays.copyOf(currentBytes, currentBytes.length + 4);
        byte[] pointerToNewFile = FSUtils.intAsFourBytes(fileCluster);
        System.arraycopy(pointerToNewFile, 0, newContent, currentBytes.length, pointerToNewFile.length);
        write0(newContent, getFileEntryFromCluster(directoryCluster));
    }

    private void setFATClusterValue(int clusterIndex, int clusterValue) throws IOException {
        readerWriter.seek(getClusterOffset(clusterIndex));
        readerWriter.write(FSUtils.intAsFourBytes(clusterValue));
    }

    @Override
    public IFile createDirectory(IFile parent, String newDirectoryName) throws IOException {
        checkName(newDirectoryName);
        int parentCluster = findFileCluster(parent);
        int newDirectoryCluster = getFirstFreeCluster();
        setFATClusterValue(newDirectoryCluster, FSConstants.END_OF_CHAIN);
        FSFileEntry newFile = FSFileEntry.from(newDirectoryName, true, newDirectoryCluster);
        readerWriter.seek(getClusterDataOffset(newDirectoryCluster));
        readerWriter.write(newFile.toByteArray());
        appendClusterLinkToDirectory(parentCluster, newDirectoryCluster);
        return parent.child(newDirectoryName);
    }

    private int getFirstFreeCluster(int startFrom) throws IOException {
        for (int i = startFrom; i < clusterCount; i++) {
            int clusterOffset = getClusterOffset(i);
            int nextClusterInChain = FSUtils.readIntFromFsOnOffset(readerWriter, clusterOffset);
            if (nextClusterInChain == 0) {
                return i;
            }
        }
        throw new IOException("Don't found free cluster");
    }

    private FSFileEntry getFileEntryFromCluster(int clusterNumber) throws IOException {
        readerWriter.seek(getClusterDataOffset(clusterNumber));
        byte[] currentClusterData = new byte[clusterSize];
        readerWriter.readBytes(currentClusterData);
        return FSFileEntry.fromByteArray(currentClusterData);
    }

    private int getFirstFreeCluster() throws IOException {
        return getFirstFreeCluster(0);
    }

    @Override
    public void removeFile(IFile file) throws IOException {
        int fileCluster = findFileCluster(file);
        int clusterOffset = getClusterOffset(fileCluster);
        IFile parentFile = file.parent();
        int parentCluster = findFileCluster(parentFile);
        readerWriter.seek(getClusterDataOffset(fileCluster));
        byte[] currentClusterData = new byte[clusterSize];
        readerWriter.readBytes(currentClusterData);
        FSFileEntry currentFile = FSFileEntry.fromByteArray(currentClusterData);
        currentFile.remove();
        readerWriter.seek(getClusterDataOffset(fileCluster));
        readerWriter.write(currentFile.toByteArray());

        byte[] currentContent = getFileContent(parentCluster);
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
        write0(newContent, getFileEntryFromCluster(parentCluster));
        while (true) {
            int nextClusterInChain = FSUtils.readIntFromFsOnOffset(readerWriter, clusterOffset);
            readerWriter.seek(clusterOffset);
            readerWriter.write(FSUtils.intAsFourBytes(0));
            if (nextClusterInChain == FSConstants.END_OF_CHAIN) {
                break;
            }
        }
    }

    @Override
    public List<String> getFilesList(IFile directory) throws IOException {
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

    private int findFileCluster(IFile file) throws IOException {
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
        byte[] currentClusterData = new byte[clusterSize];
        readerWriter.readBytes(currentClusterData);
        FSFileEntry currentFile = FSFileEntry.fromByteArray(currentClusterData);
        checkThatFileIsNotRemoved(currentFile);
        byte[] content = getFileContent(currentFile, nextClusterInChain, currentClusterData);
        List<Integer> childFilesClusters = new ArrayList<>();
        for (int i = 0; i < content.length; i += 4) {
            childFilesClusters.add(FSUtils.intFromFourBytes(new byte[]{content[i], content[i + 1], content[i + 2], content[i + 3]}));
        }
        for (int clusterNum : childFilesClusters) {
            readerWriter.seek(getClusterDataOffset(clusterNum));
            currentClusterData = new byte[clusterSize];
            readerWriter.readBytes(currentClusterData);
            FSFileEntry childFile = FSFileEntry.fromByteArray(currentClusterData);
            checkThatFileIsNotRemoved(childFile);
            if (childFile.name.equals(currentName)) {
                return findFileCluster(clusterNum, fileNames, currentNameIdx + 1);
            }
        }
        throw new FileNotFoundException();
    }

    private void checkThatFileIsNotRemoved(FSFileEntry file) {
        if (file.isRemoved()) {
            throw new FileNotFoundException();
        }
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
