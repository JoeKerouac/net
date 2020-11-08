package com.joe.tls.msg;

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
     * @param data 握手消息序列化数据
     * @param <T> 握手消息实际类型
     * @return 握手消息
     */
    <T extends HandshakeProtocol> T read(byte[] data);

}
