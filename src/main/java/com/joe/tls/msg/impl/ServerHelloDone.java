package com.joe.tls.msg.impl;

import java.nio.ByteBuffer;

import com.joe.tls.enums.HandshakeType;
import com.joe.tls.msg.HandshakeProtocol;
import com.joe.tls.util.ByteBufferUtil;

/**
 * 服务端握手完成消息
 * 
 * @author JoeKerouac
 * @data 2020-11-10 16:54
 */
public class ServerHelloDone implements HandshakeProtocol {

    @Override
    public HandshakeType type() {
        return HandshakeType.SERVER_HELLO_DONE;
    }

    @Override
    public int len() {
        return 0;
    }

    @Override
    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[4 + len()]);
        ByteBufferUtil.writeInt8(type().getCode(), buffer);
        ByteBufferUtil.writeInt24(len(), buffer);
        return buffer.array();
    }
}
