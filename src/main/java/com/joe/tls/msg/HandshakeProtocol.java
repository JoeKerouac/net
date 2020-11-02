package com.joe.tls.msg;

import com.joe.tls.enums.HandshakeType;

/**
 * handshake协议
 * 
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-02 10:53
 */
public interface HandshakeProtocol extends RecordContent {

    /**
     * 握手类型
     * @return 握手类型
     */
    HandshakeType type();

    /**
     * 消息长度，3byte，不包含header（type+本身len字段，总共4byte）
     * @return 消息长度
     */
    int len();

    /**
     * 消息序列化为网络传输数据，这里是完整数据，包含header
     * @return 序列化后的网络传输数据
     */
    byte[] serialize();
}
