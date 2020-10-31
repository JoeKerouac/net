package com.joe.ssl.example;

import lombok.Data;

/**
 * DH密钥对
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-10-31 14:24
 */
@Data
public class ECDHKeyPair {

    /**
     * 私钥
     */
    private byte[] privateKey;

    /**
     * 公钥
     */
    private byte[] publicKey;
}
