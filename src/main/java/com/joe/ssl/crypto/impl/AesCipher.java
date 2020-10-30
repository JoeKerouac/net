package com.joe.ssl.crypto.impl;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.joe.ssl.cipher.CipherSuite;
import com.joe.ssl.crypto.CipherSpi;
import com.sun.crypto.provider.SunJCE;

/**
 * AES加密器
 * 
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-10-28 21:46
 */
public class AesCipher implements CipherSpi {

    public Cipher                 cipher;

    private CipherSuite.CipherDesc cipherDesc;

    public AesCipher(CipherSuite.CipherDesc cipherAlgorithm) {
        this.cipherDesc = Objects.requireNonNull(cipherAlgorithm);

        try {
            // 这里使用默认的provider（JCE）
            this.cipher = Cipher.getInstance(cipherDesc.getCipherName(), new SunJCE());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init(byte[] key, byte[] iv, int mode) {
        // 这里是固定的
        GCMParameterSpec parameterSpec = new GCMParameterSpec(16 * 8, iv);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        try {
            cipher.init(mode, secretKeySpec, parameterSpec, new SecureRandom());
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(byte[] data) {
        cipher.update(data);
    }

    @Override
    public void updateAAD(byte[] data) {
        cipher.updateAAD(data);
    }

    @Override
    public byte[] doFinal(byte[] data) {
        try {
            return cipher.doFinal(data);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
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
    public String name() {
        return cipherDesc.getCipherName();
    }

    @Override
    public CipherSpi copy() throws CloneNotSupportedException {
        return new AesCipher(cipherDesc);
    }
}
