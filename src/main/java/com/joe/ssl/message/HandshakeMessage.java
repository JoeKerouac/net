package com.joe.ssl.message;

import com.joe.tls.enums.HandshakeType;

import java.io.IOException;

/**
 * @author JoeKerouac
 * @version 2020年06月13日 17:56
 */
public interface HandshakeMessage extends RecordMessage {

    /**
     * 从输入流中读取握手信息，输入流的起始位置应该是Handshake的body开始（1byte的类型、3byte的长度信息已经去除过了）
     *
     * @param bodyLen body的长度
     * @param inputStream body的输入流
     * @throws IOException IO异常
     */
    void init(int bodyLen, WrapedInputStream inputStream) throws IOException;

    /**
     * 握手消息类型
     * @return 类型
     */
    HandshakeType type();

    /**
     * 消息大小，单位byte
     * @return 消息大小
     */
    int size();
}
