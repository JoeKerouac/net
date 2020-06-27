package com.joe.ssl.message;

import java.io.IOException;

/**
 * @author JoeKerouac
 * @version 2020年06月13日 17:56
 */
public interface HandshakeMessage {

    /**
     * 握手消息类型
     * @return
     */
    HandshakeType type();

    /**
     * 将消息写入输出流
     *
     * @param stream 输出流
     * @throws IOException IO异常
     */
    void write(WrapedOutputStream stream) throws IOException;
}
