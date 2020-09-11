package com.joe.ssl.message;

import java.io.IOException;
import java.security.*;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.JCEECPublicKey;

/**
 *
 *
 * @author JoeKerouac
 * @version 2020年06月14日 11:14
 */
public class ECDHServerKeyExchange implements HandshakeMessage {

    /**
     * clientHello中的随机数
     */
    private byte[]                                       clientRandom;

    /**
     * 服务端生成的随机数
     */
    private byte[]                                       serverRandom;

    /**
     * 服务端私钥
     */
    private PrivateKey                                   serverPrivateKey;

    /**
     * 签名器
     */
    private Signature                                    signature;

    /**
     * 签名算法
     */
    private SignatureAndHashAlgorithm.SignatureAlgorithm signatureAlgorithm;

    /**
     * 签名数据
     */
    private byte[]                                       signData;

    /**
     * 服务端EC公钥
     */
    private JCEECPublicKey                               serverPublicKey;

    /**
     * 服务端证书私钥，签名用
     */
    private PrivateKey                                   privateKey;

    /**
     * curve type，固定3
     */
    private int                                          curveType  = 3;

    /**
     * named curve : 固定23，即secp256r1
     */
    private int                                          namedCurve = 23;

    public void initSign() throws InvalidKeyException, SignatureException {
        byte[] pointBytes = serverPublicKey.getQ().getEncoded();
        signature.initSign(serverPrivateKey);
        signature.update(clientRandom);
        signature.update(serverRandom);
        // 固定curve type
        signature.update((byte) curveType);
        // named curve : 固定23，即secp256r1
        signature.update((byte) (namedCurve >> 8));
        signature.update((byte) namedCurve);
        signature.update((byte) pointBytes.length);
        signature.update(pointBytes);
        signData = signature.sign();
    }

    public void write() throws Throwable {
        // 添加bouncycastle实现
        Security.addProvider(new BouncyCastleProvider());

        // 服务端使用的namedCurve
        // 23是secp256r1
        int namedCurve = 23;

        // key大小
        int keySize = 256;

        //        // 这里因为getECParameters方法不是共有的，也懒得copy出来了，所以直接反射调用，其中23是服务器返回的Named Curve
        //        ECDomainParameters domainParameters = ReflectUtil.invoke(NamedCurve.class,
        //                "getECParameters", new Class[] { int.class }, new Object[] { namedCurve });
        //
        //        // 获取出来ECDH实现（bouncycastle提供），provider选择BC（固定的，在BouncyCastleProvider中定义）
        //        KeyFactory keyFactory = KeyFactory.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);

        // 准备生成密钥对
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH",
            BouncyCastleProvider.PROVIDER_NAME);
        keyPairGenerator.initialize(keySize);
        // 生成服务端的密钥
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

    }

    @Override
    public int size() {
        throw new RuntimeException("未实现");
    }

    @Override
    public HandshakeType type() {
        return HandshakeType.SERVER_KEY_EXCHANGE;
    }

    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        Provider provider = new BouncyCastleProvider();
        System.out.println(provider);
        provider.forEach((k, v) -> {
            if (k.toString().startsWith("Alg.Alias.Signature")) {
                System.out.println(k + ":" + v);
            }
        });
    }

    @Override
    public void write(WrapedOutputStream stream) throws IOException {
        stream.writeInt8(type().getCode());
        byte[] publicKeyData = serverPublicKey.getQ().getEncoded();
        int len = 7 + publicKeyData.length + signData.length;
        // 后边消息总长
        stream.writeInt8(len);
        stream.writeInt8(curveType);
        stream.writeInt16(namedCurve);
        stream.write(publicKeyData);
        // 签名算法写死0601(SHA512withRSA)
        stream.writeInt8(6);
        stream.writeInt8(1);
        stream.writeInt16(signData.length);
        stream.write(signData);
    }
}
