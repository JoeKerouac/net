package com.joe.tls.msg.impl;

import com.joe.tls.enums.HandshakeType;
import com.joe.tls.msg.HandshakeProtocol;

/**
 * 服务端ECDH密钥交换消息
 *
 * @author JoeKerouac
 * @data 2020-11-02 21:06
 */
public class ECDHServerKeyExchange implements HandshakeProtocol {

    /**
     * 消息总长度，包含header
     */
    private int msgLen;

    private byte curveType;

    private int curveId;


    @Override
    public HandshakeType type() {
        return HandshakeType.SERVER_KEY_EXCHANGE;
    }

    @Override
    public int len() {
        return 0;
    }

    @Override
    public byte[] serialize() {
        return new byte[0];
    }
}
