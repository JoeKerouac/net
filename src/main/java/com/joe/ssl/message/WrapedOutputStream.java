package com.joe.ssl.message;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 输出流包装
 * 
 * @author JoeKerouac
 * @version 2020年06月13日 18:37
 */
public class WrapedOutputStream extends OutputStream {

    private final OutputStream stream;

    public WrapedOutputStream(OutputStream stream) {
        this.stream = stream;
    }

    /**
     * 将int的低8位写出（无符号，网络序）
     * @param b 要写出的数据
     * @throws IOException IO异常
     */
    public void writeInt8(int b) throws IOException {
        write(b);
    }

    /**
     * 将int的低16位写出（无符号，网络序）
     * @param b 要写出的数据
     * @throws IOException IO异常
     */
    public void writeInt16(int b) throws IOException {
        write(b >>> 8);
        write(b);
    }

    /**
     * 将int的低24位写出（无符号，网络序）
     * @param b 要写出的数据
     * @throws IOException IO异常
     */
    public void writeInt24(int b) throws IOException {
        write(b >>> 16);
        write(b >>> 8);
        write(b);
    }

    /**
     * 将int全部写出（无符号，网络序）
     * @param b 要写出的数据
     * @throws IOException IO异常
     */
    public void writeInt32(int b) throws IOException {
        write(b >>> 24);
        write(b >>> 16);
        write(b >>> 8);
        write(b);
    }

    @Override
    public void write(int b) throws IOException {
        stream.write(b);
    }

    @Override
    public void flush() throws IOException {
        stream.flush();
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
