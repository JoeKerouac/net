package com.joe.tls.crypto;

import com.joe.tls.ECDHKeyPair;

/**
 * ECDH密钥交换算法
 * 
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-10-31 14:22
 */
public interface ECDHKeyExchangeSpi {

    /**
     * 使用指定公私钥完成密钥交换密钥交换
     * 
     * @param publicKey
     *            公钥
     * @param privateKey
     *            私钥
     * @param curveId
     *            curveId
     * @return 密钥交换结果
     */
    byte[] keyExchange(byte[] publicKey, byte[] privateKey, int curveId);

    /**
     * 生成一个DH公私钥对
     * 
     * @param curveId
     *            curveId
     * @return 公私钥对
     */
    ECDHKeyPair generate(int curveId);

}
