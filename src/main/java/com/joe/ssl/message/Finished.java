package com.joe.ssl.message;

import java.io.IOException;

import com.joe.ssl.crypto.DigestSpi;
import com.joe.ssl.crypto.PhashSpi;
import com.joe.tls.enums.HandshakeType;
import com.joe.utils.collection.CollectionUtil;

public class Finished implements HandshakeMessage {

    private DigestSpi digestSpi;

    private byte[]    data = new byte[12];

    public Finished(DigestSpi digestSpi, String macAlg, byte[] masterKey, boolean isClient) {
        init(digestSpi, macAlg, masterKey, isClient);
    }

    private void init(DigestSpi digestSpi, String macAlg, byte[] masterKey, boolean isClient) {
        this.digestSpi = digestSpi;
        String tlsLabel = isClient ? "client finished" : "server finished";

        try {
            byte[] seed = digestSpi.copy().digest();
            PhashSpi phashSpi = PhashSpi.getInstance(macAlg);
            phashSpi.init(masterKey);
            phashSpi.phash(CollectionUtil.merge(tlsLabel.getBytes(), seed), this.data);

        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("PRF failed", e);
        }
    }

    @Override
    public void init(int bodyLen, WrapedInputStream inputStream) throws IOException {
        throw new RuntimeException("未实现");
    }

    @Override
    public HandshakeType type() {
        return HandshakeType.CLIENT_ENCRYPT;
    }

    @Override
    public int size() {
        return 1 + 3 + data.length;
    }

    @Override
    public void write(WrapedOutputStream stream) throws IOException {
        stream.writeInt8(type().getCode());
        stream.writeInt24(data.length);
        stream.write(data);
    }
}
