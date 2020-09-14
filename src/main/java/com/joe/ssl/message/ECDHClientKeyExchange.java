package com.joe.ssl.message;

import org.bouncycastle.crypto.params.ECPublicKeyParameters;

import java.io.IOException;

public class ECDHClientKeyExchange implements HandshakeMessage {

    private byte[] publicKey;

    public ECDHClientKeyExchange(ECPublicKeyParameters ecPublicKeyParameters) {
        this.publicKey = ecPublicKeyParameters.getQ().getEncoded();
    }

    @Override
    public void init(int bodyLen, WrapedInputStream inputStream) throws IOException {

    }

    @Override
    public HandshakeType type() {
        return HandshakeType.CLIENT_KEY_EXCHANGE;
    }

    @Override
    public int size() {
        return 1 + 3 + 1 + publicKey.length;
    }

    @Override
    public void write(WrapedOutputStream stream) throws IOException {
        stream.writeInt8(type().getCode());
        stream.writeInt24(publicKey.length + 1);
        stream.writeInt8(publicKey.length);
        stream.write(publicKey);
    }
}
