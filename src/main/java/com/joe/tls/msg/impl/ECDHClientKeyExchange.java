package com.joe.tls.msg.impl;

import java.nio.ByteBuffer;

import com.joe.tls.enums.HandshakeType;
import com.joe.tls.msg.HandshakeProtocol;
import com.joe.tls.util.ByteBufferUtil;

import lombok.Getter;

/**
 * 客户端ECDH密钥交换消息
 * 
 * @author JoeKerouac
 * @data 2020-11-03 12:41
 */
public class ECDHClientKeyExchange implements HandshakeProtocol {

    /**
     * 客户端密钥交换公钥数据
     */
    @Getter
    private final byte[] publicKey;

    public ECDHClientKeyExchange(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public ECDHClientKeyExchange(ByteBuffer buffer) {
        // 跳过4byte的header
        ByteBufferUtil.mergeReadInt32(buffer);
        this.publicKey = ByteBufferUtil.getInt8(buffer);
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
        ByteBuffer buffer = ByteBuffer.wrap(new byte[4 + len()]);
        ByteBufferUtil.writeInt8(type().getCode(), buffer);
        ByteBufferUtil.writeInt24(publicKey.length + 1, buffer);
        ByteBufferUtil.putBytes8(publicKey, buffer);
        return buffer.array();
    }
}
