package maxim.z;

import maxim.z.exceptions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static maxim.z.FSUtils.intAsFourBytes;

public class FileSystem implements IFileSystem {

    private final BytesReaderWriter readerWriter;
    private final int clusterCount;
    private final int clusterSize;

    private FileSystem(BytesReaderWriter readerWriter, int clusterCount, int clusterSize) {
        this.readerWriter = readerWriter;
        this.clusterCount = clusterCount;
        this.clusterSize = clusterSize;
    }

    public static FileSystem getFileSystem(String pathToFile) throws IOException {
        java.io.File fsFile = new java.io.File(pathToFile);
        if (fsFile.exists()) {
            BytesReaderWriter readerWriter = new RAFWrapper(fsFile);
            int clusterCount = readClusterCount(readerWriter);
            int clusterSize = readClusterSize(readerWriter);
            return getFileSystem(readerWriter, clusterCount, clusterSize);
        }
        createFSFile(fsFile);
        FileSystem fs = new FileSystem(new RAFWrapper(fsFile), FSConstants.DEFAULT_CLUSTER_COUNT, FSConstants.DEFAULT_CLUSTER_SIZE);
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
        return readIntFromFsOnOffset(readerWriter, FSConstants.Offsets.CLUSTERS_COUNT);
    }

    private static int readClusterCount(BytesReaderWriter readerWriter) throws IOException {
        return readIntFromFsOnOffset(readerWriter, FSConstants.Offsets.CLUSTER_SIZE);
    }

    private static int readIntFromFsOnOffset(BytesReaderWriter readerWriter, int offset) throws IOException {
        readerWriter.seek(offset);
        byte[] bytes = new byte[FSConstants.BYTE_DEPTH];
        readerWriter.readBytes(bytes);
        return FSUtils.intFromFourBytes(bytes);
    }

    void init() throws IOException {
        readerWriter.seekAndWrite(intAsFourBytes(clusterCount), FSConstants.Offsets.CLUSTERS_COUNT);
        readerWriter.seekAndWrite(intAsFourBytes(0), FSConstants.Offsets.LAST_USED_CLUSTER);
        readerWriter.seekAndWrite(intAsFourBytes(clusterSize), FSConstants.Offsets.CLUSTER_SIZE);
        readerWriter.seekAndWrite(intAsFourBytes(FSConstants.END_OF_CHAIN), FSConstants.Offsets.FAT_TABLE);
        readerWriter.seekAndWrite(FSFileEntry.EMPTY_ROOT.toByteArray(), getClusterDataOffset(0));
    }

    private int getClusterFATOffset(int clusterIndex) {
        return FSConstants.Offsets.FAT_TABLE + clusterIndex * FSConstants.BYTE_DEPTH;
    }

    private void clearFATChain(int firstCluster) throws IOException {
        int clearedCluster = firstCluster;
        int nextCluster;
        do {
            nextCluster = readIntFromFsOnOffset(readerWriter, getClusterFATOffset(clearedCluster));
            setFATClusterValue(clearedCluster, 0);
            clearedCluster = nextCluster;
        } while (nextCluster != FSConstants.END_OF_CHAIN);
    }

    private void createFATChain(List<Integer> clusterIndexes) throws IOException {
        for (int i = 0; i < clusterIndexes.size(); i++) {
            int currentClusterIndex = clusterIndexes.get(i);
            boolean isLastCluster = (i == clusterIndexes.size() - 1);
            int nextClusterIndex = isLastCluster ? FSConstants.END_OF_CHAIN : clusterIndexes.get(i + 1);
            setFATClusterValue(currentClusterIndex, nextClusterIndex);
        }
    }

    @Override
    public void write(IFile file, String content) throws IOException {
        write(file, content.getBytes(FSConstants.CHARSET));
    }

    @Override
    public void write(IFile file, byte[] content) throws IOException {
        int fileCluster = findFileCluster(file);
        FSFileEntry currentFile = getFileEntryFromCluster(fileCluster);
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
            int writeBytesCount = Math.min(clusterSize, contentWithHeader.length - writeBytes);
            readerWriter.seekAndWrite(Arrays.copyOfRange(contentWithHeader, writeBytes, writeBytes + writeBytesCount), getClusterDataOffset(clusterForWrite));
            usedClusterIndexes.add(clusterForWrite);
            clusterForWrite = getFirstFreeCluster(clusterForWrite + 1);
            writeBytes += writeBytesCount;
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
        checkThatFileIsDirectory(parentFile, parent.getPath());
        setFATClusterValue(clusterForNewFile, FSConstants.END_OF_CHAIN);
        FSFileEntry newFile = FSFileEntry.from(newFileName, false, clusterForNewFile);
        int clusterDataOffset = getClusterDataOffset(clusterForNewFile);
        readerWriter.seekAndWrite(newFile.toByteArray(), clusterDataOffset);
        appendClusterLinkToDirectory(parentCluster, clusterForNewFile, parentFile);
        return parent.child(newFileName);
    }

