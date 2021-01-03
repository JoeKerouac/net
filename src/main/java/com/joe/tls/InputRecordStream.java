package com.joe.tls;

import java.io.IOException;

import com.joe.tls.msg.Record;

/**
 * 读取RecordMessage
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-02 10:32
 */
public interface InputRecordStream {

    /**
     * 读取一个RecordMessage
     * 
     * @return RecordMessage
     * @throws IOException
     *             IO异常
     */
    Record read() throws IOException;

    /**
     * 更改cipher，在密钥交换完毕后调用
     * 
     * @param cipherBox
     *            加密盒子
     * @param authenticator
     *            令牌生成器，对于非AEAD模式，需要传入{@link MacAuthenticator}实例，AEAD模式需要传 入{@link Authenticator}实例
     */
    void changeCipher(CipherBox cipherBox, Authenticator authenticator);

}
