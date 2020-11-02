package com.joe.tls.msg;

import com.joe.ssl.message.TlsVersion;
import com.joe.tls.enums.ContentType;

/**
 * record消息
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-02 10:42
 */
public interface Record {

    /**
     * 消息contentType
     * @return 消息的contentType
     */
    ContentType type();

    /**
     * 消息版本号
     * @return 版本号
     */
    TlsVersion version();

    /**
     * 协议消息
     * @param <T> 协议消息类型
     * @return 协议消息
     */
    <T extends RecordContent> T msg();

}
