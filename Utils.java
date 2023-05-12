import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;

public class Utils {
    private static HashMap<Character, String> mp;
    private static String[] hexList = { "0000", "0001", "0010", "0011", "0100", "0101", "0110", "0111", "1000", "1001",
            "1010", "1011", "1100", "1101", "1110", "1111", };
    private static char[] charList = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    static {
        mp = new HashMap<Character, String>();
        for (int i = 0; i < charList.length; i++) {
            mp.put(charList[i], hexList[i]);
        }
    }

    // byte转二进制字符串
    public static String byteToBinaryString(byte b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 7; i >= 0; --i) {
            sb.append(b >>> i & 1);
        }
        return sb.toString();
    }

    /// 十六进制转二进制
    public static String hex2bin(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            sb.append(mp.get(s.charAt(i)));
        }
        return sb.toString();

    }

    /// 二进制转十六进制
    public static String bin2hex(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i += 4) {
            String ss = s.substring(i, i + 4);
            sb.append(charList[Integer.parseInt(ss, 2)]);
        }
        return sb.toString();
    }

    /// 移动操作
    public static String shift_left(String k, int l) {
        l = l % k.length();
        String s1 = k.substring(0, l);
        String s2 = k.substring(l, k.length());
        return s2 + s1;
    }


    /// 字符串异或
    public static String string_xor(String a, String b) {
        if (a.length() != b.length()) {
            throw new Error("Error: Two strings is not the same size in string_xor！");
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) == b.charAt(i)) {
                sb.append('0');
            } else {
                sb.append('1');
            }
        }
        return sb.toString();
    }

    /**
     * 小文件读取，一次buffer缓冲，将全部文件内容读出，若不能一次读出则throw IOException，不执行数据读取操作。
     *
     * @param fileFullName 文件读取全路径名称
     * @return
     */
    public static byte[] readOnce(String fileFullName) throws IOException {
        // open the file
        File file = new File(fileFullName);
        return readOnce(file);
    }
 
    /**
     * 小文件读取，一次buffer缓冲，将全部文件内容读出，若不能一次读出则throw IOException，不执行数据读取操作。
     *
     * @param file
     * @return
     */
    public static byte[] readOnce(File file) throws IOException {
        //check the file is Exists
        checkFileExists(file);
        // check the file is too long, if the file length is too long ,returned. because the byte array can not buffered.
        // byte array bufferSize=file.lenght,and  must between 0 and Integer_MAX_VALUE
        if (file.length() > Integer.MAX_VALUE) {
            System.err.println("file is too big ,not to read !");
            throw new IOException(file.getName() + " is too big ,not to read ");
        }
        int _bufferSize = (int) file.length();
        //定义buffer缓冲区大小
        byte[] buffer = new byte[_bufferSize];
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            int len = 0;
            if ((len = in.available()) <= buffer.length) {
                in.read(buffer, 0, len);
            }
        } finally {
            closeInputStream(in);
        }
        return buffer;
    }
 
    public static byte[] readByByteArrayOutputStream(File file) throws IOException {
        checkFileExists(file);
        // 传统IO方式
        //1、定义一个Byte字节数组输出流，设置大小为文件大小
        //2、将打开的文件输入流转换为Buffer输入流，循环 读取buffer输入流到buffer[]缓冲，并将buffer缓冲数据输入到目标输出流。
        //3、将目标输出流转换为字节数组。
        ByteArrayOutputStream bos = new ByteArrayOutputStream((int) file.length());
        BufferedInputStream bin = null;
        try {
            bin = new BufferedInputStream(new FileInputStream(file));
            byte[] buffer = new byte[1024];
            while (bin.read(buffer) > 0) {
                bos.write(buffer);
            }
            return bos.toByteArray();
        } finally {
            closeInputStream(bin);
            closeOutputStream(bos);
        }
    }
 
    public static byte[] readByNIO(File file) throws IOException {
        checkFileExists(file);
        //1、定义一个File管道，打开文件输入流，并获取该输入流管道。
        //2、定义一个ByteBuffer，并分配指定大小的内存空间
        //3、while循环读取管道数据到byteBuffer，直到管道数据全部读取
        //4、将byteBuffer转换为字节数组返回
        FileChannel fileChannel = null;
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            fileChannel = in.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate((int) fileChannel.size());
 
            while (fileChannel.read(buffer) > 0) {
            }
            return buffer.array();
        } finally {
            closeChannel(fileChannel);
            closeInputStream(in);
        }
    }
 
    public static byte[] readRandomAccess(File file) throws IOException {
 
        //1、使用RandomAccessFile打开文件管道
        //2、创建MappedByteBuffer，并使用NIO管道进行数据映射,加载数据到物理内存
        //3、读取数据到byte数组中。
        FileChannel channel = new RandomAccessFile(file, "r").getChannel();
        int fileSize = (int) channel.size();
        try {
            //load(): 将此缓冲区的内容加载到物理内存中。此方法尽最大努力确保当它返回时，缓冲区的内容驻留在物理内存中。调用此方法可能会导致出现一些页面错误和I/O操作。
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize).load();
            //remaining() 返回当前位置和限制之间的元素数。此缓冲区中剩余的元素数
            byte[] result = new byte[fileSize];
            if (buffer.remaining() > 0) {
                buffer.get(result, 0, fileSize);
            }
            buffer.clear();
            return result;
        } finally {
            closeChannel(channel);
        }
    }
 
    private static void checkFileExists(File file) throws FileNotFoundException {
        if (file == null || !file.exists()) {
            System.err.println("file is not null or exist !");
            throw new FileNotFoundException(file.getName());
        }
    }
 
    private static void closeChannel(FileChannel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
 
    private static void closeOutputStream(OutputStream bos) {
        try {
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
 
    private static void closeInputStream(InputStream in) {
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte parse_binString(String bin) {
        String s = bin.substring(1, 8);
        byte b = Byte.parseByte(s, 2);
        char c = bin.charAt(0);
        //return bin.charAt(0) == '0' ? b : (byte)(~b + 1);
        if (bin.charAt(0) == '1') {
            b = (byte)(~b + 1);
        }
        return b;
    }
}
