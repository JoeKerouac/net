package com.joe.tls;

import java.security.SecureRandom;

import com.joe.ssl.cipher.CipherSuite;
import com.joe.ssl.crypto.CipherSpi;

/**
 * @author JoeKerouac
 * @data 2020-11-06 22:24
 */
public class CipherBox {

    /**
     * 加密key
     */
    private final byte[]                 cipherKey;

    /**
     * 加密iv
     */
    private final byte[]                 iv;

    /**
     * 加密mac
     */
    private final byte[]                 mac;

    /**
     * 令牌
     */
    private final Authenticator          authenticator;

    /**
     * AEAD模式有该值
     */
    private final int                    tagSize;

    /**
     * 加密说明
     */
    private final CipherSuite.CipherDesc cipherDesc;

    /**
     * 安全随机数
     */
    private final SecureRandom           secureRandom;

    /**
     * 加密模式
     */
    private final int                    mode;

    /**
     * 加密接口
     */
    private final CipherSpi              cipherSpi;

    public CipherBox(SecureRandom secureRandom, CipherSuite.CipherDesc cipherDesc,
                     Authenticator authenticator, byte[] cipherKey, byte[] iv, byte[] mac,
                     boolean encrypt) {
        this.secureRandom = secureRandom;
        this.cipherDesc = cipherDesc;
        this.authenticator = authenticator;
        this.cipherKey = cipherKey;
        this.iv = iv;
        this.mac = mac;

        if (encrypt) {
            this.mode = CipherSpi.ENCRYPT_MODE;
        } else {
            this.mode = CipherSpi.DECRYPT_MODE;
        }

        if (cipherDesc.getCipherType() == CipherSuite.CipherType.AEAD) {
            this.tagSize = 16;
            this.cipherSpi = CipherSpi.getInstance(cipherDesc);
        } else {
            this.cipherSpi = CipherSpi.getInstance(cipherDesc);
            throw new RuntimeException("当前不支持的加密模式：" + cipherDesc.getCipherType());
        }
    }

    public byte[] encrypt(byte[] data, int offset, int len) {


        if (cipherDesc.getCipherType() == CipherSuite.CipherType.BLOCK) {
            int blockSize = cipherSpi.getBlockSize();
            // 如果是block模式，要对齐
            len = addPadding(data, offset, len, blockSize);

            return cipherSpi.doFinal(data)
        }

    }

    private static int addPadding(byte[] buf, int offset, int len, int blockSize) {
        int newlen = len + 1;
        byte pad;
        int i;

        if ((newlen % blockSize) != 0) {
            newlen += blockSize - 1;
            newlen -= newlen % blockSize;
        }
        pad = (byte) (newlen - len);

        if (buf.length < (newlen + offset)) {
            throw new IllegalArgumentException("no space to pad buffer");
        }

        /*
         * TLS version of the padding works for both SSLv3 and TLSv1
         */
        for (i = 0, offset += len; i < pad; i++) {
            buf[offset++] = (byte) (pad - 1);
        }
        return newlen;
    }

}
