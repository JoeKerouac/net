package com.joe.tls;

import java.io.IOException;

import com.joe.tls.msg.Record;

/**
 * 写出Record
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-02 10:32
 */
public interface OutputRecordStream {

    /**
     * 写出一个Record
     * @param record 要写出的Record
     * @throws IOException IO异常
     */
    void write(Record record) throws IOException;

    /**
     * 更改cipher，在密钥交换完毕后调用
     * @param cipherBox 加密盒子
     * @param authenticator 令牌生成器，对于非AEAD模式，需要传入{@link MacAuthenticator}实例，AEAD模式需要传
     *                      入{@link Authenticator}实例
     */
    void changeCipher(CipherBox cipherBox, Authenticator authenticator);

}
