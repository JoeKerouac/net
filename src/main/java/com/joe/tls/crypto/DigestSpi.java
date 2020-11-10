package com.joe.tls.crypto;

/**
 * 消息摘要SPI
 *
 * <p>
 *     非线程安全
 * </p>
 * @author JoeKerouac
 * @version 2020年07月23日 14:46
 */
public interface DigestSpi extends AlgorithmSpi<DigestSpi> {

    /**
     * 更新数据
     *
     * @param data 源数据
     */
    default void update(byte[] data) {
        update(data, 0, data.length);
    }

    /**
     * 更新数据
     *
     * @param data 源数据
     * @param offset 起始位置
     * @param len 长度
     */
    void update(byte[] data, int offset, int len);

    /**
     * 更新数据
     *
     * @param data 源数据
     */
    void update(byte data);

    /**
     * 对所有源数据进行摘要
     * @return 摘要结果
     */
    byte[] digest();

    /**
     * 将当前内存中数据的摘要输出到指定数组，输出数组offset后必须有足够的空间存放摘要
     *
     * @param output 输出数组
     * @param offset 输出起始位置，会从输出数组的该位置将摘要输出到输出数组
     */
    void digest(byte[] output, int offset);

    /**
     * 对指定数据生成摘要
     * @param data 数据
     * @return 摘要
     */
    default byte[] digest(byte[] data) {
        update(data);
        return digest();
    }

    /**
     * 重置摘要，重新生成摘要
     */
    void reset();

    @Override
    default int type() {
        return AlgorithmSpi.DIGEST;
    }

    static DigestSpi getInstance(String algorithm) {
        return AlgorithmRegistry.newInstance("alias.digest." + algorithm);
    }
}