    private void checkName(String name) {
        if (!FSUtils.isCorrectName(name)) {
            throw new IncorrectNameException();
        }
    }

    private void appendClusterLinkToDirectory(int directoryCluster, int fileCluster, FSFileEntry directory) throws IOException {
        byte[] currentBytes = getFileContent(directoryCluster);
        byte[] newContent = Arrays.copyOf(currentBytes, currentBytes.length + 4);
        byte[] pointerToNewFile = intAsFourBytes(fileCluster);
        System.arraycopy(pointerToNewFile, 0, newContent, currentBytes.length, pointerToNewFile.length);
        write0(newContent, directory);
    }

    private void setFATClusterValue(int clusterIndex, int clusterValue) throws IOException {
        readerWriter.seekAndWrite(intAsFourBytes(clusterValue), getClusterFATOffset(clusterIndex));
    }

    private void checkThatFileIsDirectory(FSFileEntry file, String path) {
        if (!file.isDirectory) {
            throw new CreateFileException(String.format("parent file %s is a file", path));
        }
    }

    @Override
    public IFile createDirectory(IFile parent, String newDirectoryName) throws IOException {
        checkName(newDirectoryName);
        int parentCluster = findFileCluster(parent);
        int newDirectoryCluster = getFirstFreeCluster();
        FSFileEntry parentFile = getFileEntryFromCluster(parentCluster);
        checkThatFileIsDirectory(parentFile, parent.getPath());
        setFATClusterValue(newDirectoryCluster, FSConstants.END_OF_CHAIN);
        FSFileEntry newFile = FSFileEntry.from(newDirectoryName, true, newDirectoryCluster);
        readerWriter.seekAndWrite(newFile.toByteArray(), getClusterDataOffset(newDirectoryCluster));
        appendClusterLinkToDirectory(parentCluster, newDirectoryCluster, parentFile);
        return parent.child(newDirectoryName);
    }

    private int getFirstFreeCluster(int startFrom) throws IOException {
        for (int i = startFrom; i < clusterCount; i++) {
            int clusterOffset = getClusterFATOffset(i);
            int nextClusterInChain = readIntFromFsOnOffset(readerWriter, clusterOffset);
            if (nextClusterInChain == 0) {
                return i;
            }
        }
        //if not find from start, try find from 0
        for (int i = 0; i < startFrom; i++) {
            int clusterOffset = getClusterFATOffset(i);
            int nextClusterInChain = readIntFromFsOnOffset(readerWriter, clusterOffset);
            if (nextClusterInChain == 0) {
                return i;
            }
        }
        throw new FSException("Don't found free cluster");
    }

    private FSFileEntry getFileEntryFromCluster(int clusterNumber) throws IOException {
        readerWriter.seek(getClusterDataOffset(clusterNumber));
        byte[] currentClusterData = new byte[FSConstants.FILE_HEADER_LENGTH];
        readerWriter.readBytes(currentClusterData);
        return FSFileEntry.fromByteArray(currentClusterData);
    }

    private int getFirstFreeCluster() throws IOException {
        return getFirstFreeCluster(0);
    }

    @Override
    public void removeFile(IFile file) throws IOException {
        int fileCluster = findFileCluster(file);
        IFile parentFile = file.parent();
        int parentCluster = findFileCluster(parentFile);
        byte[] currentClusterData = new byte[clusterSize];
        readerWriter.seekAndRead(currentClusterData, getClusterDataOffset(fileCluster));
        FSFileEntry currentFile = FSFileEntry.fromByteArray(currentClusterData);
        currentFile.remove();
        readerWriter.seekAndWrite(currentFile.toByteArray(), getClusterDataOffset(fileCluster));

        removeFileLinkFromDirectory(parentCluster, fileCluster);
        clearFATChain(fileCluster);
    }

    private void removeFileLinkFromDirectory(int parentCluster, int fileCluster) throws IOException {
        byte[] currentContent = getFileContent(parentCluster);
        byte[] newContent = removeFileLinkFromContent(currentContent, fileCluster);
        write0(newContent, getFileEntryFromCluster(parentCluster));
    }

