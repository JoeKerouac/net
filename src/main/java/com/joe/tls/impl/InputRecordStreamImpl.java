package com.joe.tls.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.joe.ssl.cipher.CipherSuite;
import com.joe.ssl.message.EnumInterface;
import com.joe.ssl.message.TlsVersion;
import com.joe.tls.Authenticator;
import com.joe.tls.CipherBox;
import com.joe.tls.InputRecordStream;
import com.joe.tls.enums.ContentType;
import com.joe.tls.msg.HandshakeMsgReaderUtil;
import com.joe.tls.msg.HandshakeProtocol;
import com.joe.tls.msg.Record;

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
     * 缓冲区
     */
    private final ByteBuffer       headerBuffer;

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

    public InputRecordStreamImpl(InputStream netStream, TlsVersion version) {
        this.netStream = netStream;
        this.version = version;
        this.headerBuffer = ByteBuffer.allocate(5);
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

        if (cipherBox != null) {

        }

        switch (contentType) {
            case HANDSHAKE:
            case CHANGE_CIPHER_SPEC:
                HandshakeProtocol protocol = HandshakeMsgReaderUtil.read(content);
                return new Record(contentType, version, protocol, content);
            case ALTER:
            case APPLICATION_DATA:
            default:
                throw new RuntimeException("暂不支持的contentType:" + header[0]);
        }
    }

    private byte[] decrypt(byte contentType, byte[] data) {
        int nonceLen = cipherBox.applyExplicitNonce(authenticator, contentType, data);
        int tagLen = cipherBox.getTagSize();

        if (cipherDesc.getCipherType() == CipherSuite.CipherType.AEAD) {
            // AEAD模式应该去除iv
            return cipherBox.decrypt(data, cipherBox.getRecordIvSize(),
                data.length - cipherBox.getRecordIvSize());
        } else if (cipherDesc.getCipherType() == CipherSuite.CipherType.BLOCK) {
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
