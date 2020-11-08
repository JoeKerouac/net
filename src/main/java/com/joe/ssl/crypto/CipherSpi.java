package com.joe.ssl.crypto;

import java.security.SecureRandom;
import java.util.Arrays;

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
     * 获取加解密模式，{@link #ENCRYPT_MODE}表示加密模式，{@link #DECRYPT_MODE}表示解密
     * @return 加解密模式
     */
    int getMode();

    /**
     * 初始化密钥
     * @param key 加密密钥
     * @param iv IV，目前对于非GCM模式允许为空
     * @param mode 密码器模式，1表示加密模式，2表示解密模式
     */
    default void init(byte[] key, byte[] iv, int mode) {
        init(key, iv, mode, new SecureRandom());
    }

    /**
     * 初始化密钥
     * @param key 加密密钥
     * @param iv IV，目前对于非GCM模式允许为空
     * @param mode 密码器模式，1表示加密模式，2表示解密模式
     * @param secureRandom 安全随机数
     */
    void init(byte[] key, byte[] iv, int mode, SecureRandom secureRandom);

    /**
     * 获取块大小
     * @return 块大小
     */
    int getBlockSize();

    /**
     * 对长度len的数据进行加解密，得到的结果长度，如果是加密模式，那么返回要加密数据长度+tagLen，如果是解密模式，返回要解密数据长度-tagLen
     * @param len 原始数据长度
     * @return 加密后的数据长度
     */
    int getOutputSize(int len);

    /**
     * 认证数据长度，只有GCM模式才会有，GCM模式会在加密数据的最后补充上该长度的认证数据
     * @return 认证数据长度，不是GCM模式返回0，GCM模式返回其对应的认证数据长度，目前应该都是16
     */
    int getTagLen();

    /**
     * 更新附加认证信息（注意，这里和{@link #getTagLen()}）方法中说的认证数据不是一个概念，{@link #getTagLen()}中说的是GCM自带计算的认证
     * 信息，而这个是在加密前人为添加的认证信息
     * @param data 认证信息，该方法必须在{@link #update(byte[])}方法和{@link #doFinal()}之前调用（再加密实际数据前）
     */
    void updateAAD(byte[] data);

    /**
     * 追加更新内容
     * @param data 追加更新的加密内容
     */
    default byte[] update(byte[] data) {
        return update(data, 0, data.length);
    }

    /**
     * 追加更新内容
     * @param data 数据
     * @param offset 起始位置
     * @param len 长度
     */
    default byte[] update(byte[] data, int offset, int len) {
        int outputSize = getOutputSize(len);

        byte[] result = new byte[outputSize];
        int resultLen = update(data, offset, len, result, 0);

        // GCM模式下update返回的resultLen是固定0，在调用doFinal的时候才会将结果一次性输出，因为GCM模式还有一个附加的认证信息需要计算或者校验
        if (resultLen != outputSize) {
            return Arrays.copyOfRange(result, 0, resultLen);
        } else {
            return result;
        }
    }

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
     * @param data 待加密数据
     * @return 加密结果
     */
    default byte[] doFinal(byte[] data) {
        return doFinal(data, 0, data.length);
    }

    /**
     * 最终处理
     * @param data 待加密数据
     * @param offset 起始位置
     * @param len 长度
     * @return 加密结果
     */
    default byte[] doFinal(byte[] data, int offset, int len) {
        byte[] result = new byte[getOutputSize(len)];
        doFinal(data, offset, len, result, 0);
        return result;
    }

    /**
     * 最终加密处理
     * @return AES模式下返回认证数据长度，长度等于{@link #getTagLen() 认证数据长度}
     */
    byte[] doFinal();

    /**
     * 结束加密
     * @param data 待加密数据，不允许为null
     * @param offset 起始位置
     * @param len 长度
     * @param result 存放加密结果的数组，不允许为null
     * @param resultOffset 结果写入存放加密数据数组的起始位置
     * @return 写入result中的数据的长度，也就是加密结果长度
     */
    int doFinal(byte[] data, int offset, int len, byte[] result, int resultOffset);

    @Override
    default int type() {
        return AlgorithmSpi.CIPHER;
    }

    static CipherSpi getInstance(CipherSuite.CipherDesc algorithm) {
        return AlgorithmRegistry.newInstance("alias.cipher." + algorithm.name());
    }
}
