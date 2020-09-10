package com.joe.ssl.message;

import com.joe.ssl.cipher.CipherSuite;
import com.joe.ssl.message.extension.EllipticCurvesExtension;
import com.joe.ssl.message.extension.EllipticPointFormatsExtension;
import com.joe.ssl.message.extension.HelloExtension;
import com.joe.ssl.openjdk.ssl.ProtocolVersion;
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author JoeKerouac
 * @version 2020年06月13日 16:24
 */
@Data
public class ClientHello implements HandshakeMessage {

    /**
     * 客户端随机数，32位
     */
    private byte[] clientRandom;

    /**
     * sessionId，最大256
     */
    private byte[] sessionId = new byte[0];

    private ProtocolVersion protocolVersion;

    public void init() {

    }


    @Override
    public HandshakeType type() {
        return HandshakeType.CLIENT_HELLO;
    }

    @Override
    public void write(WrapedOutputStream stream) throws IOException {
        stream.writeInt8(protocolVersion.major);
        stream.writeInt8(protocolVersion.minor);
        stream.write(clientRandom);
        stream.putBytes8(sessionId);
        {
            // 写出加密套件
            // 加密套件的总长度，一个加密套件是2 byte，所以需要*2
            stream.writeInt16(2 * CipherSuite.CIPHER_SUITES.size());
            // 实际加密套件写出
            for (CipherSuite cipherSuite : CipherSuite.CIPHER_SUITES) {
                stream.writeInt16(cipherSuite.getSuite());
            }

        }

        {
            // 写出compression_methods，固定写出null，表示不使用
            stream.writeInt8(1);
            stream.writeInt8(0);

        }

        List<HelloExtension> extensionList = new ArrayList<>();


        // TODO 写出extensions
        {
            // 判断加密套件是否包含ECC算法
            boolean containEc = CipherSuite.CIPHER_SUITES.stream().filter(CipherSuite::isEc).findFirst().map(CipherSuite::isEc).orElse(Boolean.FALSE);
            if (containEc) {
                extensionList.add(new EllipticCurvesExtension());
                extensionList.add(EllipticPointFormatsExtension.DEFAULT);
            }

            // 大于等于TLS1.2需要写出本地支持的签名算法
        }

    }


}
