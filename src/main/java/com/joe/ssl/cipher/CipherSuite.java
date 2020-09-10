package com.joe.ssl.cipher;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.joe.ssl.crypto.exception.CryptoException;
import com.joe.ssl.message.WrapedOutputStream;
import com.joe.utils.collection.CollectionUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * int类型的静态字段的字段名必须是加密套件名，值是对应的id
 * <p>
 * 密码套件说明：TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384，
 * <li>其中TLS是固定的</li>
 * <li>ECDH_RSA表示密钥交换使用ECDH算法， 使用RSA算法做签名</li>
 * <li>AES_256_GCM表示使用AES算法，密钥256位，GCM是AES的加密模式</li>
 * <li>SHA384表示MAC算法使用的hash算法</li>
 *
 * @author JoeKerouac
 * @version 2020年06月30日 20:10
 */
public class CipherSuite {

    private static final int TLS_NULL_WITH_NULL_NULL = 0x0000;
    private static final int TLS_RSA_WITH_NULL_MD5 = 0x0001;
    private static final int TLS_RSA_WITH_NULL_SHA = 0x0002;
    private static final int TLS_RSA_EXPORT_WITH_RC4_40_MD5 = 0x0003;
    private static final int TLS_RSA_WITH_RC4_128_MD5 = 0x0004;
    private static final int TLS_RSA_WITH_RC4_128_SHA = 0x0005;
    private static final int TLS_RSA_EXPORT_WITH_RC2_CBC_40_MD5 = 0x0006;
    private static final int TLS_RSA_WITH_IDEA_CBC_SHA = 0x0007;
    private static final int TLS_RSA_EXPORT_WITH_DES40_CBC_SHA = 0x0008;
    private static final int TLS_RSA_WITH_DES_CBC_SHA = 0x0009;
    private static final int TLS_RSA_WITH_3DES_EDE_CBC_SHA = 0x000A;
    private static final int TLS_DH_DSS_EXPORT_WITH_DES40_CBC_SHA = 0x000B;
    private static final int TLS_DH_DSS_WITH_DES_CBC_SHA = 0x000C;
    private static final int TLS_DH_DSS_WITH_3DES_EDE_CBC_SHA = 0x000D;
    private static final int TLS_DH_RSA_EXPORT_WITH_DES40_CBC_SHA = 0x000E;
    private static final int TLS_DH_RSA_WITH_DES_CBC_SHA = 0x000F;
    private static final int TLS_DH_RSA_WITH_3DES_EDE_CBC_SHA = 0x0010;
    private static final int TLS_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA = 0x0011;
    private static final int TLS_DHE_DSS_WITH_DES_CBC_SHA = 0x0012;
    private static final int TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA = 0x0013;
    private static final int TLS_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA = 0x0014;
    private static final int TLS_DHE_RSA_WITH_DES_CBC_SHA = 0x0015;
    private static final int TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA = 0x0016;
    private static final int TLS_DH_anon_EXPORT_WITH_RC4_40_MD5 = 0x0017;
    private static final int TLS_DH_anon_WITH_RC4_128_MD5 = 0x0018;
    private static final int TLS_DH_anon_EXPORT_WITH_DES40_CBC_SHA = 0x0019;
    private static final int TLS_DH_anon_WITH_DES_CBC_SHA = 0x001A;
    private static final int TLS_DH_anon_WITH_3DES_EDE_CBC_SHA = 0x001B;

    /*
     * RFC 3268
     */
    public static final int TLS_RSA_WITH_AES_128_CBC_SHA = 0x002F;
    public static final int TLS_DH_DSS_WITH_AES_128_CBC_SHA = 0x0030;
    public static final int TLS_DH_RSA_WITH_AES_128_CBC_SHA = 0x0031;
    public static final int TLS_DHE_DSS_WITH_AES_128_CBC_SHA = 0x0032;
    public static final int TLS_DHE_RSA_WITH_AES_128_CBC_SHA = 0x0033;
    public static final int TLS_DH_anon_WITH_AES_128_CBC_SHA = 0x0034;
    public static final int TLS_RSA_WITH_AES_256_CBC_SHA = 0x0035;
    public static final int TLS_DH_DSS_WITH_AES_256_CBC_SHA = 0x0036;
    public static final int TLS_DH_RSA_WITH_AES_256_CBC_SHA = 0x0037;
    public static final int TLS_DHE_DSS_WITH_AES_256_CBC_SHA = 0x0038;
    public static final int TLS_DHE_RSA_WITH_AES_256_CBC_SHA = 0x0039;
    public static final int TLS_DH_anon_WITH_AES_256_CBC_SHA = 0x003A;

