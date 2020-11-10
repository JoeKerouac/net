package com.joe.tls.crypto.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.joe.tls.crypto.DigestSpi;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-12 14:39
 */
public class AbstractDigest implements DigestSpi {

    private MessageDigest digest;

    private String        algorithm;

    public AbstractDigest(String algorithm) {
        init(algorithm);
    }

    private void init(String algorithm) {
        try {
            this.algorithm = algorithm;
            this.digest = MessageDigest.getInstance(algorithm, new BouncyCastleProvider());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(byte[] data, int offset, int len) {
        digest.update(data, offset, len);
    }

    @Override
    public void update(byte data) {
        digest.update(data);
    }

    @Override
    public byte[] digest() {
        return digest.digest();
    }

    @Override
    public void digest(byte[] output, int offset) {
        byte[] result = digest.digest();
        System.arraycopy(result, 0, output, offset, result.length);
    }

    @Override
    public void reset() {
        digest.reset();
    }

    @Override
    public String name() {
        return algorithm;
    }

    @Override
    public AbstractDigest copy() throws CloneNotSupportedException {
        AbstractDigest digest = (AbstractDigest) super.clone();
        digest.digest = (MessageDigest) this.digest.clone();
        digest.algorithm = this.algorithm;
        return digest;
    }
}
