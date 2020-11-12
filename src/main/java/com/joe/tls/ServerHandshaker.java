package com.joe.tls;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-12 11:56
 */
public class ServerHandshaker extends Handshaker {

    public ServerHandshaker(InputStream netInputStream, OutputStream netOutputStream, SecureRandom secureRandom) {
        super(netInputStream, netOutputStream, secureRandom);
    }

    @Override
    protected void changeCipher() {

    }
}