    private byte[] removeFileLinkFromContent(byte[] currentContent, int fileCluster) {
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
        return newContent;
    }

    @Override
    public List<String> getFilesList(IFile directory) throws IOException {
        int clusterNumber = findFileCluster(directory);
        int nextClusterInChain = readIntFromFsOnOffset(readerWriter, getClusterFATOffset(clusterNumber));
        byte[] currentClusterData = new byte[clusterSize];
        readerWriter.seekAndRead(currentClusterData, getClusterDataOffset(clusterNumber));
        FSFileEntry currentFile = FSFileEntry.fromByteArray(currentClusterData);
        byte[] content = getFileContent(currentFile, nextClusterInChain, currentClusterData);
        if (!currentFile.isDirectory) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        List<Integer> childFilesClusters = getChildClusters(content);
        for (int childClusterNumber : childFilesClusters) {
            currentClusterData = new byte[clusterSize];
            readerWriter.seekAndRead(currentClusterData, getClusterDataOffset(childClusterNumber));
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
        int clusterOffset = getClusterFATOffset(clusterOfCurrentFile);
        int nextClusterInChain = readIntFromFsOnOffset(readerWriter, clusterOffset);
        byte[] currentClusterData = new byte[clusterSize];
        readerWriter.seekAndRead(currentClusterData, getClusterDataOffset(clusterOfCurrentFile));
        FSFileEntry currentFile = FSFileEntry.fromByteArray(currentClusterData);
        checkThatFileIsNotRemoved(currentFile);
        byte[] content = getFileContent(currentFile, nextClusterInChain, currentClusterData);
        List<Integer> childFilesClusters = getChildClusters(content);
        for (int clusterNum : childFilesClusters) {
            currentClusterData = new byte[clusterSize];
            readerWriter.seekAndRead(currentClusterData, getClusterDataOffset(clusterNum));
            FSFileEntry childFile = FSFileEntry.fromByteArray(currentClusterData);
            checkThatFileIsNotRemoved(childFile);
            if (childFile.name.equals(currentName)) {
                return findFileCluster(clusterNum, fileNames, currentNameIdx + 1);
            }
        }
        throw new FileNotFoundException();
    }

    private List<Integer> getChildClusters(byte[] directoryContent) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < directoryContent.length; i += FSConstants.BYTE_DEPTH) {
            byte[] intBytes = new byte[]{directoryContent[i], directoryContent[i + 1], directoryContent[i + 2], directoryContent[i + 3]};
            result.add(FSUtils.intFromFourBytes(intBytes));
        }
        return result;
    }

    private void checkThatFileIsNotRemoved(FSFileEntry file) {
        if (file.isRemoved()) {
            throw new FileNotFoundException();
        }
    }

    private byte[] getFileContent(int clusterNumber) throws IOException {
        int clusterOffset = getClusterFATOffset(clusterNumber);
        int nextClusterInChain = readIntFromFsOnOffset(readerWriter, clusterOffset);
        readerWriter.seek(getClusterDataOffset(clusterNumber));
        byte[] currentClusterData = new byte[clusterSize];
        readerWriter.readBytes(currentClusterData);
        FSFileEntry currentFile = FSFileEntry.fromByteArray(currentClusterData);
        return getFileContent(currentFile, nextClusterInChain, currentClusterData);
    }

    private byte[] getFileContent(FSFileEntry file, int nextCluster, byte[] clusterData) throws IOException {
        byte[] result = new byte[file.size];
        checkThatFileIsNotRemoved(file);
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
                throw new FSFormatException();
            }
            int nextClusterOffset = getClusterDataOffset(nextCluster);
            int fatClusterOffset = getClusterFATOffset(nextCluster);
            nextCluster = readIntFromFsOnOffset(readerWriter, fatClusterOffset);
            readerWriter.seek(nextClusterOffset);
            byte[] currentClusterData = new byte[clusterSize];
            readerWriter.readBytes(currentClusterData);
            int bytesToRead = readBytesCount + clusterSize > file.size ? (file.size - readBytesCount) : clusterSize;
            System.arraycopy(currentClusterData, 0, result, readBytesCount, bytesToRead);
            readBytesCount += bytesToRead;
        }
        return result;
    }

    private int getClusterDataOffset(int clusterNumber) {
        return FSConstants.Offsets.FAT_TABLE + FSConstants.BYTE_DEPTH * clusterCount + clusterNumber * clusterSize;
    }

    @Override
    public void close() throws IOException {
        readerWriter.close();
    }
}
