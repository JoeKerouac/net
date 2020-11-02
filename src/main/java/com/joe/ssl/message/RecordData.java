package com.joe.ssl.message;

import com.joe.tls.enums.ContentType;

import java.io.IOException;

/**
 * record数据
 * 
 * @author JoeKerouac
 * @version 2020年06月17日 20:43
 */
public class RecordData {

    /**
     * record类型
     */
    private final ContentType contentType;

    /**
     * tls版本
     */
    private final TlsVersion  tlsVersion;

    /**
     * 数据长度，要等于{@link #data}的长度
     */
    private final int         len;

    /**
     * 实际负载数据，要不为null，只要不为null就表示读取完毕
     */
    private byte[]            data;

    private RecordData(ContentType contentType, TlsVersion tlsVersion, int len, byte[] data) {
        this.contentType = contentType;
        this.tlsVersion = tlsVersion;
        this.len = len;
        this.data = data;
    }

    /**
     * 读取负载数据，除非负载数据完整才会读取
     * @param stream 输入流
     * @return RecordData
     * @throws IOException IO异常
     */
    public RecordData readData(WrapedInputStream stream) throws IOException {
        this.data = stream.read(this.len);
        return this;
    }

    /**
     * 从流中读取RecordData
     * 
     * @param stream 输入流
     * @return RecordData
     * @throws IOException IO异常
     */
    public static RecordData build(WrapedInputStream stream) throws IOException {
        if (stream.available() < 5) {
            return null;
        }

        ContentType type = EnumInterface.getByCode(stream.readInt8(), ContentType.class);
        TlsVersion version = EnumInterface.getByCode(stream.readInt16(), TlsVersion.class);
        int len = stream.readInt16();

        RecordData recordData = new RecordData(type, version, len, null);
        // 这里先尝试读取，看是否能读取到数据
        recordData.readData(stream);
        return recordData;
    }

    /**
     * Getter method for property <tt>contentType</tt>
     *
     * @return property value of contentType
     */
    public ContentType getContentType() {
        return contentType;
    }

    /**
     * Getter method for property <tt>tlsVersion</tt>
     *
     * @return property value of tlsVersion
     */
    public TlsVersion getTlsVersion() {
        return tlsVersion;
    }

    /**
     * Getter method for property <tt>len</tt>
     *
     * @return property value of len
     */
    public int getLen() {
        return len;
    }

    /**
     * Getter method for property <tt>data</tt>
     *
     * @return property value of data
     */
    public byte[] getData() {
        return data.clone();
    }
}
