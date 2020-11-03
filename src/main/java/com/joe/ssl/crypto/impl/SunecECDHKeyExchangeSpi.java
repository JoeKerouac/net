package com.joe.ssl.crypto.impl;

import java.io.IOException;
import java.math.BigInteger;
import java.security.Provider;
import java.security.spec.*;

import com.joe.ssl.crypto.ECDHKeyExchangeSpi;
import com.joe.tls.enums.NamedCurve;

import sun.security.ec.SunEC;
import sun.security.util.ECUtil;

/**
 * 使用sunec实现的ECDH密钥交换
 * 
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-10-31 14:56
 */
public class SunecECDHKeyExchangeSpi extends AbstractECDHKeyExchangeSpi
                                     implements ECDHKeyExchangeSpi {

    private static final Provider SUNEC_PROVIDER = new SunEC();

    @Override
    protected Provider provider() {
        return SUNEC_PROVIDER;
    }

    @Override
    protected KeySpec convertToPublicKeySpec(int curveId, byte[] publicKeyData) {
        ECParameterSpec parameters = ECUtil.getECParameterSpec(new SunEC(),
            NamedCurve.getCurveName(curveId));
        ECPoint point;
        try {
            point = ECUtil.decodePoint(publicKeyData, parameters.getCurve());
        } catch (IOException e) {
            // 这里不会发生
            throw new RuntimeException(e);
        }
        return new ECPublicKeySpec(point, parameters);
    }

    @Override
    protected KeySpec convertToPrivateKeySpec(int curveId, byte[] privateKeyData) {
        ECParameterSpec parameters = ECUtil.getECParameterSpec(new SunEC(),
            NamedCurve.getCurveName(curveId));

        return new ECPrivateKeySpec(new BigInteger(privateKeyData), parameters);
    }

    @Override
    protected AlgorithmParameterSpec getECParameterSpec(int curveId) {
        return ECUtil.getECParameterSpec(new SunEC(), NamedCurve.getCurveName(curveId));
    }

    @Override
    protected int fieldSize(AlgorithmParameterSpec ecParamter) {
        if (!(ecParamter instanceof ECParameterSpec)) {
            throw new RuntimeException("不支持的参数：" + ecParamter);
        }
        return ((ECParameterSpec) ecParamter).getCurve().getField().getFieldSize();
    }
}
