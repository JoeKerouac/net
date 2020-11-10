package com.joe.tls.crypto;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.joe.tls.cipher.CipherSuite;
import com.joe.tls.crypto.exception.CryptoException;
import com.joe.tls.crypto.exception.NoSuchAlgorithmException;
import com.joe.tls.crypto.impl.*;
import com.joe.utils.common.Assert;

/**
 * 全局算法注册器
 *
 * @author JoeKerouac
 * @version 2020年07月23日 16:09
 */
public class AlgorithmRegistry {

    private static final ConcurrentMap<String, AlgorithmSpi<?>> REGISTRY = new ConcurrentHashMap<>();

    static {
        {
            REGISTRY.put("alias.digest.md5", new DigestMD5());
            REGISTRY.put("alias.digest.MD5", new DigestMD5());

            REGISTRY.put("alias.digest.sha1", new DigestSHA1());
            REGISTRY.put("alias.digest.SHA1", new DigestSHA1());
            REGISTRY.put("alias.digest.SHA-1", new DigestSHA1());

            REGISTRY.put("alias.digest.sha256", new DigestSHA256());
            REGISTRY.put("alias.digest.SHA256", new DigestSHA256());
            REGISTRY.put("alias.digest.SHA-256", new DigestSHA256());

            REGISTRY.put("alias.digest.sha384", new DigestSHA384());
            REGISTRY.put("alias.digest.SHA384", new DigestSHA384());
            REGISTRY.put("alias.digest.SHA-384", new DigestSHA384());
        }

        {
            REGISTRY.put("alias.hmac.md5", new HmacMD5());
            REGISTRY.put("alias.hmac.MD5", new HmacMD5());
            REGISTRY.put("alias.hmac.hmacMd5", new HmacMD5());
            REGISTRY.put("alias.hmac.hmacmd5", new HmacMD5());
            REGISTRY.put("alias.hmac.HmacMD5", new HmacMD5());

            REGISTRY.put("alias.hmac.sha1", new HmacSHA1());
            REGISTRY.put("alias.hmac.sha-1", new HmacSHA1());
            REGISTRY.put("alias.hmac.SHA1", new HmacSHA1());
            REGISTRY.put("alias.hmac.SHA-1", new HmacSHA1());
            REGISTRY.put("alias.hmac.hmacSHA1", new HmacSHA1());
            REGISTRY.put("alias.hmac.HmacSHA1", new HmacSHA1());
            REGISTRY.put("alias.hmac.hmac-sha1", new HmacSHA1());
            REGISTRY.put("alias.hmac.Hmac-SHA1", new HmacSHA1());

            REGISTRY.put("alias.hmac.sha256", new HmacSHA256());
            REGISTRY.put("alias.hmac.sha-256", new HmacSHA256());
            REGISTRY.put("alias.hmac.SHA256", new HmacSHA256());
            REGISTRY.put("alias.hmac.SHA-256", new HmacSHA256());
            REGISTRY.put("alias.hmac.hmacSHA256", new HmacSHA256());
            REGISTRY.put("alias.hmac.HmacSHA256", new HmacSHA256());
            REGISTRY.put("alias.hmac.hmac-sha256", new HmacSHA256());
            REGISTRY.put("alias.hmac.Hmac-SHA256", new HmacSHA256());

            REGISTRY.put("alias.hmac.sha384", new HmacSHA384());
            REGISTRY.put("alias.hmac.sha-384", new HmacSHA384());
            REGISTRY.put("alias.hmac.SHA384", new HmacSHA384());
            REGISTRY.put("alias.hmac.SHA-384", new HmacSHA384());
            REGISTRY.put("alias.hmac.hmacSHA384", new HmacSHA384());
            REGISTRY.put("alias.hmac.HmacSHA384", new HmacSHA384());
            REGISTRY.put("alias.hmac.hmac-sha384", new HmacSHA384());
            REGISTRY.put("alias.hmac.Hmac-SHA384", new HmacSHA384());
        }

        {
            REGISTRY.put("alias.phash.sha1", new PhashSHA1());
            REGISTRY.put("alias.phash.sha-1", new PhashSHA1());
            REGISTRY.put("alias.phash.SHA1", new PhashSHA1());
            REGISTRY.put("alias.phash.SHA-1", new PhashSHA1());
            REGISTRY.put("alias.phash.PhashSHA1", new PhashSHA1());
            REGISTRY.put("alias.phash.phashSHA1", new PhashSHA1());
            REGISTRY.put("alias.phash.Phash-SHA1", new PhashSHA1());
            REGISTRY.put("alias.phash.phash-sha1", new PhashSHA1());

            REGISTRY.put("alias.phash.sha256", new PhashSHA256());
            REGISTRY.put("alias.phash.sha-256", new PhashSHA256());
            REGISTRY.put("alias.phash.SHA256", new PhashSHA256());
            REGISTRY.put("alias.phash.SHA-256", new PhashSHA256());
            REGISTRY.put("alias.phash.PhashSHA256", new PhashSHA256());
            REGISTRY.put("alias.phash.phashSHA256", new PhashSHA256());
            REGISTRY.put("alias.phash.Phash-SHA256", new PhashSHA256());
            REGISTRY.put("alias.phash.phash-sha256", new PhashSHA256());

            REGISTRY.put("alias.phash.sha384", new PhashSHA384());
            REGISTRY.put("alias.phash.sha-384", new PhashSHA384());
            REGISTRY.put("alias.phash.SHA384", new PhashSHA384());
            REGISTRY.put("alias.phash.SHA-384", new PhashSHA384());
            REGISTRY.put("alias.phash.PhashSHA384", new PhashSHA384());
            REGISTRY.put("alias.phash.phashSHA384", new PhashSHA384());
            REGISTRY.put("alias.phash.Phash-SHA384", new PhashSHA384());
            REGISTRY.put("alias.phash.phash-sha384", new PhashSHA384());
        }

        {
            for (CipherSuite.CipherDesc value : CipherSuite.CipherDesc.values()) {
                REGISTRY.put(value.name(), new AesCipher(value));
                REGISTRY.put("alias.cipher." + value.name(), new AesCipher(value));
            }
        }
    }

    /**
     * 查找指定算法
     *
     * @param algorithmName 算法名
     * @param <T>           算法实际类型
     * @return 对应的算法
     * @throws NoSuchAlgorithmException 指定算法不存在时抛出异常
     */
    @SuppressWarnings("unchecked")
    public static <T extends AlgorithmSpi<T>> T newInstance(String algorithmName) throws NoSuchAlgorithmException {
        Assert.notBlank(algorithmName, "algorithmName不能为空");

        // 先加载
        try {
            T algorithmSpi = (T) REGISTRY.get(algorithmName);

            if (algorithmSpi == null) {
                throw new NoSuchAlgorithmException(algorithmName);
            }

            return algorithmSpi.copy();
        } catch (CloneNotSupportedException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * 获取当前所有指定类型的算法实现的名字
     *
     * @return 所有指定类型的算法实现的名字
     */
    public static Set<String> getAllAlgorithm() {
        return REGISTRY.values().stream().map(AlgorithmSpi::name).collect(Collectors.toSet());
    }

}
