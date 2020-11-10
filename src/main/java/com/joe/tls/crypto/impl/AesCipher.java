package com.joe.tls.crypto.impl;

import java.security.*;
import java.util.Objects;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.joe.tls.cipher.CipherSuite;
import com.joe.tls.crypto.CipherSpi;
import com.joe.utils.common.Assert;
import com.sun.crypto.provider.SunJCE;

/**
 * AES加密器
 * 
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-10-28 21:46
 */
public class AesCipher implements CipherSpi {

    /**
     * 实际的加密器
     */
    public final Cipher                  cipher;

    /**
     * 加密器提供者
     */
    private final Provider               provider;

    /**
     * 加密套件说明
     */
    private final CipherSuite.CipherDesc cipherDesc;

    /**
     * 加解密模式
     */
    private int                          mode = -1;

    public AesCipher(CipherSuite.CipherDesc cipherAlgorithm) {
        // 注意，BouncyCastleProvider目前还有问题，需要继续测试
        this(cipherAlgorithm, new SunJCE());
    }

    public AesCipher(CipherSuite.CipherDesc cipherAlgorithm, Provider provider) {
        this.cipherDesc = Objects.requireNonNull(cipherAlgorithm);
        this.provider = provider == null ? new SunJCE() : provider;

        try {
            // 这里使用默认的provider（JCE）
            this.cipher = Cipher.getInstance(cipherDesc.getCipherName(), provider);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getMode() {
        return mode;
    }

    @Override
    public void init(byte[] key, byte[] iv, int mode, SecureRandom secureRandom) {
        // 这里是固定的
        Assert.notNull(key, "key不能为null");
        Assert.notNull(iv, "IV不能为null");
        Assert.isTrue(mode == ENCRYPT_MODE || mode == DECRYPT_MODE, "不支持的加解密模式：" + mode);
        this.mode = mode;
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.clone(), "AES");
        try {
            if ((provider instanceof SunJCE) && cipherDesc.isGcm()) {
                // 注意，SunJCE提供的加密器如果是GCM模式，需要使用GCMParameterSpec
                iv = iv.clone();
                GCMParameterSpec parameterSpec = new GCMParameterSpec(16 * 8, iv);
                cipher.init(mode, secretKeySpec, parameterSpec, secureRandom);
            } else {
                IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
                cipher.init(mode, secretKeySpec, ivParameterSpec, secureRandom);
            }
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getBlockSize() {
        return cipher.getBlockSize();
    }

    @Override
    public void updateAAD(byte[] data) {
        cipher.updateAAD(data);
    }

    @Override
    public byte[] doFinal() {
        try {
            return cipher.doFinal();
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int doFinal(byte[] data, int offset, int len, byte[] result, int resultOffset) {
        try {
            return cipher.doFinal(data, offset, len, result, resultOffset);
        } catch (IllegalBlockSizeException | BadPaddingException | ShortBufferException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int update(byte[] data, int offset, int len, byte[] result, int resultOffset) {
        try {
            return cipher.update(data, offset, len, result, resultOffset);
        } catch (ShortBufferException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getOutputSize(int len) {
        // 对于javax.crypto.Cipher.getOutputSize来说，如果是加密模式，那么返回加密数据长度+tagLen，如果是解密模式，返回解密数据长度-tagLen
        return cipher.getOutputSize(len);
    }

    @Override
    public int getTagLen() {
        return cipherDesc.getTagLen();
    }

    @Override
    public String name() {
        return cipherDesc.getCipherName();
    }

    @Override
    public CipherSpi copy() throws CloneNotSupportedException {
        return new AesCipher(cipherDesc);
    }
}
