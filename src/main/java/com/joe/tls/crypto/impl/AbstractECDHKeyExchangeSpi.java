package com.joe.tls.crypto.impl;

import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.KeyAgreement;

import com.joe.tls.ECDHKeyPair;
import com.joe.tls.crypto.ECDHKeyExchangeSpi;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-10-31 17:20
 */
public abstract class AbstractECDHKeyExchangeSpi implements ECDHKeyExchangeSpi {

    /**
     * 根据AlgorithmParameterSpec获取fieldSize
     * 
     * @param ecParamter
     *            AlgorithmParameterSpec
     * @return fieldSize
     */
    protected abstract int fieldSize(AlgorithmParameterSpec ecParamter);

    /**
     * 获取ECParameterSpec
     * 
     * @param curveId
     *            curveId
     * @return ECParameterSpec
     */
    protected abstract AlgorithmParameterSpec getECParameterSpec(int curveId);

    /**
     * 算法提供厂商
     * 
     * @return 算法提供厂商
     */
    protected abstract Provider provider();

    /**
     * 将公钥数据转换为KeySpec
     * 
     * @param curveId
     *            curveId
     * @param publicKeyData
     *            publicKeyData
     * @return KeySpec
     */
    protected abstract KeySpec convertToPublicKeySpec(int curveId, byte[] publicKeyData);

    /**
     * 将私钥数据转换为KeySpec
     * 
     * @param curveId
     *            curveId
     * @param privateKeyData
     *            privateKeyData
     * @return KeySpec
     */
    protected abstract KeySpec convertToPrivateKeySpec(int curveId, byte[] privateKeyData);

    @Override
    public byte[] keyExchange(byte[] publicKeyData, byte[] privateKeyData, int curveId) {
        try {
            KeyFactory factory = KeyFactory.getInstance("EC", provider());
            PublicKey publicKey = factory.generatePublic(convertToPublicKeySpec(curveId, publicKeyData));
            PrivateKey privateKey = factory.generatePrivate(convertToPrivateKeySpec(curveId, privateKeyData));
            KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH", provider());
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(publicKey, true);
            return keyAgreement.generateSecret();
        } catch (InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ECDHKeyPair generate(int curveId) {
        AlgorithmParameterSpec parameters = getECParameterSpec(curveId);

        try {
            KeyPairGenerator factory = KeyPairGenerator.getInstance("EC", provider());
            factory.initialize(parameters);
            KeyPair keyPair = factory.genKeyPair();
            ECDHKeyPair ecdhKeyPair = new ECDHKeyPair();
            ECPrivateKey ecPrivateKey = (ECPrivateKey)keyPair.getPrivate();
            ECPublicKey ecPublicKey = (ECPublicKey)keyPair.getPublic();

            ecdhKeyPair.setPublicKey(encodePoint(ecPublicKey.getW(), fieldSize(parameters)));
            ecdhKeyPair.setPrivateKey(ecPrivateKey.getS().toByteArray());
            return ecdhKeyPair;
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 对point进行编码
     * 
     * @param point
     *            point
     * @param fieldSize
     *            fieldSize
     * @return 编码结果
     */
    private byte[] encodePoint(ECPoint point, int fieldSize) {
        int var2 = fieldSize + 7 >> 3;
        byte[] var3 = trimZeroes(point.getAffineX().toByteArray());
        byte[] var4 = trimZeroes(point.getAffineY().toByteArray());
        if (var3.length <= var2 && var4.length <= var2) {
            byte[] var5 = new byte[1 + (var2 << 1)];
            var5[0] = 4;
            System.arraycopy(var3, 0, var5, var2 - var3.length + 1, var3.length);
            System.arraycopy(var4, 0, var5, var5.length - var4.length, var4.length);
            return var5;
        } else {
            throw new RuntimeException("Point coordinates do not match field size");
        }
    }

    /**
     * 将指定数组中开头位置的0去除
     * 
     * @param array
     *            数组
     * @return 去除开头0后的数组
     */
    private byte[] trimZeroes(byte[] array) {
        int offset;
        for (offset = 0; offset < array.length - 1 && array[offset] == 0; ++offset) {
        }

        return offset == 0 ? array : Arrays.copyOfRange(array, offset, array.length);
    }

}
