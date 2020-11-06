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
        
        if (cipherDesc.getCipherType() == CipherSuite.CipherType.AEAD) {
            this.tagSize = 16;
            this.cipherSpi = CipherSpi.getInstance(cipherDesc);
        } else {
            throw new RuntimeException("当前不支持的加密模式：" + cipherDesc.getCipherType());
        }

        if (encrypt) {
            this.mode = CipherSpi.ENCRYPT_MODE;
        } else {
            this.mode = CipherSpi.DECRYPT_MODE;
        }
    }

}
