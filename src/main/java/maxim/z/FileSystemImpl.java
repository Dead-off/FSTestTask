package maxim.z;

import maxim.z.exceptions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static maxim.z.FSUtils.intAsFourBytes;
import static maxim.z.FSUtils.parseFileNames;

/**
 * Implementation of virtual file system, that store all data in a single real file.
 * The file has following structure:
 * Bytes 0-63 contains meta information. Bytes 20-23 in this header is INT32 value of total clusters count in fs,
 * bytes 28-31 contains size of one cluster. This constants specified in {@link FSConstants} class.
 * At 64 byte is start of table of used clusters. The table is divided into blocks of 4 bytes (int32).
 * Every block index in table coincides with index of data cluster. Value in table is index of next cluster with data.
 * If current cluster is end of chain, then value of cluster is 0xFFFFFFFF {@link FSConstants}
 * First cluster always contains root directory.
 * Table length is 4*clusterCount bytes.
 * After table file contains all data clusters.
 * Each first cluster contains 32 bytes of meta file information (see {@link FSFileEntry}).
 * First 0-19 bytes is file name. If name length is less 20 symbols, then at end of name appends UTF-8 spaces (0x20 byte)
 * 20 byte is attributes values (one bit per attribute).
 * 24-27 bytes contains index of first cluster of current file.
 * 28-31 bytes is count of data bytes of current file.
 * for files ("is directory attribute - false") data it just file content. For directories each 4 bytes is
 * cluster index of child file.
 */
public class FileSystemImpl extends VirtualFileSystem {

    private final BytesReaderWriter readerWriter;
    private final int clusterCount;
    private final int clusterSize;

    FileSystemImpl(BytesReaderWriter readerWriter) throws IOException {
        this.readerWriter = readerWriter;
        int localClusterCount = readClusterCount(readerWriter);
        int localClusterSize = readClusterSize(readerWriter);
        boolean alreadyInitialized = (localClusterCount != 0 && localClusterSize != 0);
        if (!alreadyInitialized) {
            localClusterCount = FSConstants.DEFAULT_CLUSTER_COUNT;
            localClusterSize = FSConstants.DEFAULT_CLUSTER_SIZE;
        }
        this.clusterCount = localClusterCount;
        this.clusterSize = localClusterSize;
        if (alreadyInitialized) {
            checkThatRootFileAndFATChainExist();
        } else {
            readerWriter.seekAndWrite(intAsFourBytes(localClusterCount), FSConstants.Offsets.CLUSTERS_COUNT);
            readerWriter.seekAndWrite(intAsFourBytes(0), FSConstants.Offsets.LAST_USED_CLUSTER);
            readerWriter.seekAndWrite(intAsFourBytes(localClusterSize), FSConstants.Offsets.CLUSTER_SIZE);
            readerWriter.seekAndWrite(intAsFourBytes(FSConstants.END_OF_CHAIN), FSConstants.Offsets.FAT_TABLE);
            readerWriter.seekAndWrite(FSFileEntry.EMPTY_ROOT.toByteArray(), getClusterDataOffset(0));
        }
    }

    private void checkThatRootFileAndFATChainExist() throws IOException {
        int rootFileClusterNumber = 0;
        int fatValue = readIntFromFsOnOffset(readerWriter, getClusterFATOffset(rootFileClusterNumber));
        if (fatValue == 0) {
            throw new FSFormatException("root cluster doesn't exist");
        }
        try {
            FSFileEntry rootFile = getFileEntryFromCluster(rootFileClusterNumber);
        } catch (Throwable e) {
            throw new FSFormatException("root file have incorrect header", e);
        }
    }

    private static int readClusterSize(BytesReaderWriter readerWriter) throws IOException {
        return readIntFromFsOnOffset(readerWriter, FSConstants.Offsets.CLUSTER_SIZE);
    }

    private static int readClusterCount(BytesReaderWriter readerWriter) throws IOException {
        return readIntFromFsOnOffset(readerWriter, FSConstants.Offsets.CLUSTERS_COUNT);
    }

