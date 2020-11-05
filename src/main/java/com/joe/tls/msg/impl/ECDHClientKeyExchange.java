package com.joe.tls.msg.impl;

import java.nio.ByteBuffer;

import com.joe.tls.enums.HandshakeType;
import com.joe.tls.msg.HandshakeProtocol;
import com.joe.tls.util.ByteBufferUtil;

/**
 * @author JoeKerouac
 * @data 2020-11-03 12:41
 */
public class ECDHClientKeyExchange implements HandshakeProtocol {

    private byte[] publicKey;

    public ECDHClientKeyExchange(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public HandshakeType type() {
        return HandshakeType.CLIENT_KEY_EXCHANGE;
    }

    @Override
    public int len() {
        return 1 + publicKey.length;
    }

    @Override
    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(5 + publicKey.length);
        ByteBufferUtil.writeInt8(type().getCode(), buffer);
        ByteBufferUtil.writeInt24(publicKey.length + 1, buffer);
        ByteBufferUtil.putBytes8(publicKey, buffer);
        return buffer.array();
    }
}
