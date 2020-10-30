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
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.joe.ssl.cipher.CipherSuite;
import com.joe.ssl.crypto.CipherSpi;

/**
 * AES加密器
 * 
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-10-28 21:46
 */
public class AesCipher implements CipherSpi {

    private Cipher                 cipher;

    private CipherSuite.CipherDesc cipherDesc;

    public AesCipher(CipherSuite.CipherDesc cipherAlgorithm) {
        this.cipherDesc = Objects.requireNonNull(cipherAlgorithm);

        try {
            this.cipher = Cipher.getInstance(cipherDesc.getCipherName(),
                new BouncyCastleProvider());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init(byte[] key, byte[] iv, int mode) {
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        try {
            cipher.init(mode, secretKeySpec, ivParameterSpec, new SecureRandom());
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
