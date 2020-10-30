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
     * 追加更新加密内容
     * @param data 追加更新的加密内容
     */
    void update(byte[] data);

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
