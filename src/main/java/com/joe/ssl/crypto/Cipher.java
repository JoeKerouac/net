package com.joe.ssl.crypto;

/**
 * 加密接口
 *
 * @author JoeKerouac
 * @version 2020年07月23日 17:23
 */
public interface Cipher extends AlgorithmSpi {

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
     * @param key 密钥
     * @param mode 密码器模式
     */
    void init(byte[] key, int mode);

    /**
     * 追加更新加密内容
     * @param data 追加更新的加密内容
     */
    void update(byte[] data);

    /**
     * 最终加密处理
     * @return 加密结果
     */
    byte[] doFinal();
}
