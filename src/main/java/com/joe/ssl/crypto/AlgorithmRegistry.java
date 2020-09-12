package com.joe.ssl.crypto;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joe.ssl.crypto.exception.CryptoException;
import com.joe.ssl.crypto.exception.NoSuchAlgorithmException;
import com.joe.ssl.crypto.impl.*;
import com.joe.utils.common.Assert;
import com.joe.utils.reflect.clazz.ClassUtils;

/**
 * 全局算法注册器
 *
 * @author JoeKerouac
 * @version 2020年07月23日 16:09
 */
public class AlgorithmRegistry {

    private static final Logger                              LOGGER   = LoggerFactory
        .getLogger(AlgorithmRegistry.class);

    private static final ConcurrentMap<String, AlgorithmSpi> REGISTRY = new ConcurrentHashMap<>();

    static {
        {
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
            REGISTRY.put("alias.hmac.hmacSHA1", new HmacSHA1());
            REGISTRY.put("alias.hmac.HmacSHA1", new HmacSHA1());
            REGISTRY.put("alias.hmac.hmac-sha1", new HmacSHA1());
            REGISTRY.put("alias.hmac.Hmac-SHA1", new HmacSHA1());

            REGISTRY.put("alias.hmac.hmacSHA256", new HmacSHA256());
            REGISTRY.put("alias.hmac.HmacSHA256", new HmacSHA256());
            REGISTRY.put("alias.hmac.hmac-sha256", new HmacSHA256());
            REGISTRY.put("alias.hmac.Hmac-SHA256", new HmacSHA256());

            REGISTRY.put("alias.hmac.hmacSHA384", new HmacSHA384());
            REGISTRY.put("alias.hmac.HmacSHA384", new HmacSHA384());
            REGISTRY.put("alias.hmac.hmac-sha384", new HmacSHA384());
            REGISTRY.put("alias.hmac.Hmac-SHA384", new HmacSHA384());
        }

        {
            REGISTRY.put("alias.phash.PhashSHA1", new PhashSHA1());
            REGISTRY.put("alias.phash.phashSHA1", new PhashSHA1());
            REGISTRY.put("alias.phash.Phash-SHA1", new PhashSHA1());
            REGISTRY.put("alias.phash.phash-sha1", new PhashSHA1());

            REGISTRY.put("alias.phash.PhashSHA256", new PhashSHA256());
            REGISTRY.put("alias.phash.phashSHA256", new PhashSHA256());
            REGISTRY.put("alias.phash.Phash-SHA256", new PhashSHA256());
            REGISTRY.put("alias.phash.phash-sha256", new PhashSHA256());

            REGISTRY.put("alias.phash.PhashSHA384", new PhashSHA384());
            REGISTRY.put("alias.phash.phashSHA384", new PhashSHA384());
            REGISTRY.put("alias.phash.Phash-SHA384", new PhashSHA384());
            REGISTRY.put("alias.phash.phash-sha384", new PhashSHA384());
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
    public static <T extends AlgorithmSpi> T newInstance(String algorithmName) throws NoSuchAlgorithmException {
        Assert.notBlank(algorithmName, "algorithmName不能为空");
        return newInstance(ClassUtils.getDefaultClassLoader(), algorithmName);
    }

    /**
     * 获取当前所有加密算法名字
     *
     * @return 所有加密算法名
     */
    public static Set<String> getAllAlgorithm() {
        return getAllAlgorithm(ClassUtils.getDefaultClassLoader());
    }

    /**
     * 获取当前所有指定类型的算法实现的名字
     *
     * @param algorithmClass 指定算法类型
     * @return 所有指定类型的算法实现的名字
     */
    public static Set<String> getAllAlgorithm(Class<? extends AlgorithmSpi> algorithmClass) {
        Assert.notNull(algorithmClass, "algorithmClass不能为null");
        return getAllAlgorithm(ClassUtils.getDefaultClassLoader(), algorithmClass);
    }

    /**
     * 查找指定算法
     *
     * @param loader        指定ClassLoader
     * @param algorithmName 算法名
     * @param <T>           算法实际类型
     * @return 对应的算法
     * @throws NoSuchAlgorithmException 指定算法不存在时抛出异常
     */
    @SuppressWarnings("unchecked")
    public static <T extends AlgorithmSpi> T newInstance(@NotNull ClassLoader loader,
                                                         String algorithmName) throws NoSuchAlgorithmException {
        Assert.notNull(loader, "ClassLoader不能为null");
        Assert.notBlank(algorithmName, "algorithmName不能为空");

        // 先加载
        try {
            List<AlgorithmSpi> list = find(
                algorithmSpi -> algorithmSpi.name().equals(algorithmName));
            if (list.isEmpty()) {
                throw new NoSuchAlgorithmException(algorithmName);
            }

            return (T) list.get(0).copy();
        } catch (CloneNotSupportedException e) {
            throw new CryptoException(e);
        }
    }

    /**
     * 获取当前所有加密算法名字
     *
     * @param loader 指定ClassLoader
     * @return 所有加密算法名
     */
    public static Set<String> getAllAlgorithm(@NotNull ClassLoader loader) {
        Assert.notNull(loader, "ClassLoader不能为null");

        List<AlgorithmSpi> list = find(algorithmSpi -> true);
        return list.stream().map(AlgorithmSpi::name).collect(Collectors.toSet());
    }

    /**
     * 获取当前所有指定类型的算法实现的名字
     *
     * @param loader         指定ClassLoader
     * @param algorithmClass 指定算法类型
     * @return 所有指定类型的算法实现的名字
     */
    public static Set<String> getAllAlgorithm(@NotNull ClassLoader loader,
                                              Class<? extends AlgorithmSpi> algorithmClass) {
        Assert.notNull(loader, "ClassLoader不能为null");
        Assert.notNull(algorithmClass, "algorithmClass不能为null");

        List<AlgorithmSpi> list = find(
            algorithmSpi -> algorithmClass.isAssignableFrom(algorithmSpi.getClass()));
        return list.stream().map(AlgorithmSpi::name).collect(Collectors.toSet());
    }

    /**
     * 查找指定算法
     *
     * @param filter 过滤器，过滤器返回true
     * @return 对应的算法列表，会返回过滤器返回true的算法
     */
    private static List<AlgorithmSpi> find(@NotNull Predicate<AlgorithmSpi> filter) {
        return REGISTRY.values().stream().filter(filter).collect(Collectors.toList());
    }

}
