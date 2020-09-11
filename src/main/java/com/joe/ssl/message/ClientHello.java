package com.joe.ssl.message;

import com.joe.ssl.cipher.CipherSuite;
import com.joe.ssl.message.extension.*;
import com.joe.ssl.openjdk.ssl.ProtocolVersion;
import com.joe.utils.protocol.DatagramUtil;
import lombok.Data;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * @author JoeKerouac
 * @version 2020年06月13日 16:24
 */
@Data
public class ClientHello implements HandshakeMessage {

    /**
     * 客户端随机数，32byte
     */
    private byte[] clientRandom = new byte[32];

    /**
     * sessionId，最大256
     */
    private byte[] sessionId = new byte[0];

    private TlsVersion tlsVersion;

    private List<CipherSuite> cipherSuites;

    private List<HelloExtension> extensions;

    private String serverName;

    public ClientHello(String serverName) {
        this.serverName = serverName;
        this.init();
    }

    public void init() {
        // 随机数，前4byte需要是当前时间
        {
            new SecureRandom().nextBytes(clientRandom);

            long temp = System.currentTimeMillis() / 1000;
            int gmt_unix_time;
            if (temp < Integer.MAX_VALUE) {
                gmt_unix_time = (int) temp;
            } else {
                gmt_unix_time = Integer.MAX_VALUE;          // Whoops!
            }

            clientRandom[0] = (byte)(gmt_unix_time >> 24);
            clientRandom[1] = (byte)(gmt_unix_time >> 16);
            clientRandom[2] = (byte)(gmt_unix_time >>  8);
            clientRandom[3] = (byte)gmt_unix_time;
        }

        this.tlsVersion = TlsVersion.TLS1_2;
        this.cipherSuites = new ArrayList<>(CipherSuite.CIPHER_SUITES);

        this.extensions = new ArrayList<>();

        // 初始化扩展
        // TODO 完善扩展
        {
            // 判断加密套件是否包含ECC算法
            boolean containEc = this.cipherSuites.stream().filter(CipherSuite::isEc).findFirst().map(CipherSuite::isEc).orElse(Boolean.FALSE);
            if (containEc) {
                extensions.add(new EllipticCurvesExtension());
                extensions.add(EllipticPointFormatsExtension.DEFAULT);
            }

            // 大于等于TLS1.2需要写出本地支持的签名算法
            extensions.add(new SignatureAndHashAlgorithmExtension());

            // master secret扩展
            extensions.add(new ExtendedMasterSecretExtension());

            // server name扩展
            extensions.add(new ServerNameExtension(serverName));

            // 暂时不知道是做什么的
            extensions.add(new RenegotiationInfoExtension());
        }
    }


    @Override
    public HandshakeType type() {
        return HandshakeType.CLIENT_HELLO;
    }

    @Override
    public int size() {
        // 1byte type + 3byte 长度字段 + 2byte版本号 + 32byte随机数 + 1byte的sessionId长度字段 + sessionId的实际长度 + 2byte加密
        // 套件的长度字段 + 加密套件实际长度 + 2byte压缩算法长度+算法信息（写死不使用压缩算法） + 2byte扩展长度字段 + 扩展实际长度
        return 1 + 3 + 2 + 32 + 1 + sessionId.length + 2 + cipherSuites.size() * 2 + 2 + 2 + extensions.stream().mapToInt(HelloExtension::size).sum();
    }

    @Override
    public void write(WrapedOutputStream stream) throws IOException {
        stream.writeInt8(type().getCode());
        stream.writeInt24(size() - 4);
        stream.writeInt8(tlsVersion.getMajorVersion());
        stream.writeInt8(tlsVersion.getMinorVersion());
        stream.write(clientRandom);
        stream.putBytes8(sessionId);
        {
            // 写出加密套件
            // 加密套件的总长度，一个加密套件是2 byte，所以需要*2
            stream.writeInt16(2 * cipherSuites.size());
            // 实际加密套件写出
            for (CipherSuite cipherSuite : cipherSuites) {
                stream.writeInt16(cipherSuite.getSuite());
            }

        }

        {
            // 写出compression_methods，固定写出null，表示不使用
            stream.writeInt8(1);
            stream.writeInt8(0);
        }



        // 写出extensions
        {
            // 计算扩展总长度，单位byte
            int size = extensions.stream().mapToInt(HelloExtension::size).sum();
            // 写出扩展长度
            stream.writeInt16(size);
            // 写出各个扩展
            for (HelloExtension helloExtension : extensions) {
                helloExtension.write(stream);
            }
        }
    }




}
