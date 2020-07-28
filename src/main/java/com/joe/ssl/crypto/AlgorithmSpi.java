package com.joe.ssl.crypto;

/**
 * 算法接口，所有算法都继承自该接口
 * 
 * @author JoeKerouac
 * @version 2020年07月23日 16:00
 */
public interface AlgorithmSpi extends Cloneable {

    /**
     * 摘要算法类型
     */
    int DIGEST = 0;

    /**
     * 加密算法类型
     */
    int CIPHER = 1;

    /**
     * Hmac算法类型
     */
    int HMAC   = 2;

    /**
     * Phash算法类型
     */
    int PHASH  = 3;

    /**
     * 算法名
     * @return 算法名
     */
    String name();

    /**
     * 算法类型
     * @return 算法类型
     */
    int type();

    /**
     * 克隆，默认不支持，请自行实现
     * @return 克隆结果
     * @throws CloneNotSupportedException 异常
     */
    default Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("不支持clone操作");
    }
}
