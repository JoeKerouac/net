package com.joe.ssl.message;

import com.joe.ssl.openjdk.ssl.ProtocolVersion;
import lombok.Data;

import java.io.IOException;

/**
 * @author JoeKerouac
 * @version 2020年06月13日 16:24
 */
@Data
public class ClientHello implements HandshakeMessage {

    /**
     * 客户端随机数，32位
     */
    private byte[] clientRandom;

    /**
     * sessionId，最大256
     */
    private byte[] sessionId = new byte[0];

    private ProtocolVersion protocolVersion;

    public void init() {

    }


    @Override
    public HandshakeType type() {
        return HandshakeType.CLIENT_HELLO;
    }

    @Override
    public void write(WrapedOutputStream stream) throws IOException {
        stream.writeInt8(protocolVersion.major);
        stream.writeInt8(protocolVersion.minor);
        stream.write(clientRandom);
        stream.putBytes8(sessionId);
        // TODO 写出加密套件
        // TODO 写出compression_methods
        // TODO 写出extensions

    }
}
