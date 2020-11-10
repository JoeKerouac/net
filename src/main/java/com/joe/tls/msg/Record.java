package com.joe.tls.msg;

import com.joe.tls.TlsVersion;
import com.joe.tls.enums.ContentType;

/**
 * record消息，record消息格式：
 * <br/>
 * <br/>
 * | 1byte content type | 2byte version | 2byte content len | n byte nonce | content | mac |
 * <br/>
 * <br/>
 * 其中nonce的长度是加密套件决定的，同时对于AEAD模式来说nonce不能加密，要明文发送，最后的mac对于AEAD模式来说是没有的
 * 
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-02 10:42
 */
public class Record {

    private final ContentType   type;
    private final TlsVersion    version;
    private final RecordContent content;
    private final byte[]        contentData;

    public Record(ContentType type, TlsVersion version, RecordContent content) {
        this(type, version, content, null);
    }

    public Record(ContentType type, TlsVersion version, RecordContent content, byte[] contentData) {
        this.type = type;
        this.version = version;
        this.content = content;
        this.contentData = contentData == null ? content.serialize() : contentData;
    }

    /**
     * 消息contentType
     * @return 消息的contentType
     */
    public ContentType type() {
        return type;
    }

    /**
     * 消息版本号
     * @return 版本号
     */
    public TlsVersion version() {
        return version;
    }

    /**
     * 协议消息的长度
     * @return 协议消息的长度，2byte，不包含record层5byte的header（1byte type + 2byte version + 2byte长度）
     */
    public int len() {
        return contentData.length;
    }

    /**
     * 协议消息
     * @return 协议消息
     */
    public RecordContent msg() {
        return content;
    }

}
