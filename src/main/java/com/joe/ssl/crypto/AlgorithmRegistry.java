package com.joe.ssl.crypto;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joe.ssl.crypto.exception.NoSuchAlgorithmException;
import com.joe.utils.common.Assert;
import com.joe.utils.reflect.clazz.ClassUtils;

/**
 * 全局算法注册器
 * 
 * @author JoeKerouac
 * @version 2020年07月23日 16:09
 */
public class AlgorithmRegistry {

    private static final Logger                                                          LOGGER   = LoggerFactory
        .getLogger(AlgorithmRegistry.class);

    private static final ConcurrentMap<ClassLoader, ConcurrentMap<String, AlgorithmSpi>> REGISTRY = new ConcurrentHashMap<>();

    /**
     * 查找指定算法
     * @param algorithmName 算法名
     * @param <T> 算法实际类型
     * @return 对应的算法
     * @throws NoSuchAlgorithmException 指定算法不存在时抛出异常
     */
    public static <T extends AlgorithmSpi> T newInstance(String algorithmName) throws NoSuchAlgorithmException {
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
        return getAllAlgorithm(ClassUtils.getDefaultClassLoader(), algorithmClass);
    }

    /**
     * 加载指定ClassLoader下所有算法实现
     * @param cl ClassLoader
     */
    public static void load(ClassLoader cl) {
        REGISTRY.compute(cl, (loader, stringAlgorithmSpiMap) -> {
            if (stringAlgorithmSpiMap == null) {
                ServiceLoader<AlgorithmSpi> serviceLoader = ServiceLoader.load(AlgorithmSpi.class,
                    loader);
                ConcurrentHashMap<String, AlgorithmSpi> map = new ConcurrentHashMap<>();

                for (AlgorithmSpi algorithm : serviceLoader) {
                    if (map.containsKey(algorithm.name())) {
                        LOGGER.warn("ClassLoader[{}]下算法名[{}]重复，当前系统实现[{}]，实现[{}]将被忽略", loader,
                            algorithm.name(), map.get(algorithm.name()).getClass(),
                            algorithm.getClass());
                        continue;
                    }

                    map.put(algorithm.name(), algorithm);
                }

                return map;
            } else {
                return stringAlgorithmSpiMap;
            }
        });
    }

    /**
     * 查找指定算法
     * @param loader 指定ClassLoader
     * @param algorithmName 算法名
     * @param <T> 算法实际类型
     * @return 对应的算法
     * @throws NoSuchAlgorithmException 指定算法不存在时抛出异常
     */
    @SuppressWarnings("unchecked")
    public static <T extends AlgorithmSpi> T newInstance(@NotNull ClassLoader loader,
                                                         String algorithmName) throws NoSuchAlgorithmException {
        Assert.notNull(loader, "ClassLoader不能为null");
        // 先加载
        load(loader);
        // 最后加载不到就抛出异常
        return (T) Optional.ofNullable(REGISTRY.get(loader)).map(map -> map.get(algorithmName))
            .orElseThrow(() -> new NoSuchAlgorithmException(algorithmName))1;
    }

    /**
     * 获取当前所有加密算法名字
     * 
     * @param loader 指定ClassLoader
     * @return 所有加密算法名
     */
    public static Set<String> getAllAlgorithm(@NotNull ClassLoader loader) {
        Assert.notNull(loader, "ClassLoader不能为null");
        // 先加载
        load(loader);
        return Optional.ofNullable(REGISTRY.get(loader)).map(Map::keySet).map(HashSet::new)
            .orElseGet(HashSet::new);
    }

    /**
     * 获取当前所有指定类型的算法实现的名字
     * 
     * @param loader 指定ClassLoader
     * @param algorithmClass 指定算法类型
     * @return 所有指定类型的算法实现的名字
     */
    public static Set<String> getAllAlgorithm(@NotNull ClassLoader loader,
                                              Class<? extends AlgorithmSpi> algorithmClass) {
        Assert.notNull(loader, "ClassLoader不能为null");
        // 先加载
        load(loader);
        Set<String> set = new HashSet<>();

        Optional.ofNullable(REGISTRY.get(loader)).orElseGet(ConcurrentHashMap::new)
            .forEach((k, v) -> {
                if (algorithmClass.isAssignableFrom(v.getClass())) {
                    set.add(k);
                }
            });

        return set;
    }

}
