package com.joe.tls.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.joe.tls.*;
import com.joe.tls.cipher.CipherSuite;
import com.joe.tls.enums.ContentType;
import com.joe.tls.enums.HandshakeType;
import com.joe.tls.msg.HandshakeProtocol;
import com.joe.tls.msg.Record;
import com.joe.tls.msg.impl.ChangeCipherSpace;
import com.joe.tls.msg.reader.HandshakeMsgReaderUtil;

/**
 * record读取
 * 
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-08 16:49
 */
public class InputRecordStreamImpl implements InputRecordStream {

    /**
     * 实际的网络输出流
     */
    public final InputStream       netStream;

    /**
     * 对应的版本号
     */
    private final TlsVersion       version;

    /**
     * 摘要记录器
     */
    private final HandshakeHash    handshakeHash;

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

    public InputRecordStreamImpl(InputStream netStream, HandshakeHash handshakeHash,
                                 TlsVersion version) {
        this.netStream = netStream;
        this.handshakeHash = handshakeHash;
        this.version = version;
    }

    @Override
    public Record read() throws IOException {
        byte[] header = read(5);
        // 先读取header
        ContentType contentType = EnumInterface.getByCode(header[0], ContentType.class);
        if (contentType == null) {
            throw new RuntimeException("不支持的content type:" + header[0]);
        }

        TlsVersion version = TlsVersion.valueOf(header[1], header[2]);
        if (version == null) {
            throw new RuntimeException(String
                .format("不支持的version，major version: %d, minor version: %d", header[1], header[2]));
        }

        // 长度
        int contentLen = Byte.toUnsignedInt(header[3]) << 8 | Byte.toUnsignedInt(header[4]);
        byte[] content = read(contentLen);

        if (cipherBox != null && contentType != ContentType.ALTER
            && contentType != ContentType.CHANGE_CIPHER_SPEC) {
            content = decrypt(header[0], content);
        }

        switch (contentType) {
            case HANDSHAKE:
                // finished消息不进行hash
                if (content[0] != HandshakeType.FINISHED.getCode()) {
                    handshakeHash.update(content, 0, content.length);
                }

                HandshakeProtocol protocol = HandshakeMsgReaderUtil.read(content);
                return new Record(contentType, version, protocol, content);
            case CHANGE_CIPHER_SPEC:
                return new Record(contentType, version, new ChangeCipherSpace(), content);
            case ALTER:
                throw new RuntimeException("收到了alter信息：" + Arrays.toString(content));
            case APPLICATION_DATA:
            default:
                throw new RuntimeException("暂不支持的contentType:" + header[0]);
        }
    }

    /**
     * 解密数据，同时校验mac
     * @param contentType content type
     * @param data 要解密的数据
     * @return 解密后的数据，没有nonce和mac
     */
    private byte[] decrypt(byte contentType, byte[] data) {
        int nonceLen = cipherBox.applyExplicitNonce(authenticator, contentType, data);
        int tagLen = cipherBox.getTagSize();

        if (cipherDesc.getCipherType() == CipherSuite.CipherType.AEAD) {
            // AEAD模式下IV是明文的，所以不需要加密
            return cipherBox.decrypt(data, nonceLen, data.length - nonceLen);
        } else if (cipherDesc.getCipherType() == CipherSuite.CipherType.BLOCK) {
            byte[] decryptResult = cipherBox.decrypt(data, 0, data.length);
            MacAuthenticator macAuthenticator = (MacAuthenticator) authenticator;
            int macLen = macAuthenticator.macLen();

            byte[] mac = macAuthenticator.compute(contentType, decryptResult, nonceLen,
                decryptResult.length - nonceLen - macLen, true);

            if (Arrays.equals(mac, Arrays.copyOfRange(decryptResult, decryptResult.length - macLen,
                decryptResult.length))) {
                throw new RuntimeException("bad record MAC");
            }

            return Arrays.copyOfRange(decryptResult, nonceLen, decryptResult.length - macLen);
        } else {
            throw new RuntimeException("不支持的加密模式：" + cipherDesc);
        }
    }

    /**
     * 从网络流中读取指定长度的数据
     * @param len 要读取的长度
     * @return 指定长度的数据
     * @throws IOException IO异常
     */
    private byte[] read(int len) throws IOException {
        byte[] buffer = new byte[len];
        int offset = 0;
        while (offset < len) {
            offset += netStream.read(buffer, offset, buffer.length - offset);
        }
        return buffer;
    }

    @Override
    public void changeCipher(CipherBox cipherBox, Authenticator authenticator) {
        this.cipherBox = cipherBox;
        this.cipherDesc = cipherBox.getCipherDesc();
        this.authenticator = authenticator;
    }
}
