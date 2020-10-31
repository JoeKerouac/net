package com.joe.ssl.crypto.impl;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;

import com.joe.ssl.crypto.ECDHKeyExchangeSpi;
import com.joe.ssl.crypto.NamedCurve;
import com.joe.ssl.example.ECDHKeyPair;

import sun.security.ec.SunEC;
import sun.security.util.ECUtil;

/**
 * 使用sunec实现的ECDH密钥交换
 * 
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-10-31 14:56
 */
public class SunecECDHKeyExchangeSpi implements ECDHKeyExchangeSpi {

    @Override
    public byte[] keyExchange(byte[] publicKey, byte[] privateKey, int curveId) {
        ECParameterSpec parameters = ECUtil.getECParameterSpec(new SunEC(),
            NamedCurve.getCurveName(curveId));

        try {
            ECPoint point = ECUtil.decodePoint(publicKey, parameters.getCurve());

            KeyFactory factory = KeyFactory.getInstance("EC", new SunEC());
            ECPublicKey ecAgreePublicKey = (ECPublicKey) factory
                .generatePublic(new ECPublicKeySpec(point, parameters));

            ECPrivateKey ecAgreePrivateKey = (ECPrivateKey) factory
                .generatePrivate(new ECPrivateKeySpec(new BigInteger(privateKey), parameters));

            KeyAgreement ka = KeyAgreement.getInstance("ECDH", new SunEC());
            ka.init(ecAgreePrivateKey);
            ka.doPhase(ecAgreePublicKey, true);
            SecretKey secretKey = ka.generateSecret("TlsPremasterSecret");
            return secretKey.getEncoded();
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException
                | InvalidKeyException e) {
            throw new RuntimeException("密钥交换失败", e);
        }
    }

    @Override
    public ECDHKeyPair generate(int curveId) {
        ECParameterSpec parameters = ECUtil.getECParameterSpec(new SunEC(),
            NamedCurve.getCurveName(curveId));

        try {
            KeyPairGenerator factory = KeyPairGenerator.getInstance("EC", new SunEC());
            // 这一步初始化不能省略
            factory.initialize(parameters);
            KeyPair keyPair = factory.genKeyPair();
            ECDHKeyPair ecdhKeyPair = new ECDHKeyPair();
            ECPrivateKey ecPrivateKey = (ECPrivateKey) keyPair.getPrivate();
            ECPublicKey ecPublicKey = (ECPublicKey) keyPair.getPublic();

            ecdhKeyPair.setPublicKey(ECUtil.encodePoint(ecPublicKey.getW(), parameters.getCurve()));
            ecdhKeyPair.setPrivateKey(ecPrivateKey.getS().toByteArray());
            return ecdhKeyPair;
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
