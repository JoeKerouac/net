package com.joe.tls.example;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;

import javax.crypto.KeyAgreement;

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

import com.joe.ssl.openjdk.ssl.EllipticCurvesExtension;
import com.joe.tls.ECDHKeyPair;
import com.joe.tls.crypto.ECDHKeyExchangeSpi;
import com.joe.tls.crypto.impl.SunecECDHKeyExchangeSpi;
import com.joe.tls.enums.NamedCurve;

import sun.security.ec.SunEC;
import sun.security.util.ECUtil;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-10-31 11:47
 */
public class BCECDHExample {

    public static void main(String[] args) throws Exception {
        int curveId = 9;

        ECDHKeyExchangeSpi exchangeSpi = new SunecECDHKeyExchangeSpi();
        //        ECDHKeyExchangeSpi exchangeSpi = new BCECDHKeyExchangeSpi();

        //        73 91(自带的，失败了)   584

        ECDHKeyPair keyPair = exchangeSpi.generate(curveId);

        System.out.println(
            Arrays.toString(keyExchange(curveId, keyPair.getPublicKey(), keyPair.getPrivateKey())));
        System.out.println(Arrays
            .toString(keyExchangeByBC(curveId, keyPair.getPublicKey(), keyPair.getPrivateKey())));
        System.out.println(Arrays.toString(
            exchangeSpi.keyExchange(keyPair.getPublicKey(), keyPair.getPrivateKey(), curveId)));
        //        testServer();
    }

    public static byte[] keyExchange(int curveId, byte[] serverPublicKey,
                                     byte[] clientPrivateKey) throws Exception {
        System.out.println(Arrays.toString(serverPublicKey));
        System.out.println(serverPublicKey.length);
        System.out.println("\n\n\n\n");
        String curveOid = EllipticCurvesExtension.getCurveOid(curveId);
        java.security.spec.ECParameterSpec parameters = ECUtil.getECParameterSpec(new SunEC(),
            curveOid);
        java.security.spec.ECPoint point = ECUtil.decodePoint(serverPublicKey,
            parameters.getCurve());

        KeyFactory factory = KeyFactory.getInstance("EC");
        ECPublicKey publicKey = (ECPublicKey) factory
            .generatePublic(new ECPublicKeySpec(point, parameters));

        ECPrivateKey privateKey = (ECPrivateKey) factory
            .generatePrivate(new ECPrivateKeySpec(new BigInteger(clientPrivateKey), parameters));
        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(privateKey);
        ka.doPhase(publicKey, true);
        return ka.generateSecret("TlsPremasterSecret").getEncoded();
    }

    public static byte[] keyExchangeByBC(int curveId, byte[] serverPublicKey,
                                         byte[] clientPrivateKey) {
        ECDomainParameters domainParameters = NamedCurve.getECParameters(curveId);

        // 使用指定数据解析出ECPoint
        ECPoint Q = domainParameters.getCurve().decodePoint(serverPublicKey);

        // EC密钥交换算法服务端公钥
        ECPublicKeyParameters ecAgreeServerPublicKey = new ECPublicKeyParameters(Q,
            domainParameters);

        AsymmetricCipherKeyPair asymmetricCipherKeyPair = generateECKeyPair(domainParameters);

        ECPrivateKeyParameters privateKeyParameters = new ECPrivateKeyParameters(
            new BigInteger(clientPrivateKey), domainParameters);

        // 计算preMasterKey
        return calculateECDHBasicAgreement(ecAgreeServerPublicKey, privateKeyParameters);
    }

    public static byte[] testServer() throws Exception {
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
        JCEECPublicKey publicKey = (JCEECPublicKey) keyPair.getPublic();
        ECParameterSpec ecParameterSpec = publicKey.getParameters();

        byte[] privateKeyData = privateKey.getEncoded();
        byte[] publicKeyData = publicKey.getEncoded();

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
