package com.joe.ssl.crypto;

import com.joe.ssl.cipher.CipherSuite;

/**
 * 加密接口
 *
 * @author JoeKerouac
 * @version 2020年07月23日 17:23
 */
public interface CipherSpi extends AlgorithmSpi<CipherSpi> {

    /**
     * 加密模式
     */
    int ENCRYPT_MODE = 1;

    /**
     * 解密模式
     */
    int DECRYPT_MODE = 2;

    int WRAP_MODE    = 3;

    int UNWRAP_MODE  = 4;

    /**
     * 初始化密钥
     * @param key 加密密钥
     * @param mode 密码器模式，1表示加密模式，2表示解密模式
     */
    void init(byte[] key, byte[] iv, int mode);

    /**
     * 获取块大小
     * @return 块大小
     */
    int getBlockSize();

    /**
     * 追加更新加密内容
     * @param data 追加更新的加密内容
     */
    void update(byte[] data);

    /**
     * 追加更新加密内容
     * @param data 数据
     * @param offset 起始位置
     * @param len 长度
     */
    void update(byte[] data, int offset, int len);

    /**
     * 更新认证信息
     * @param data 认证信息，该方法必须在{@link #update(byte[])}方法和{@link #doFinal()}之前调用（再加密实际数据前）
     */
    void updateAAD(byte[] data);

    /**
     * 最终加密处理
     * @param data 待加密数据
     * @return 加密结果
     */
    byte[] doFinal(byte[] data);

    /**
     * 最终加密处理
     * @param data 待加密数据
     * @param offset 起始位置
     * @param len 长度
     * @return 加密结果
     */
    byte[] doFinal(byte[] data, int offset, int len);

    /**
     * 结束加密
     * @param data 待加密数据
     * @param offset 起始位置
     * @param len 长度
     * @param result 存放加密结果的数组
     * @param resultOffset 结果写入存放加密数据数组的起始位置
     * @return 写入result中的数据的长度，也就是加密结果长度
     */
    int doFinal(byte[] data, int offset, int len, byte[] result, int resultOffset);

    /**
     * 追加更新
     * @param data 待加密数据
     * @param offset 起始位置
     * @param len 长度
     * @param result 存放加密结果的数组
     * @param resultOffset 结果写入存放加密数据数组的起始位置
     * @return 写入result中的数据的长度，也就是加密结果长度
     */
    int update(byte[] data, int offset, int len, byte[] result, int resultOffset);

    /**
     * 最终加密处理
     * @return 加密结果
     */
    byte[] doFinal();

    @Override
    default int type() {
        return AlgorithmSpi.CIPHER;
    }

    static CipherSpi getInstance(CipherSuite.CipherDesc algorithm) {
        return AlgorithmRegistry.newInstance("alias.cipher." + algorithm.name());
    }
}
