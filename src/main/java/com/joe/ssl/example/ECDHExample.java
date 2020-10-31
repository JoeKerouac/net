package com.joe.ssl.example;

import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.net.ssl.SSLHandshakeException;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;

import com.joe.ssl.NamedCurve;
import com.joe.ssl.openjdk.ssl.ECDHCrypt;
import com.joe.ssl.openjdk.ssl.EllipticCurvesExtension;

import sun.security.util.ECUtil;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-10-31 11:12
 */
public class ECDHExample {

    public static void main(String[] args) throws Throwable {
        byte[] serverPlublicKey = { 4, 2, 70, -31, 18, -112, 13, 38, 23, 14, 112, -42, 32, -80, 3,
                                    -119, 22, 18, -104, -82, 53, -18, 12, -25, -5, 89, 36, -1, -124,
                                    -58, 8, -13, -122, 54, 42, 13, 113, 4, 117, 26, -69, 114, 117,
                                    105, -56, 16, 92, 63, -107, -4, 10, 87, -34, 50, 6, 64, -66, 1,
                                    -90, -10, 66, -77, 82, -20, -51, 53, -13, 98, 39, 6, -39, 37,
                                    -55 };
        int curveId = 9;
        System.out.println(Arrays.toString(testBCClient(curveId, serverPlublicKey)));
        System.out.println(Arrays.toString(testJVMClient(curveId, serverPlublicKey)));
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

    /* -----------------------------------------分割线---------------------------------------------- */

    public static byte[] testJVMClient(int curveId, byte[] publicKeyData) throws Exception {
        String curveOid = EllipticCurvesExtension.getCurveOid(curveId);

        AlgorithmParameters algorithmParameters = AlgorithmParameters.getInstance("EC");
        algorithmParameters.init(new ECGenParameterSpec("EC"));
        ECParameterSpec parameters = (ECParameterSpec) algorithmParameters
            .getParameterSpec(ECParameterSpec.class);

        //        ECParameterSpec parameters = ECUtil.getECParameterSpec(new SunJCE(), curveOid);

        java.security.spec.ECPoint point = ECUtil.decodePoint(publicKeyData, parameters.getCurve());
        KeyFactory factory = KeyFactory.getInstance("EC");
        ECPublicKey publicKey = (ECPublicKey) factory
            .generatePublic(new ECPublicKeySpec(point, parameters));
        ECDHCrypt ecdh = new ECDHCrypt(publicKey.getParams(), new SecureRandom());
        PublicKey ephemeralServerKey = publicKey;
        SecretKey preMasterSecret = ecdh.getAgreedSecret(ephemeralServerKey);
        return preMasterSecret.getEncoded();

        //        ECPublicKey key = mesg.getPublicKey();
        //
        //        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        //        ECGenParameterSpec params = EllipticCurvesExtension.getECGenParamSpec(curveId);
        //        kpg.initialize(params, new SecureRandom());
        //        KeyPair kp = kpg.generateKeyPair();
        //        PrivateKey privateKey = kp.getPrivate();
        //        ECPublicKey publicKey = (ECPublicKey) kp.getPublic();

    }

    static SecretKey getAgreedSecret(byte[] encodedPoint, ECPublicKey publicKey,
                                     PrivateKey privateKey) throws SSLHandshakeException {

        try {
            ECParameterSpec params = publicKey.getParams();
            java.security.spec.ECPoint point = ECUtil.decodePoint(encodedPoint, params.getCurve());
            KeyFactory kf = KeyFactory.getInstance("EC");
            ECPublicKeySpec spec = new ECPublicKeySpec(point, params);
            PublicKey peerPublicKey = kf.generatePublic(spec);
            return getAgreedSecret(peerPublicKey, privateKey);
        } catch (GeneralSecurityException | java.io.IOException e) {
            throw (SSLHandshakeException) new SSLHandshakeException("Could not generate secret")
                .initCause(e);
        }
    }

    static SecretKey getAgreedSecret(PublicKey peerPublicKey,
                                     PrivateKey privateKey) throws SSLHandshakeException {
        try {
            KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
            keyAgreement.init(privateKey);
            keyAgreement.doPhase(peerPublicKey, true);
            return keyAgreement.generateSecret("TlsPremasterSecret");
        } catch (GeneralSecurityException e) {
            throw (SSLHandshakeException) new SSLHandshakeException("Could not generate secret")
                .initCause(e);
        }
    }

}
