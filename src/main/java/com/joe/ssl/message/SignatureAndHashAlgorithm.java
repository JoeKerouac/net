package com.joe.ssl.message;

import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import lombok.Getter;

/**
 * 签名算法那
 *
 * @author JoeKerouac
 * @version 2020年06月14日 12:26
 */
public class SignatureAndHashAlgorithm {

    /**
     * 所有支持的签名算法枚举
     */
    private static final Map<Integer, SignatureAndHashAlgorithm> ALL_SUPPORTS = new HashMap<>();

    @Getter
    private HashAlgorithm                                        hash;

    @Getter
    private SignatureAlgorithm                                   sign;

    @Getter
    private int                                                  id;

    @Getter
    private String                                               algorithm;

    private SignatureAndHashAlgorithm(HashAlgorithm hash, SignatureAlgorithm sign,
                                      String algorithm) {
        this.hash = hash;
        this.sign = sign;
        this.id = ((hash.value & 0xFF) << 8) | (sign.value & 0xFF);
        this.algorithm = algorithm;
    }

    static {
        try {
            ALL_SUPPORTS.put(
                ((HashAlgorithm.SHA1.value & 0xFF) << 8) | (SignatureAlgorithm.RSA.value & 0xFF),
                new SignatureAndHashAlgorithm(HashAlgorithm.SHA1, SignatureAlgorithm.RSA,
                    "SHA1WithRSA"));
            ALL_SUPPORTS.put(
                ((HashAlgorithm.SHA224.value & 0xFF) << 8) | (SignatureAlgorithm.RSA.value & 0xFF),
                new SignatureAndHashAlgorithm(HashAlgorithm.SHA224, SignatureAlgorithm.RSA,
                    "SHA224WithRSA"));
            ALL_SUPPORTS.put(
                ((HashAlgorithm.SHA256.value & 0xFF) << 8) | (SignatureAlgorithm.RSA.value & 0xFF),
                new SignatureAndHashAlgorithm(HashAlgorithm.SHA256, SignatureAlgorithm.RSA,
                    "SHA256WithRSA"));
            ALL_SUPPORTS.put(
                ((HashAlgorithm.SHA384.value & 0xFF) << 8) | (SignatureAlgorithm.RSA.value & 0xFF),
                new SignatureAndHashAlgorithm(HashAlgorithm.SHA384, SignatureAlgorithm.RSA,
                    "SHA384WithRSA"));
            ALL_SUPPORTS.put(
                ((HashAlgorithm.SHA512.value & 0xFF) << 8) | (SignatureAlgorithm.RSA.value & 0xFF),
                new SignatureAndHashAlgorithm(HashAlgorithm.SHA512, SignatureAlgorithm.RSA,
                    "SHA512WithRSA"));

            ALL_SUPPORTS.put(
                ((HashAlgorithm.SHA1.value & 0xFF) << 8) | (SignatureAlgorithm.DSA.value & 0xFF),
                new SignatureAndHashAlgorithm(HashAlgorithm.SHA1, SignatureAlgorithm.DSA,
                    "SHA1WithDSA"));
            ALL_SUPPORTS.put(
                ((HashAlgorithm.SHA224.value & 0xFF) << 8) | (SignatureAlgorithm.DSA.value & 0xFF),
                new SignatureAndHashAlgorithm(HashAlgorithm.SHA224, SignatureAlgorithm.DSA,
                    "SHA224WithDSA"));
            ALL_SUPPORTS.put(
                ((HashAlgorithm.SHA256.value & 0xFF) << 8) | (SignatureAlgorithm.DSA.value & 0xFF),
                new SignatureAndHashAlgorithm(HashAlgorithm.SHA256, SignatureAlgorithm.DSA,
                    "SHA256WithDSA"));
            ALL_SUPPORTS.put(
                ((HashAlgorithm.SHA384.value & 0xFF) << 8) | (SignatureAlgorithm.DSA.value & 0xFF),
                new SignatureAndHashAlgorithm(HashAlgorithm.SHA384, SignatureAlgorithm.DSA,
                    "SHA384WithDSA"));
            ALL_SUPPORTS.put(
                ((HashAlgorithm.SHA512.value & 0xFF) << 8) | (SignatureAlgorithm.DSA.value & 0xFF),
                new SignatureAndHashAlgorithm(HashAlgorithm.SHA512, SignatureAlgorithm.DSA,
                    "SHA512WithDSA"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据签名算法ID生成签名器
     * @param id 签名算法id
     * @return 签名器
     */
    public static Signature newSignatureAndHash(int id) {
        SignatureAndHashAlgorithm signatureAndHashAlgorithm = ALL_SUPPORTS.get(id);
        if (signatureAndHashAlgorithm == null) {
            throw new RuntimeException(String.format("不支持的签名算法:%d", id));
        }
        try {
            return Signature.getInstance(signatureAndHashAlgorithm.algorithm,
                new BouncyCastleProvider());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<Integer, SignatureAndHashAlgorithm> getAllSupports() {
        return new HashMap<>(ALL_SUPPORTS);
    }

    @Override
    public String toString() {
        return algorithm;
    }

    enum SignatureAlgorithm {
                             UNDEFINED("undefined", -1),

                             ANONYMOUS("anonymous", 0),

                             RSA("rsa", 1),

                             DSA("dsa", 2),

                             ECDSA("ecdsa", 3);

        final String name;
        final int    value;

        SignatureAlgorithm(String name, int value) {
            this.name = name;
            this.value = value;
        }

        static SignatureAlgorithm valueOf(int var0) {
            for (SignatureAlgorithm value : SignatureAlgorithm.values()) {
                if (value.value == var0) {
                    return value;
                }
            }
            return null;
        }
    }

    enum HashAlgorithm {
                        UNDEFINED("undefined", "", -1, -1),

                        NONE("none", "NONE", 0, -1),

                        MD5("md5", "MD5", 1, 16),

                        SHA1("sha1", "SHA-1", 2, 20),

                        SHA224("sha224", "SHA-224", 3, 28),

                        SHA256("sha256", "SHA-256", 4, 32),

                        SHA384("sha384", "SHA-384", 5, 48),

                        SHA512("sha512", "SHA-512", 6, 64);

        final String name;
        final String standardName;
        final int    value;
        final int    length;

        HashAlgorithm(String name, String standardName, int value, int length) {
            this.name = name;
            this.standardName = standardName;
            this.value = value;
            this.length = length;
        }

        static SignatureAndHashAlgorithm.HashAlgorithm valueOf(int var0) {
            for (HashAlgorithm value : HashAlgorithm.values()) {
                if (value.value == var0) {
                    return value;
                }
            }
            return null;
        }
    }
}
