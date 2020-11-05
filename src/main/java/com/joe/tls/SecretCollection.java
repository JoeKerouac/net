package com.joe.tls;

import lombok.Data;

/**
 * 各种密钥集合
 * 
 * @author JoeKerouac
 * @data 2020-11-05 23:27
 */
@Data
public class SecretCollection {

    /**
     * preMaster key
     */
    private byte[] preMasterKey;

    /**
     * master key
     */
    private byte[] masterKey;

    /**
     * 客户端写出key
     */
    private byte[] clientWriteKey;

    /**
     * 服务端写出key
     */
    private byte[] serverWriteKey;

    /**
     * 客户端iv
     */
    private byte[] clientWriteIv;

    /**
     * 服务端iv
     */
    private byte[] serverWriteIv;

    /**
     * 客户端mac
     */
    private byte[] clientMac;

    /**
     * 服务端mac
     */
    private byte[] serverMac;

}
