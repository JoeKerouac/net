package com.joe.ssl.crypto.impl;

import java.math.BigInteger;
import java.security.Provider;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import com.joe.ssl.crypto.ECDHKeyExchangeSpi;
import com.joe.tls.enums.NamedCurve;
import com.joe.utils.common.string.StringUtils;

/**
 * 使用bouncycastle实现的ECDH密钥交换算法
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-10-31 14:28
 */
public class BCECDHKeyExchangeSpi extends AbstractECDHKeyExchangeSpi implements ECDHKeyExchangeSpi {

    private static final Map<String, X9ECParameters> CACHE       = new ConcurrentHashMap<>();

    private static final Provider                    BC_PROVIDER = new BouncyCastleProvider();

    @Override
    protected Provider provider() {
        return BC_PROVIDER;
    }

    @Override
    protected KeySpec convertToPublicKeySpec(int curveId, byte[] publicKeyData) {
        ECDomainParameters domainParameters = getECDomainParameters(curveId);

        // 使用指定数据解析出ECPoint
        ECPoint Q = domainParameters.getCurve().decodePoint(publicKeyData);

        ECParameterSpec ecParameterSpec = internalGetECParameterSpec(curveId);
        return new ECPublicKeySpec(Q, ecParameterSpec);
    }

    @Override
    protected KeySpec convertToPrivateKeySpec(int curveId, byte[] privateKeyData) {
        ECParameterSpec ecParameterSpec = internalGetECParameterSpec(curveId);
        return new ECPrivateKeySpec(new BigInteger(privateKeyData), ecParameterSpec);
    }

    @Override
    protected AlgorithmParameterSpec getECParameterSpec(int curveId) {
        return internalGetECParameterSpec(curveId);
    }

    @Override
    protected int fieldSize(AlgorithmParameterSpec ecParamter) {
        if (!(ecParamter instanceof ECParameterSpec)) {
            throw new RuntimeException("不支持的参数：" + ecParamter);
        }
        return ((ECParameterSpec) ecParamter).getCurve().getFieldSize();
    }

    /**
     * 根据curveId获取ECDomainParameters
     * @param curveId curveId
     * @return ECDomainParameters
     */
    private static ECDomainParameters getECDomainParameters(int curveId) {
        String curveName = getCurveName(curveId);

        X9ECParameters ecP = getX9ECParameters(curveName);

        return new ECDomainParameters(ecP.getCurve(), ecP.getG(), ecP.getN(), ecP.getH(),
            ecP.getSeed());
    }

    /**
     * 根据curveId获取ECDomainParameters
     * @param curveId curveId
     * @return ECDomainParameters
     */
    private static ECParameterSpec internalGetECParameterSpec(int curveId) {
        String curveName = getCurveName(curveId);

        X9ECParameters ecP = getX9ECParameters(curveName);

        return new ECNamedCurveParameterSpec(curveName, ecP.getCurve(), ecP.getG(), ecP.getN(),
            ecP.getH(), ecP.getSeed());
    }

    private static String getCurveName(int curveId) {
        String curveName = NamedCurve.getCurveName(curveId);
        if (StringUtils.isEmpty(curveName)) {
            throw new RuntimeException("不支持的curveId:" + curveId);
        }
        return curveName;
    }

    private static X9ECParameters getX9ECParameters(String curveName) {
        X9ECParameters ecP = CACHE.compute(curveName, (name, paramter) -> {
            if (paramter == null) {
                return SECNamedCurves.getByName(curveName);
            } else {
                return paramter;
            }
        });

        if (ecP == null) {
            throw new RuntimeException("不支持的curveName:" + curveName);
        }
        return ecP;
    }
}
