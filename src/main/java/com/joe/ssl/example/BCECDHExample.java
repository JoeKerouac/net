package com.joe.ssl.example;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.JCEECPrivateKey;
import org.bouncycastle.jce.provider.JCEECPublicKey;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;

import com.joe.ssl.NamedCurve;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-10-31 11:47
 */
public class BCECDHExample {

    public static void main(String[] args) throws Exception {
        testServer(1, null);
    }

    public static byte[] testServer(int curveId, byte[] publicKeyData) throws Exception {
        // 添加bouncycastle实现

        // 服务端使用的namedCurve
        // 23是secp256r1
        int namedCurve = 23;

        // key大小
        int keySize = 256;

        // 准备生成密钥对
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH",
            new BouncyCastleProvider());

        keyPairGenerator.initialize(keySize);
        // 生成服务端的密钥
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        JCEECPrivateKey privateKey = (JCEECPrivateKey) keyPair.getPrivate();
        JCEECPublicKey publicKey = (JCEECPublicKey)keyPair.getPublic();
        ECParameterSpec ecParameterSpec = publicKey.getParameters();
        ecParameterSpec.getCurve();


        return null;
    }

    public static byte[] testBCClient(int curveId, byte[] publicKeyData) {
        ECDomainParameters domainParameters = NamedCurve.getECParameters(curveId);

        // 使用指定数据解析出ECPoint
        ECPoint Q = domainParameters.getCurve().decodePoint(publicKeyData);

        // EC密钥交换算法服务端公钥
        ECPublicKeyParameters ecAgreeServerPublicKey = new ECPublicKeyParameters(Q,
            domainParameters);

        AsymmetricCipherKeyPair asymmetricCipherKeyPair = generateECKeyPair(domainParameters);

        ECPrivateKeyParameters ecAgreeClientPrivateKey = (ECPrivateKeyParameters) asymmetricCipherKeyPair
            .getPrivate();
        ECPublicKeyParameters ecAgreeClientPublicKey = (ECPublicKeyParameters) asymmetricCipherKeyPair
            .getPublic();

        // 计算preMasterKey
        return calculateECDHBasicAgreement(ecAgreeServerPublicKey, ecAgreeClientPrivateKey);
    }

    /**
     * 根据自己的私钥和对方的公钥计算PremasterSecret，进而计算MasterSecret
     *
     * @param publicKey  公钥
     * @param privateKey 私钥
     * @return PremasterSecret
     */
    protected static byte[] calculateECDHBasicAgreement(ECPublicKeyParameters publicKey,
                                                        ECPrivateKeyParameters privateKey) {
        ECDHBasicAgreement basicAgreement = new ECDHBasicAgreement();
        basicAgreement.init(privateKey);
        BigInteger agreement = basicAgreement.calculateAgreement(publicKey);
        return BigIntegers.asUnsignedByteArray(agreement);
    }

    /**
     * 生成非对称加密密钥
     *
     * @param ecParams EC加密参数
     * @return 根据EC加密参数得到的非对称加密密钥
     */
    protected static AsymmetricCipherKeyPair generateECKeyPair(ECDomainParameters ecParams) {
        ECKeyPairGenerator keyPairGenerator = new ECKeyPairGenerator();
        ECKeyGenerationParameters keyGenerationParameters = new ECKeyGenerationParameters(ecParams,
            new SecureRandom());
        keyPairGenerator.init(keyGenerationParameters);
        return keyPairGenerator.generateKeyPair();
    }

}
