package com.joe.ssl.crypto;

import com.joe.ssl.crypto.exception.CryptoException;
import com.joe.ssl.crypto.exception.NoSuchAlgorithmException;
import com.joe.utils.common.Assert;
import com.joe.utils.reflect.clazz.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 全局算法注册器
 *
 * @author JoeKerouac
 * @version 2020年07月23日 16:09
 */
public class AlgorithmRegistry {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AlgorithmRegistry.class);

    private static final ConcurrentMap<ClassLoader, ConcurrentMap<String, AlgorithmSpi>> REGISTRY = new ConcurrentHashMap<>();

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
     * 加载指定ClassLoader下所有算法实现
     *
     * @param cl ClassLoader
     */
    private static void load(ClassLoader cl) {
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
        load(loader);
        try {
            List<AlgorithmSpi> list = find(loader,
                algorithmSpi -> algorithmSpi.name().equals(algorithmName));
            if (list.isEmpty()) {
                throw new NoSuchAlgorithmException(algorithmName);
            }

            return (T) list.get(0).clone();
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

        // 先加载
        load(loader);

        List<AlgorithmSpi> list = find(loader, algorithmSpi -> true);
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

        // 先加载
        load(loader);

        List<AlgorithmSpi> list = find(loader,
                algorithmSpi -> algorithmClass.isAssignableFrom(algorithmSpi.getClass()));
        return list.stream().map(AlgorithmSpi::name).collect(Collectors.toSet());
    }

    /**
     * 查找指定算法
     *
     * @param loader 指定ClassLoader
     * @param filter 过滤器，过滤器返回true
     * @return 对应的算法列表，会返回过滤器返回true的算法
     */
    private static List<AlgorithmSpi> find(@NotNull ClassLoader loader,
                                           @NotNull Predicate<AlgorithmSpi> filter) {
        ConcurrentMap<String, AlgorithmSpi> map = REGISTRY.get(loader);

        if (map == null || map.isEmpty()) {
            return Collections.emptyList();
        }

        return map.values().stream().filter(filter).collect(Collectors.toList());
    }

}
