package com.joe.tls.msg.extensions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.joe.ssl.message.WrapedOutputStream;
import com.joe.tls.util.ByteBufferUtil;

/**
 * 目前不支持的扩展
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-12 10:26
 */
public class UnknownExtension implements HelloExtension {

    /**
     * 扩展类型
     */
    private int    type;

    /**
     * 扩展数据
     */
    private byte[] data;

    public UnknownExtension(int type, byte[] data) {
        this.type = type;
        this.data = data == null ? new byte[0] : data;
    }

    @Override
    public void write(WrapedOutputStream outputStream) throws IOException {
        outputStream.writeInt16(type);
        outputStream.writeInt16(data.length);
        if (data.length > 0) {
            outputStream.write(data);
        }
    }

    @Override
    public void write(ByteBuffer buffer) throws IOException {
        ByteBufferUtil.writeInt16(getExtensionType().id, buffer);
        ByteBufferUtil.putBytes16(data, buffer);
    }

    @Override
    public int size() {
        return data.length + 4;
    }

    @Override
    public ExtensionType getExtensionType() {
        return ExtensionType.get(type);
    }

    @Override
    public String toString() {
        return String.format("UnknownExtension : \t[type : %d]\t[data : %s]", type,
            Arrays.toString(data));
    }
}