    private static int readIntFromFsOnOffset(BytesReaderWriter readerWriter, int offset) throws IOException {
        readerWriter.seek(offset);
        byte[] bytes = new byte[FSConstants.BYTE_DEPTH];
        readerWriter.readBytes(bytes);
        return FSUtils.intFromFourBytes(bytes);
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

    /**
     * Overrides data of specified file by specified content.
     *
     * @param file    file for writing data
     * @param content string (encoding specified in {@link FSConstants}), that must written to file
     * @throws IOException           on any default IO error
     * @throws FileNotFoundException if specified file was not found
     * @throws WriteException        if specified file is not available for writing (e.g. file is a directory)
     */
    @Override
    public void write(VirtualFile file, String content) throws IOException {
        write(file, content.getBytes(FSConstants.CHARSET));
    }

    @Override
    void write(VirtualFile file, int offset, byte[] content) throws IOException {
        int fileCluster = findFileCluster(file);
        FSFileEntry currentFile = getFileEntryFromCluster(fileCluster);
        if (currentFile.isDirectory) {
            throw new WriteException(String.format("file %s is a directory", file.getPath()));
        }
        write0(content, offset, currentFile);
    }

    /**
     * Overrides data of specified file by specified content.
     *
     * @param file    file for writing data
     * @param content bytes, that must written to file
     * @throws IOException           on any default IO error
     * @throws FileNotFoundException if specified file was not found
     * @throws WriteException        if specified file is not available for writing (e.g. file is a directory)
     */
    @Override
    public void write(VirtualFile file, byte[] content) throws IOException {
        write(file, 0, content);
    }

    private void write0(byte[] content, int offset, FSFileEntry currentFile) throws IOException {
        int firstClusterForWrite = getClusterNumberByChainNumber(currentFile, (offset + FSConstants.FILE_HEADER_LENGTH) / clusterSize);
        int firstIndexForWrite = (offset + FSConstants.FILE_HEADER_LENGTH) % clusterSize;
        currentFile.size = content.length + offset;
        writeFileHeader(currentFile);
        clearFATChain(firstClusterForWrite);
        int writeBytes = 0;
        List<Integer> usedClusterIndexes = new ArrayList<>();
        int clusterForWrite = firstClusterForWrite;
        do {
            boolean isFirstIteration = (writeBytes == 0);
            int writeBytesCount = Math.min(clusterSize - (isFirstIteration ? FSConstants.FILE_HEADER_LENGTH : 0), content.length - writeBytes);
            int writeOffsetInCurrentCluster = isFirstIteration ? firstIndexForWrite : 0;
            readerWriter.seekAndWrite(Arrays.copyOfRange(content, writeBytes, writeBytes + writeBytesCount), getClusterDataOffset(clusterForWrite) + writeOffsetInCurrentCluster);
            usedClusterIndexes.add(clusterForWrite);
            clusterForWrite = getFirstFreeCluster(clusterForWrite + 1);
            writeBytes += writeBytesCount;
        } while (writeBytes != content.length);
        createFATChain(usedClusterIndexes);
    }

    private void writeFileHeader(FSFileEntry file) throws IOException {
        readerWriter.seekAndWrite(file.toByteArray(), getClusterDataOffset(file.clusterNumber));
    }

    /**
     * Reads file content and return it
     *
     * @param file file for reading data
     * @return data of specified file
     * @throws IOException           on any default IO error
     * @throws FileNotFoundException if specified file was not found
     * @throws ReadException         if specified file is not available for reading (e.g. file is a directory)
     */
    @Override
    public byte[] read(VirtualFile file) throws IOException {
        int fileCluster = findFileCluster(file);
        FSFileEntry fileEntry = getFileEntryFromCluster(fileCluster);
        if (fileEntry.isDirectory) {
            throw new ReadException(String.format("file %s is a directory", file.getPath()));
        }
        return read(file, 0, fileEntry.size);
    }

    @Override
    byte[] read(VirtualFile file, int from, int count) throws IOException {
        FSFileEntry fileEntry = getFileEntryFromCluster(findFileCluster(file));
        return getFileContent(fileEntry, from, count);
    }

    /**
     * Reads file content and return it
     *
     * @param file file for reading data
     * @return data of specified file as string (encoding specified in {@link FSConstants})
     * @throws IOException           on any default IO error
     * @throws FileNotFoundException if specified file was not found
     * @throws ReadException         if specified file is not available for reading (e.g. file is a directory)
     */
    @Override
    public String readAsString(VirtualFile file) throws IOException {
        return new String(read(file), FSConstants.CHARSET);
    }

    /**
     * creates a new file in file system and return it
     *
     * @param parent      directory for creating file
     * @param newFileName name of new file
     * @return File object of created file
     * @throws IOException            on any default IO error
     * @throws IncorrectNameException if name contains forbidden symbols
     * @throws FileNotFoundException  if parent directory was not found
     * @throws CreateFileException    if parent object is not directory
     */
    @Override
    public VirtualFile createFile(VirtualFile parent, String newFileName) throws IOException {
        int parentCluster = findFileCluster(parent);
        checkName(parentCluster, newFileName);
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

    @Override
    public VirtualFile getRootFile() {
        return FileImpl.rootInstance(this);
    }

    /**
     * @param file specified directory
     * @return true, is specified directory exist. Otherwise return false
     * @throws IOException on any default IO error
     */
    @Override
    public boolean isDirectoryExist(VirtualFile file) throws IOException {
        int fileCluster;
        try {
            fileCluster = findFileCluster(file);
        } catch (FileNotFoundException e) {
            return false;
        }
        return getFileEntryFromCluster(fileCluster).isDirectory;
    }

    private void checkName(int parentCluster, String name) throws IOException {
        if (!FSUtils.isCorrectName(name)) {
            throw new IncorrectNameException(String.format("File name can contains only letters, numbers, hyphen and underscore. " +
                    "Max length is %s symbols", String.valueOf(FSConstants.FILE_NAME_LENGTH)));
        }
        List<Integer> childFilesClusters = getChildClusters(getFileContent(parentCluster));
        for (int childCluster : childFilesClusters) {
            if (name.equals(getFileEntryFromCluster(childCluster).name)) {
                throw new IncorrectNameException(String.format("file with name %s already exist", name));
            }
        }
    }

    private void appendClusterLinkToDirectory(int directoryCluster, int fileCluster, FSFileEntry directory) throws IOException {
        byte[] currentBytes = getFileContent(directoryCluster);
        byte[] newContent = Arrays.copyOf(currentBytes, currentBytes.length + 4);
        byte[] pointerToNewFile = intAsFourBytes(fileCluster);
        System.arraycopy(pointerToNewFile, 0, newContent, currentBytes.length, pointerToNewFile.length);
        write0(newContent, 0, directory);
    }

    private void setFATClusterValue(int clusterIndex, int clusterValue) throws IOException {
        readerWriter.seekAndWrite(intAsFourBytes(clusterValue), getClusterFATOffset(clusterIndex));
    }

    private void checkThatFileIsDirectory(FSFileEntry file, String path) {
        if (!file.isDirectory) {
            throw new CreateFileException(String.format("parent file %s is a file", path));
        }
    }

    /**
     * creates a new directory in file system and return it
     *
     * @param parent           directory for creating file
     * @param newDirectoryName name of new directory
     * @return File object of created file
     * @throws IOException            on any default IO error
     * @throws IncorrectNameException if name contains forbidden symbols
     * @throws FileNotFoundException  if parent directory was not found
     * @throws CreateFileException    if parent object is not directory
     */
    @Override
    public VirtualFile createDirectory(VirtualFile parent, String newDirectoryName) throws IOException {
        int parentCluster = findFileCluster(parent);
        checkName(parentCluster, newDirectoryName);
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

    /**
     * removes a specified file
     *
     * @param file file for removing
     * @throws IOException           on any default IO error
     * @throws FileNotFoundException if specified file was not found
     */
    @Override
    public void removeFile(VirtualFile file) throws IOException {
        int fileCluster = findFileCluster(file);
        VirtualFile parentFile = file.parent();
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
        write0(newContent, 0, getFileEntryFromCluster(parentCluster));
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
            throw new FSFormatException("");
        }
        byte[] newContent = new byte[currentContent.length - FSConstants.BYTE_DEPTH];
        System.arraycopy(currentContent, 0, newContent, 0, idxLink);
        System.arraycopy(currentContent, idxLink + FSConstants.BYTE_DEPTH, newContent, idxLink, newContent.length - idxLink);
        return newContent;
    }

    /**
     * return a list of files and directories in specify directory
     *
     * @param directory directory for get files list
     * @return list of files and directories in specify directory
     * @throws FileNotFoundException if directory was not found
     * @throws IOException           on any default IO error
     */
    @Override
    public List<String> getFilesList(VirtualFile directory) throws IOException {
        int clusterNumber = findFileCluster(directory);
        int nextClusterInChain = readIntFromFsOnOffset(readerWriter, getClusterFATOffset(clusterNumber));
        byte[] currentClusterData = new byte[clusterSize];
        readerWriter.seekAndRead(currentClusterData, getClusterDataOffset(clusterNumber));
        FSFileEntry currentFile = FSFileEntry.fromByteArray(currentClusterData);
        byte[] content = getFileContent(currentFile, 0, currentFile.size);
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

    private int findFileCluster(VirtualFile file) throws IOException {
        String[] dirNames = parseFileNames(file);
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
        byte[] content = getFileContent(currentFile, 0, currentFile.size);
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
        throw new FileNotFoundException(String.format("file %s was not found", currentName));
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
            throw new FileNotFoundException(String.format("file %s was removed", file.name));
        }
    }

    private byte[] getFileContent(int clusterNumber) throws IOException {
        FSFileEntry currentFile = getFileEntryFromCluster(clusterNumber);
        return getFileContent(currentFile, 0, currentFile.size);
    }

    private int getClusterNumberByChainNumber(FSFileEntry file, int chainNumber) throws IOException {
        int result = file.clusterNumber;
        while (chainNumber > 0) {
            result = readIntFromFsOnOffset(readerWriter, getClusterFATOffset(result));
            chainNumber--;

        }
        return result;
    }

    private byte[] getFileContent(FSFileEntry file, int offset, int count) throws IOException {
        int resultBytesCount = Math.min(count - offset, file.size - offset);
        if (resultBytesCount < 0) {
            return new byte[0];
        }
        int firstClusterForRead = getClusterNumberByChainNumber(file, (offset + FSConstants.FILE_HEADER_LENGTH) / clusterSize);
        int firstIndexForRead = (offset + FSConstants.FILE_HEADER_LENGTH) % clusterSize;
        byte[] result = new byte[resultBytesCount];
        checkThatFileIsNotRemoved(file);
        int readBytesCount = 0;
        int clusterIdx = firstClusterForRead;
        do {
            if (clusterIdx == FSConstants.END_OF_CHAIN) {
                throw new FSFormatException("");
            }
            int nextClusterOffset = getClusterDataOffset(clusterIdx);
            int fatClusterOffset = getClusterFATOffset(clusterIdx);
            clusterIdx = readIntFromFsOnOffset(readerWriter, fatClusterOffset);
            boolean isFirstIteration = (readBytesCount == 0);
            int availableBytesInCluster = clusterSize - (isFirstIteration ? firstIndexForRead : 0);
            int bytesToRead = Math.min(availableBytesInCluster, result.length-readBytesCount);
//            if (isFirstIteration) {todo remove commented code
//                if (clusterIdx == FSConstants.END_OF_CHAIN) {
//                    bytesToRead = (file.size - (offset%clusterSize));
//                } else {
//                    bytesToRead = clusterSize - firstIndexForRead;
//                }
//            } else {
//                if (clusterIdx == FSConstants.END_OF_CHAIN) {
//                    bytesToRead = file.size;
//                } else {
//                    bytesToRead = clusterSize;
//                }
//            }
            byte[] currentClusterData = new byte[bytesToRead];
            readerWriter.seekAndRead(currentClusterData,
                    nextClusterOffset + (isFirstIteration ? firstIndexForRead : 0));
            System.arraycopy(currentClusterData, 0, result, readBytesCount, bytesToRead);
            readBytesCount += bytesToRead;
        } while (readBytesCount != result.length);
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
