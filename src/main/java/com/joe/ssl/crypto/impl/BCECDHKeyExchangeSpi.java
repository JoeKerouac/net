package com.joe.ssl.crypto.impl;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;

import com.joe.ssl.crypto.ECDHKeyExchangeSpi;
import com.joe.ssl.crypto.NamedCurve;
import com.joe.ssl.example.ECDHKeyPair;
import com.joe.utils.common.string.StringUtils;

/**
 * 使用bouncycastle实现的ECDH密钥交换算法
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-10-31 14:28
 */
public class BCECDHKeyExchangeSpi implements ECDHKeyExchangeSpi {

    private static final Map<String, X9ECParameters> CACHE = new ConcurrentHashMap<>();

    @Override
    public byte[] keyExchange(byte[] publicKey, byte[] privateKey, int curveId) {
        ECDomainParameters domainParameters = getECParameters(curveId);

        // 使用指定数据解析出ECPoint
        ECPoint Q = domainParameters.getCurve().decodePoint(publicKey);

        // 将publicKey的二进制数据转换为ECPublicKeyParameters
        ECPublicKeyParameters ecAgreePublicKey = new ECPublicKeyParameters(Q, domainParameters);
        // 私钥
        ECPrivateKeyParameters ecAgreePrivateKey = new ECPrivateKeyParameters(
            new BigInteger(privateKey), domainParameters);

        // 密钥交换
        ECDHBasicAgreement basicAgreement = new ECDHBasicAgreement();
        basicAgreement.init(ecAgreePrivateKey);
        BigInteger agreement = basicAgreement.calculateAgreement(ecAgreePublicKey);
        return BigIntegers.asUnsignedByteArray(agreement);
    }

    @Override
    public ECDHKeyPair generate(int curveId) {
        ECDomainParameters ecDomainParameters = getECParameters(curveId);

        ECKeyGenerationParameters keyGenerationParameters = new ECKeyGenerationParameters(
            ecDomainParameters, new SecureRandom());
        ECKeyPairGenerator keyPairGenerator = new ECKeyPairGenerator();
        keyPairGenerator.init(keyGenerationParameters);
        AsymmetricCipherKeyPair keyPair = keyPairGenerator.generateKeyPair();
        ECPrivateKeyParameters privateKeyParameters = (ECPrivateKeyParameters) keyPair.getPrivate();
        ECPublicKeyParameters publicKeyParameters = (ECPublicKeyParameters) keyPair.getPublic();
        ECDHKeyPair pair = new ECDHKeyPair();
        pair.setPrivateKey(privateKeyParameters.getD().toByteArray());
        pair.setPublicKey(publicKeyParameters.getQ().getEncoded());
        return pair;
    }

    /**
     * 根据curveId获取ECDomainParameters
     * @param curveId curveId
     * @return ECDomainParameters
     */
    private static ECDomainParameters getECParameters(int curveId) {
        String curveName = NamedCurve.getCurveName(curveId);
        if (StringUtils.isEmpty(curveName)) {
            throw new RuntimeException("不支持的curveId:" + curveId);
        }

        X9ECParameters ecP = CACHE.compute(curveName, (name, paramter) -> {
            if (paramter == null) {
                return SECNamedCurves.getByName(curveName);
            } else {
                return paramter;
            }
        });

        if (ecP == null) {
            throw new RuntimeException("不支持的curveId:" + curveId);
        }

        return new ECDomainParameters(ecP.getCurve(), ecP.getG(), ecP.getN(), ecP.getH(),
            ecP.getSeed());
    }
}
