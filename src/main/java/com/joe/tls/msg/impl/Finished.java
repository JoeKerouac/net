package com.joe.tls.msg.impl;

import com.joe.ssl.crypto.DigestSpi;
import com.joe.tls.enums.HandshakeType;
import com.joe.tls.msg.HandshakeProtocol;

/**
 * 结束消息
 *
 * @author JoeKerouac
 * @data 2020-11-05 20:57
 */
public class Finished implements HandshakeProtocol {

    public Finished(DigestSpi digest) {

    }

    @Override
    public HandshakeType type() {
        return HandshakeType.CLIENT_ENCRYPT;
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
