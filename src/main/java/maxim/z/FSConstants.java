package maxim.z;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class FSConstants {

    final static int DEFAULT_CLUSTER_COUNT = 1024;
    final static int DEFAULT_CLUSTER_SIZE = 4096;
    final static int END_OF_CHAIN = 0xFFFFFFFF;
    final static int BYTE_DEPTH = 4;
    final static int FILE_HEADER_LENGTH = 32;
    final static int FILE_NAME_LENGTH = 20;
    final static Charset CHARSET = StandardCharsets.UTF_8;

    static class Offsets {
        final static int CLUSTERS_COUNT = 20;
        final static int LAST_USED_CLUSTER = 24;
        final static int CLUSTER_SIZE = 28;
        final static int FAT_TABLE = 64;
    }

    static class FileHeaderOffsets {
        final static int FILE_SIZE = 28;
        final static int FILE_CLUSTER = 24;
    }


}
