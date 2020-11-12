package com.joe.tls.msg.impl;

import java.nio.ByteBuffer;

import com.joe.tls.crypto.PhashSpi;
import com.joe.tls.enums.HandshakeType;
import com.joe.tls.msg.HandshakeProtocol;
import com.joe.tls.util.ByteBufferUtil;
import com.joe.utils.collection.CollectionUtil;

import lombok.Getter;

/**
 * 结束消息
 *
 * @author JoeKerouac
 * @data 2020-11-05 20:57
 */
public class Finished implements HandshakeProtocol {

    /**
     * 要发送的明文数据
     */
    @Getter
    private final byte[] data;

    public Finished(ByteBuffer buffer) {
        // 跳过类型
        ByteBufferUtil.mergeReadInt8(buffer);
        this.data = ByteBufferUtil.getInt24(buffer);
    }

    public Finished(PhashSpi phashSpi, byte[] masterKey, byte[] sessionHash, boolean isClient) {
        this.data = new byte[12];
        String tlsLabel = isClient ? "client finished" : "server finished";

        phashSpi.init(masterKey);
        phashSpi.phash(CollectionUtil.merge(tlsLabel.getBytes(), sessionHash), this.data);
    }

    @Override
    public HandshakeType type() {
        return HandshakeType.FINISHED;
    }

    @Override
    public int len() {
        return data.length + 3;
    }

    @Override
    public byte[] serialize() {
        byte[] result = new byte[4 + 12];
        result[0] = type().getCode();
        result[1] = 0;
        result[2] = 0;
        result[3] = 12;
        System.arraycopy(data, 0, result, 4, data.length);
        return result;
    }
}
