package com.joe.ssl.message;

import lombok.Data;

import java.io.IOException;

/**
 * @author JoeKerouac
 * @version 2020年06月13日 16:24
 */
@Data
public class ClientHello implements HandshakeMessage {

    private byte[] clientRandom;

    private byte[] sessionId;


    @Override
    public HandshakeType type() {
        return null;
    }

    @Override
    public void write(WrapedOutputStream stream) throws IOException {

    }
}
