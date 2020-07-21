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

    private static final int   OVERFLOW_OF_INT08 = (1 << 8);
    private static final int   OVERFLOW_OF_INT16 = (1 << 16);
    private static final int   OVERFLOW_OF_INT24 = (1 << 24);

    private final OutputStream stream;

    public WrapedOutputStream(OutputStream stream) {
        this.stream = stream;
    }

    /**
     * 将数据写入，首先是8个字节的数据长度，然后是实际数据
     * @param data 要写入的数据，长度不能超过1 << 8
     * @throws IOException IO异常
     */
    public void putBytes8(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            writeInt8(0);
            return;
        } else {
            checkOverflow(data.length, OVERFLOW_OF_INT08);
        }
        writeInt8(data.length);
        write(data, 0, data.length);
    }

    /**
     * 将数据写入，首先是16个字节的数据长度，然后是实际数据
     * @param data 要写入的数据，长度不能超过1 << 16
     * @throws IOException IO异常
     */
    public void putBytes16(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            writeInt16(0);
            return;
        } else {
            checkOverflow(data.length, OVERFLOW_OF_INT16);
        }
        writeInt16(data.length);
        write(data, 0, data.length);
    }

    /**
     * 将数据写入，首先是24个字节的数据长度，然后是实际数据
     * @param data 要写入的数据，长度不能超过1 << 24
     * @throws IOException IO异常
     */
    public void putBytes24(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            writeInt24(0);
            return;
        } else {
            checkOverflow(data.length, OVERFLOW_OF_INT24);
        }
        writeInt24(data.length);
        write(data, 0, data.length);
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

    private void checkOverflow(int length, int overflow) {
        if (length >= overflow) {
            // internal_error alert will be triggered
            throw new RuntimeException("Field length overflow, the field length (" + length
                                       + ") should be less than " + overflow);
        }
    }

}
