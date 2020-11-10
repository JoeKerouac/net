package com.joe.tls.msg.impl;

import com.joe.tls.enums.HandshakeType;
import com.joe.tls.msg.HandshakeProtocol;

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
        return new byte[0];
    }
}
