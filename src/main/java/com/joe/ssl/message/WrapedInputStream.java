package com.joe.ssl.message;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * 输入流包装
 * 
 * @author JoeKerouac
 * @version 2020年06月17日 22:09
 */
public class WrapedInputStream extends InputStream {

    private InputStream stream;

    public WrapedInputStream(InputStream stream) {
        this.stream = stream;
    }

    /**
     * 读取8位数据
     * @return 8位数据
     * @throws IOException IO异常
     */
    public int readInt8() throws IOException {
        return stream.read();
    }

    /**
     * 读取16位数据
     * @return 16位数据
     * @throws IOException IO异常
     */
    public int readInt16() throws IOException {
        return stream.read() << 8 | stream.read();
    }

    /**
     * 读取24位数据
     * @return 24位数据
     * @throws IOException IO异常
     */
    public int readInt24() throws IOException {
        return stream.read() << 16 | stream.read() << 8 | stream.read();
    }

    /**
     * 读取32位数据
     * @return 32位数据
     * @throws IOException IO异常
     */
    public int readInt32() throws IOException {
        return stream.read() << 24 | stream.read() << 16 | stream.read() << 8 | stream.read();
    }

    /**
     * 从输入流读取指定长度的数据，如果当前可读数据小于该长度将会返回null
     * @param len 要读取的长度
     * @return 指定长度的数据
     * @throws IOException IO异常
     */
    public byte[] read(int len) throws IOException {
        if (available() < len) {
            return null;
        }

        byte[] data = new byte[len];
        int readLen = stream.read(data);
        if (len != readLen) {
            throw new EOFException(
                String.format("输入流[%s]要求读入长度：[%d]，实际读取长度：[%d]", stream.toString(), len, readLen));
        }
        return data;
    }

    @Override
    public int read() throws IOException {
        return stream.read();
    }

    @Override
    public long skip(long n) throws IOException {
        return stream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return stream.available();
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        stream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        stream.reset();
    }

    @Override
    public boolean markSupported() {
        return stream.markSupported();
    }

}