    /*
     * RFC 4279
     */
    public static final int TLS_PSK_WITH_RC4_128_SHA = 0x008A;
    public static final int TLS_PSK_WITH_3DES_EDE_CBC_SHA = 0x008B;
    public static final int TLS_PSK_WITH_AES_128_CBC_SHA = 0x008C;
    public static final int TLS_PSK_WITH_AES_256_CBC_SHA = 0x008D;
    public static final int TLS_DHE_PSK_WITH_RC4_128_SHA = 0x008E;
    public static final int TLS_DHE_PSK_WITH_3DES_EDE_CBC_SHA = 0x008F;
    public static final int TLS_DHE_PSK_WITH_AES_128_CBC_SHA = 0x0090;
    public static final int TLS_DHE_PSK_WITH_AES_256_CBC_SHA = 0x0091;
    public static final int TLS_RSA_PSK_WITH_RC4_128_SHA = 0x0092;
    public static final int TLS_RSA_PSK_WITH_3DES_EDE_CBC_SHA = 0x0093;
    public static final int TLS_RSA_PSK_WITH_AES_128_CBC_SHA = 0x0094;
    public static final int TLS_RSA_PSK_WITH_AES_256_CBC_SHA = 0x0095;

    /*
     * RFC 4492
     */
    public static final int TLS_ECDH_ECDSA_WITH_NULL_SHA = 0xC001;
    public static final int TLS_ECDH_ECDSA_WITH_RC4_128_SHA = 0xC002;
    public static final int TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA = 0xC003;
    public static final int TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA = 0xC004;
    public static final int TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA = 0xC005;
    public static final int TLS_ECDHE_ECDSA_WITH_NULL_SHA = 0xC006;
    public static final int TLS_ECDHE_ECDSA_WITH_RC4_128_SHA = 0xC007;
    public static final int TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA = 0xC008;
    public static final int TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA = 0xC009;
    public static final int TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA = 0xC00A;
    public static final int TLS_ECDH_RSA_WITH_NULL_SHA = 0xC00B;
    public static final int TLS_ECDH_RSA_WITH_RC4_128_SHA = 0xC00C;
    public static final int TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA = 0xC00D;
    public static final int TLS_ECDH_RSA_WITH_AES_128_CBC_SHA = 0xC00E;
    public static final int TLS_ECDH_RSA_WITH_AES_256_CBC_SHA = 0xC00F;
    public static final int TLS_ECDHE_RSA_WITH_NULL_SHA = 0xC010;
    public static final int TLS_ECDHE_RSA_WITH_RC4_128_SHA = 0xC011;
    public static final int TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA = 0xC012;
    public static final int TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA = 0xC013;
    public static final int TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA = 0xC014;
    public static final int TLS_ECDH_anon_WITH_NULL_SHA = 0xC015;
    public static final int TLS_ECDH_anon_WITH_RC4_128_SHA = 0xC016;
    public static final int TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA = 0xC017;
    public static final int TLS_ECDH_anon_WITH_AES_128_CBC_SHA = 0xC018;
    public static final int TLS_ECDH_anon_WITH_AES_256_CBC_SHA = 0xC019;

    /*
     * RFC 5054
     */
    public static final int TLS_SRP_SHA_WITH_3DES_EDE_CBC_SHA = 0xC01A;
    public static final int TLS_SRP_SHA_RSA_WITH_3DES_EDE_CBC_SHA = 0xC01B;
    public static final int TLS_SRP_SHA_DSS_WITH_3DES_EDE_CBC_SHA = 0xC01C;
    public static final int TLS_SRP_SHA_WITH_AES_128_CBC_SHA = 0xC01D;
    public static final int TLS_SRP_SHA_RSA_WITH_AES_128_CBC_SHA = 0xC01E;
    public static final int TLS_SRP_SHA_DSS_WITH_AES_128_CBC_SHA = 0xC01F;
    public static final int TLS_SRP_SHA_WITH_AES_256_CBC_SHA = 0xC020;
    public static final int TLS_SRP_SHA_RSA_WITH_AES_256_CBC_SHA = 0xC021;
    public static final int TLS_SRP_SHA_DSS_WITH_AES_256_CBC_SHA = 0xC022;

    /*
     * RFC 5289
     */
    private static final int TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256 = 0xC023;
    private static final int TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384 = 0xC024;
    private static final int TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256 = 0xC025;
    private static final int TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384 = 0xC026;
    private static final int TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256 = 0xC027;
    private static final int TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384 = 0xC028;
    private static final int TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256 = 0xC029;
    private static final int TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384 = 0xC02A;
    private static final int TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256 = 0xC02B;
    private static final int TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384 = 0xC02C;
    private static final int TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256 = 0xC02D;
    private static final int TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384 = 0xC02E;
    private static final int TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256 = 0xC02F;
    private static final int TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384 = 0xC030;
    private static final int TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256 = 0xC031;
    private static final int TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384 = 0xC032;

