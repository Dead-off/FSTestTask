package maxim.z;

import org.junit.Test;

import java.io.*;
import java.io.File;
import java.util.Arrays;

public class RemoveIt {


    @Test
    public void test() throws IOException{
        RandomAccessFile raf = new RandomAccessFile(new File("D:\\1"), "rws");
//        raf.seek(1);

//        raf.write("abcd".getBytes());
//        raf.write("abd".getBytes());
//        raf.skipBytes(1);
//        raf.seek(10);
//        raf.seek(1);
//        System.out.println(0xFFFFFFFF);
//        byte[] bytes = ByteBuffer.allocate(4).putInt(257).array();
//        raf.write(bytes);
        raf.seek(2);
        byte[] b = new byte[3];
        raf.read(b, 0, b.length);
        System.out.println(Arrays.toString(b));
        raf.close();

    }
}
