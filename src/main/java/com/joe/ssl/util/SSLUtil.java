package com.joe.ssl.util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;

import com.joe.utils.collection.CollectionUtil;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.Strings;

/**
 * @author JoeKerouac
 * @version 2020年06月14日 11:41
 */
public class SSLUtil {

    /**
     * ECDH密钥交换
     * @param privateKey 自己的私钥
     * @param publicKey 对方的公钥
     * @param algorithm 对称加密密钥的算法
     * @return 对称加密密钥
     * @throws NoSuchAlgorithmException 指定算法不存在异常
     * @throws InvalidKeyException 指定key非法
     */
    public static SecretKey ecdhExchange(ECPrivateKey privateKey, ECPublicKey publicKey,
                                         String algorithm) throws NoSuchAlgorithmException,
                                                           InvalidKeyException {
        // 添加bouncycastle实现
        Security.addProvider(new BouncyCastleProvider());

        // 初始化密钥协议，使用ECDH密钥交换协议
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        // 首先使用自己的私钥初始化
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);
        return keyAgreement.generateSecret(algorithm);
    }

    public static byte[] PRF(byte[] secret, String asciiLabel, byte[] seed, int size) {
        byte[] label = Strings.toByteArray(asciiLabel);

        int s_half = (secret.length + 1) / 2;
        byte[] s1 = new byte[s_half];
        byte[] s2 = new byte[s_half];
        System.arraycopy(secret, 0, s1, 0, s_half);
        System.arraycopy(secret, secret.length - s_half, s2, 0, s_half);

        byte[] ls = CollectionUtil.merge(label, seed);

        byte[] buf = new byte[size];
        byte[] prf = new byte[size];
        hmac_hash(new MD5Digest(), s1, ls, prf);
        hmac_hash(new SHA1Digest(), s2, ls, buf);
        for (int i = 0; i < size; i++) {
            buf[i] ^= prf[i];
        }
        return buf;
    }

    private static void hmac_hash(Digest digest, byte[] secret, byte[] seed, byte[] out) {
        HMac mac = new HMac(digest);
        KeyParameter param = new KeyParameter(secret);
        byte[] a = seed;
        int size = digest.getDigestSize();
        int iterations = (out.length + size - 1) / size;
        byte[] buf = new byte[mac.getMacSize()];
        byte[] buf2 = new byte[mac.getMacSize()];
        for (int i = 0; i < iterations; i++) {
            mac.init(param);
            mac.update(a, 0, a.length);
            mac.doFinal(buf, 0);
            a = buf;
            mac.init(param);
            mac.update(a, 0, a.length);
            mac.update(seed, 0, seed.length);
            mac.doFinal(buf2, 0);
            System.arraycopy(buf2, 0, out, (size * i), Math.min(size, out.length - (size * i)));
        }
    }

}
