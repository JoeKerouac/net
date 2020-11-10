package com.joe.ssl.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.KeyAgreement;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.joe.tls.ECDHKeyPair;
import com.joe.tls.crypto.ECDHKeyExchangeSpi;
import com.joe.tls.crypto.impl.BCECDHKeyExchangeSpi;
import com.joe.tls.crypto.impl.SunecECDHKeyExchangeSpi;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-10-31 11:12
 */
public class ECDHExample {

    public static void main(String[] args) throws Exception {
        test();
        //        BCECDHKeyExchangeSpi bcecdhKeyExchangeSpi = new BCECDHKeyExchangeSpi();
        //        ECDHKeyPair ecdhKeyPair = bcecdhKeyExchangeSpi.generate(9);
        //        bcecdhKeyExchangeSpi.keyExchange(ecdhKeyPair.getPublicKey(), ecdhKeyPair.getPrivateKey(), 9);
    }

    public static void test() throws Exception {
        List<ECDHKeyExchangeSpi> keyExchangeSpis = new ArrayList<>();
        keyExchangeSpis.add(new BCECDHKeyExchangeSpi());
        keyExchangeSpis.add(new SunecECDHKeyExchangeSpi());
        byte[] publicKey = { 4, 2, 73, -9, 40, 105, -119, -109, 56, -18, -59, -82, -13, 17, -19,
                             104, 97, -45, 119, -41, 62, 13, 37, 70, 20, 122, -9, -87, -22, 75, 12,
                             4, -18, 61, -66, -59, -9, 3, 59, 88, -19, -65, -70, -9, -86, -6, 79,
                             97, -63, -73, -84, 88, -119, -62, -90, -12, -123, 18, 24, 116, -15,
                             -45, -124, 39, -22, 65, 126, 54, -39, -22, -80, 24, -94 };
        byte[] privateKey = { 65, -120, -116, 123, -25, -98, 70, 73, 70, -104, -126, 6, -73, 74,
                              123, 16, -16, -118, 121, -80, -89, 24, -106, -83, -51, 31, -50, 86,
                              -95, -87, -116, -21, 89, -59, 111 };
        System.out.println(publicKey.length);
        System.out.println(privateKey.length);

        KeyAgreement bKeyAgree = KeyAgreement.getInstance("ECDH", new BouncyCastleProvider());
        System.out.println(bKeyAgree.getClass());

        while (true) {
            // 有时候两个密钥交换结果会不一致，目前不清楚为什么
            testGenerate(keyExchangeSpis, 9);
        }
    }

    private static void testGenerate(List<ECDHKeyExchangeSpi> keyExchangeSpis, int curveId) {

        keyExchangeSpis.forEach(keyExchangeSpi -> {
            synchronized (ECDHExample.class) {
                ECDHKeyPair keyPair = keyExchangeSpi.generate(curveId);
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
            //            System.out.println("公钥长度:" + publicKey.length);
            //            System.out.println("私钥长度:" + privateKey.length);
            //            System.out.println("结果长度1是：" + result.get(0).length);
            //            System.out.println("结果长度2是：" + result.get(i).length);
            if (!Arrays.equals(result.get(0), result.get(i))) {
                System.out.println(Arrays.toString(result.get(0)));
                System.out.println(Arrays.toString(result.get(i)));
                System.out.println(Arrays.toString(publicKey));
                System.out.println(Arrays.toString(privateKey));
                System.out.println(curveId);
                throw new RuntimeException("两个实现不一致，请检查");
            }
        }
    }

}