    /*
     * RFC 5746
     */
    public static final int TLS_EMPTY_RENEGOTIATION_INFO_SCSV = 0x00FF;

    /**
     * 名字到id的映射
     */
    private static final Map<String, Integer> NAME2ID = new HashMap<>();

    /**
     * id到名字的映射
     */
    private static final Map<Integer, String> ID2NAME = new HashMap<>();

    static {
        try {
            Field[] fields = CipherSuite.class.getFields();
            for (Field field : fields) {
                // 只用公共、静态并且是int类型的字段
                if (Modifier.isPublic(field.getModifiers())
                        && Modifier.isStatic(field.getModifiers()) && field.getType() == int.class) {
                    String name = field.getName();
                    int id = field.getInt(CipherSuite.class);
                    NAME2ID.put(name, id);
                    ID2NAME.put(id, name);
                }
            }
        } catch (IllegalArgumentException | IllegalAccessException exception) {
            throw new CryptoException(exception);
        }
    }

    public static final List<CipherSuite> CIPHER_SUITES = new ArrayList<>();


    static {
        CIPHER_SUITES.add(new CipherSuite(TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384, "SHA384", KeyExchange.ECDH_RSA, CipherDesc.AES_256_GCM, true));
    }

    /**
     * 加密套件id
     */
    private int suite;

    /**
     * mac算法（对应的hash算法）
     */
    private String macAlg;

    /**
     * 密钥交换算法名
     */
    private KeyExchange keyExchange;

    /**
     * 加密算法名
     */
    private CipherDesc cipher;

    /**
     * 是否是包含ECC算法
     */
    private boolean ec;

    public CipherSuite(int suite, String macAlg, KeyExchange keyExchange, CipherDesc cipher, boolean ec) {
        this.suite = suite;
        this.macAlg = macAlg;
        this.keyExchange = keyExchange;
        this.cipher = cipher;
        this.ec = ec;
    }

    public int getSuite() {
        return suite;
    }

    public void setSuite(int suite) {
        this.suite = suite;
    }

    public String getMacAlg() {
        return macAlg;
    }

    public void setMacAlg(String macAlg) {
        this.macAlg = macAlg;
    }

    public KeyExchange getKeyExchange() {
        return keyExchange;
    }

    public void setKeyExchange(KeyExchange keyExchange) {
        this.keyExchange = keyExchange;
    }

    public CipherDesc getCipher() {
        return cipher;
    }

    public void setCipher(CipherDesc cipher) {
        this.cipher = cipher;
    }

    public boolean isEc() {
        return ec;
    }

    public void setEc(boolean ec) {
        this.ec = ec;
    }

    /**
     * 密钥交换算法
     */
    private enum KeyExchange {
        ECDH_RSA,
    }

    /**
     * 加密类型
     */
    public enum CipherType {
        BLOCK, AEAD;
    }



    /**
     * 加密算法
     */
    public enum CipherDesc {

        AES_128("AES/CBC/NoPadding", CipherType.BLOCK, 16, 16, 0),

        AES_128_GCM("AES/GCM/NoPadding", CipherType.AEAD, 16, 12, 4),

        AES_256("AES/CBC/NoPadding", CipherType.BLOCK, 32, 16, 0),

        AES_256_GCM("AES/GCM/NoPadding", CipherType.AEAD, 32, 12, 4),

        ;

        /**
         * 加密算法名
         */
        private String cipherName;

        /**
         * 密钥大小，单位byte
         */
        private int keySize;

        /**
         * iv大小，单位byte
         */
        private int ivLen;

        /**
         * GCM模式下会大于0，表示实际的ivLen，因为对于GCM模式来说iv等于fixedIv+nonce
         */
        private int fixedIvLen;

        /**
         * 加密类型
         */
        private CipherType cipherType;

        CipherDesc(String cipherName, CipherType cipherType, int keySize, int ivLen, int fixedIvLen) {
            this.cipherName = cipherName;
            this.cipherType = cipherType;
            this.keySize = keySize;
            this.ivLen = ivLen;
            this.fixedIvLen = fixedIvLen;
        }

        public String getCipherName() {
            return cipherName;
        }

        public void setCipherName(String cipherName) {
            this.cipherName = cipherName;
        }

        public int getKeySize() {
            return keySize;
        }

        public void setKeySize(int keySize) {
            this.keySize = keySize;
        }

        public int getIvLen() {
            return ivLen;
        }

        public void setIvLen(int ivLen) {
            this.ivLen = ivLen;
        }

        public int getFixedIvLen() {
            return fixedIvLen;
        }

        public void setFixedIvLen(int fixedIvLen) {
            this.fixedIvLen = fixedIvLen;
        }

        public CipherType getCipherType() {
            return cipherType;
        }

        public void setCipherType(CipherType cipherType) {
            this.cipherType = cipherType;
        }
    }
}
