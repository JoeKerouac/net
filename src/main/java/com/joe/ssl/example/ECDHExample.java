package com.joe.ssl.example;

import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.net.ssl.SSLHandshakeException;

import com.joe.ssl.crypto.ECDHKeyExchangeSpi;
import com.joe.ssl.crypto.impl.BCECDHKeyExchangeSpi;
import com.joe.ssl.crypto.impl.SunecECDHKeyExchangeSpi;
import com.joe.ssl.openjdk.ssl.ECDHCrypt;
import com.joe.ssl.openjdk.ssl.EllipticCurvesExtension;

import sun.security.ec.SunEC;
import sun.security.util.ECUtil;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-10-31 11:12
 */
public class ECDHExample {

    public static void main(String[] args) {
        List<ECDHKeyExchangeSpi> keyExchangeSpis = new ArrayList<>();
        keyExchangeSpis.add(new BCECDHKeyExchangeSpi());
        keyExchangeSpis.add(new SunecECDHKeyExchangeSpi());

        int flag = 0;
        try {
            while (true) {
                flag ++;
                testGenerate(keyExchangeSpis, 9);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        System.out.println(flag);
    }

    private static void testGenerate(List<ECDHKeyExchangeSpi> keyExchangeSpis, int curveId) {

        keyExchangeSpis.forEach(keyExchangeSpi -> {
            synchronized (ECDHExample.class) {
                ECDHKeyPair keyPair = new BCECDHKeyExchangeSpi().generate(curveId);
                testKeyExchange(keyExchangeSpis, keyPair.getPublicKey(), keyPair.getPrivateKey(),
                        curveId);
            }
        });
    }

    private static void testKeyExchange(List<ECDHKeyExchangeSpi> keyExchangeSpis, byte[] publicKey,
                                        byte[] privateKey, int curveId) {

        List<byte[]> result = new ArrayList<>();
        keyExchangeSpis.stream().forEach(keyExchangeSpi -> result
            .add(keyExchangeSpi.keyExchange(publicKey, privateKey, curveId)));

        for (int i = 1; i < result.size(); i++) {
            if (!Arrays.equals(result.get(0), result.get(i))) {
                System.out.println(Arrays.toString(result.get(0)));
                System.out.println(Arrays.toString(result.get(i)));
                throw new RuntimeException("两个实现不一致，请检查");
            }
        }
    }

    //    public static void main(String[] args) throws Throwable {
    //        byte[] serverPlublicKey = { 4, 2, 70, -31, 18, -112, 13, 38, 23, 14, 112, -42, 32, -80, 3,
    //                                    -119, 22, 18, -104, -82, 53, -18, 12, -25, -5, 89, 36, -1, -124,
    //                                    -58, 8, -13, -122, 54, 42, 13, 113, 4, 117, 26, -69, 114, 117,
    //                                    105, -56, 16, 92, 63, -107, -4, 10, 87, -34, 50, 6, 64, -66, 1,
    //                                    -90, -10, 66, -77, 82, -20, -51, 53, -13, 98, 39, 6, -39, 37,
    //                                    -55 };
    //        int curveId = 9;
    //        System.out.println(Arrays.toString(testJVMClient(curveId, serverPlublicKey)));
    //    }

    /* -----------------------------------------分割线---------------------------------------------- */

    /**
     * 使用指定publicKeyData进行
     * @param curveId
     * @param publicKeyData
     * @return
     * @throws Exception
     */
    public static byte[] testJVMClient(int curveId, byte[] publicKeyData) throws Exception {
        String curveOid = EllipticCurvesExtension.getCurveOid(curveId);

        ECParameterSpec parameters = ECUtil.getECParameterSpec(new SunEC(), curveOid);

        java.security.spec.ECPoint point = ECUtil.decodePoint(publicKeyData, parameters.getCurve());
        KeyFactory factory = KeyFactory.getInstance("EC");
        ECPublicKey publicKey = (ECPublicKey) factory
            .generatePublic(new ECPublicKeySpec(point, parameters));
        ECDHCrypt ecdh = new ECDHCrypt(publicKey.getParams(), new SecureRandom());
        PublicKey ephemeralServerKey = publicKey;
        SecretKey preMasterSecret = ecdh.getAgreedSecret(ephemeralServerKey);
        return preMasterSecret.getEncoded();
    }

    public static byte[] keyExchange(int curveId, byte[] serverPublicKey,
                                     byte[] clientPrivateKey) throws Exception {

        String curveOid = EllipticCurvesExtension.getCurveOid(curveId);
        ECParameterSpec parameters = ECUtil.getECParameterSpec(new SunEC(), curveOid);
        ECPoint point = ECUtil.decodePoint(serverPublicKey, parameters.getCurve());

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
