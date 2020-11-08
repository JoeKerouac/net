package com.joe.tls.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import com.joe.ssl.cipher.CipherSuite;
import com.joe.ssl.message.TlsVersion;
import com.joe.tls.Authenticator;
import com.joe.tls.CipherBox;
import com.joe.tls.MacAuthenticator;
import com.joe.tls.OutputRecordStream;
import com.joe.tls.msg.Record;
import com.joe.utils.common.Assert;

/**
 * 输出流
 * 
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-08 15:59
 */
public class OutputRecordStreamImpl implements OutputRecordStream {

    /**
     * 实际的网络输出流
     */
    public final OutputStream      netStream;

    /**
     * 对应的版本号
     */
    private final TlsVersion       version;

    /**
     * 加密盒子
     */
    private CipherBox              cipherBox;

    /**
     * 加密套件说明
     */
    private CipherSuite.CipherDesc cipherDesc;

    /**
     * 令牌生成器
     */
    private Authenticator          authenticator;

    public OutputRecordStreamImpl(OutputStream netStream, TlsVersion version) {
        this.netStream = netStream;
        this.version = version;
    }

    @Override
    public void write(Record record) throws IOException {
        netStream.write(record.type().getCode());
        netStream.write(record.version().getMajorVersion());
        netStream.write(record.version().getMinorVersion());
        byte[] data = record.msg().serialize();
        Assert.isTrue(data.length == record.len());

        // 加密数据
        if (cipherBox != null) {
            data = encrypt((byte) record.type().getCode(), data);
        }

        netStream.write(record.len() >> 8);
        // 这里要用data数组的实际长度，因为加密后长度会增加
        netStream.write(data.length);
        netStream.write(data);

        netStream.flush();
    }

    /**
     * 加密数据
     * @param contentType content type
     * @param data 要加密的数据
     * @return 加密后的数据
     */
    private byte[] encrypt(byte contentType, byte[] data) {
        byte[] nonce = cipherBox.createExplicitNonce(authenticator, contentType, data.length);

        if (cipherDesc.getCipherType() == CipherSuite.CipherType.AEAD) {
            byte[] encrypt = cipherBox.encrypt(data, 0, data.length);
            byte[] result = Arrays.copyOf(nonce, nonce.length + encrypt.length);
            System.arraycopy(encrypt, 0, result, nonce.length, encrypt.length);
            return result;
        } else if (cipherDesc.getCipherType() == CipherSuite.CipherType.BLOCK) {
            MacAuthenticator macAuthenticator = (MacAuthenticator) authenticator;
            // BLOCK模式下，将nonce和真实数据拼接，全部加密写出
            byte[] waitEncryptData = Arrays.copyOf(nonce,
                nonce.length + data.length + macAuthenticator.macLen());
            System.arraycopy(data, 0, waitEncryptData, nonce.length, data.length);
            // 计算出mac信息，并放入消息的末尾
            byte[] mac = macAuthenticator.compute((byte) contentType, waitEncryptData, 0,
                nonce.length + data.length, false);
            System.arraycopy(mac, 0, waitEncryptData, nonce.length + data.length, mac.length);

            return cipherBox.encrypt(waitEncryptData, 0, waitEncryptData.length);
        } else {
            throw new RuntimeException("不支持的加密模式：" + cipherDesc);
        }
    }

    @Override
    public void changeCipher(CipherBox cipherBox, Authenticator authenticator) {
        this.cipherBox = cipherBox;
        this.cipherDesc = cipherBox.getCipherDesc();
        this.authenticator = authenticator;
    }
}
