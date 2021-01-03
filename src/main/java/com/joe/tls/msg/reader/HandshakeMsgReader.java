package com.joe.tls.msg.reader;

import com.joe.tls.msg.HandshakeProtocol;

/**
 * 握手消息读取器
 * 
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-08 17:04
 */
public interface HandshakeMsgReader {

    /**
     * 读取握手消息
     * 
     * @param data
     *            握手消息序列化数据
     * @return 握手消息
     */
    HandshakeProtocol read(byte[] data);

}
