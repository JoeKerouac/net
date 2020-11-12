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

    public ServerHandshaker(InputStream netInputStream, OutputStream netOutputStream,
                            SecureRandom secureRandom) {
        super(netInputStream, netOutputStream, secureRandom);
    }

    @Override
    protected void changeCipher() {
        // 客户端写出加密盒
        CipherBox clientWrite = new CipherBox(secureRandom, cipherSuite.getCipher(),
                secretCollection.getClientWriteKey(), secretCollection.getClientWriteIv(), true);
        // 服务端读取加密盒
        CipherBox serverRead = new CipherBox(secureRandom, cipherSuite.getCipher(),
                secretCollection.getServerWriteKey(), secretCollection.getServerWriteIv(), false);

        if (cipherSuite.getCipher().isGcm()) {
            Authenticator readAuthenticator = new Authenticator(tlsVersion);
            Authenticator writeAuthenticator = new Authenticator(tlsVersion);
            inputRecordStream.changeCipher(serverRead, readAuthenticator);
            outputRecordStream.changeCipher(clientWrite, writeAuthenticator);
        } else {
            throw new RuntimeException("暂不支持的算法：" + cipherSuite.getCipher());
        }
    }
}
