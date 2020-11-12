package com.joe.tls.msg.impl;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.joe.tls.SignatureAndHashAlgorithm;
import com.joe.tls.TlsVersion;
import com.joe.tls.cipher.CipherSuite;
import com.joe.tls.enums.HandshakeType;
import com.joe.tls.enums.NamedCurve;
import com.joe.tls.msg.HandshakeProtocol;
import com.joe.tls.msg.extensions.*;
import com.joe.tls.util.ByteBufferUtil;
import com.joe.utils.codec.Hex;
import com.joe.utils.common.Assert;
import com.joe.utils.common.string.StringUtils;

/**
 * ClientHello
 * 
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-02 11:41
 */
public class ClientHello implements HandshakeProtocol {

    /**
     * 客户端随机数，32byte
     */
    private final byte[]         clientRandom = new byte[32];

    /**
     * sessionId，最大256
     */
    private byte[]               sessionId    = new byte[0];

    /**
     * 版本号
     */
    private TlsVersion           tlsVersion;

    /**
     * 加密套件
     */
    private List<CipherSuite>    cipherSuites = Collections.emptyList();

    /**
     * 扩展
     */
    private List<HelloExtension> extensions   = Collections.emptyList();

    /**
     * 服务器名，可能为null
     */
    private final String         serverName;

    /**
     * toString，性能考虑，存储起来不用每次计算
     */
    private String               toString     = "exception or incomplete";

    /**
     * 协议消息完整数据
     */
    private byte[]               data;

    /**
     * 安全随机数
     */
    private SecureRandom         secureRandom;

    /**
     * 构建一个client_hello
     * @param serverName 服务器名，可以为空
     */
    public ClientHello(String serverName, SecureRandom secureRandom) {
        this.serverName = serverName;
        this.secureRandom = secureRandom;
        this.init();
    }

    public ClientHello(ByteBuffer buffer) {
        init(buffer);

        ServerNameExtension serverNameExtension = null;
        for (HelloExtension extension : extensions) {
            if (extension instanceof ServerNameExtension) {
                serverNameExtension = (ServerNameExtension) extension;
                break;
            }
        }

        if (serverNameExtension != null) {
            this.serverName = new String(serverNameExtension.getServerName());
        } else {
            this.serverName = null;
        }
    }

    /**
     * 获取客户端随机数
     * @return 客户端随机数
     */
    public byte[] getClientRandom() {
        return clientRandom;
    }

    public TlsVersion version() {
        return tlsVersion;
    }

    @Override
    public HandshakeType type() {
        return HandshakeType.CLIENT_HELLO;
    }

    @Override
    public int len() {
        // 2byte版本号 + 32byte随机数 + 1byte的sessionId长度字段 + sessionId的实际长度 + 2byte加密
        // 套件的长度字段 + 加密套件实际长度 + 2byte压缩算法长度+算法信息（写死不使用压缩算法） + 2byte扩展长度字段 + 扩展实际长度
        return 2 + 32 + 1 + sessionId.length + 2 + cipherSuites.size() * 2 + 2 + 2
               + extensions.stream().mapToInt(HelloExtension::size).sum();
    }

    @Override
    public byte[] serialize() {
        if (data != null) {
            return data;
        }

        data = new byte[1 + 3 + len()];
        ByteBuffer heapBuffer = ByteBuffer.wrap(data);

        ByteBufferUtil.writeInt8(type().getCode(), heapBuffer);
        ByteBufferUtil.writeInt24(len(), heapBuffer);
        ByteBufferUtil.writeInt8(tlsVersion.getMajorVersion(), heapBuffer);
        ByteBufferUtil.writeInt8(tlsVersion.getMinorVersion(), heapBuffer);
        heapBuffer.put(clientRandom);
        ByteBufferUtil.putBytes8(sessionId, heapBuffer);

        {
            // 写出加密套件
            // 加密套件的总长度，一个加密套件是2 byte，所以需要*2
            ByteBufferUtil.writeInt16(2 * cipherSuites.size(), heapBuffer);
            // 实际加密套件写出
            for (CipherSuite cipherSuite : cipherSuites) {
                ByteBufferUtil.writeInt16(cipherSuite.getSuite(), heapBuffer);

            }

        }

        {
            // 写出compression_methods，固定写出null，表示不使用
            ByteBufferUtil.writeInt8(1, heapBuffer);
            ByteBufferUtil.writeInt8(0, heapBuffer);
        }

        // 写出extensions
        {
            // 计算扩展总长度，单位byte
            int size = extensions.stream().mapToInt(HelloExtension::size).sum();
            // 写出扩展长度
            ByteBufferUtil.writeInt16(size, heapBuffer);
            // 写出各个扩展
            for (HelloExtension helloExtension : extensions) {
                helloExtension.write(heapBuffer);
            }
        }

        // 最后记录数据
        return data;
    }

    /**
     * 从ByteBuffer中读取ClientHello，输入流的起始位置应该是Handshake的handshakeType字段而不是record的content type
     *
     * @param buffer 数据
     */
    private void init(ByteBuffer buffer) {
        // 1byte类型信息
        Assert.assertEquals(buffer.get(), type().getCode());

        // 3byte的长度信息，暂时无用，忽略
        ByteBufferUtil.mergeReadInt24(buffer);

        // Version信息
        this.tlsVersion = TlsVersion.valueOf(buffer.get(), buffer.get());

        // 读取客户端随机数
        buffer.get(this.clientRandom);

        // 读取session
        this.sessionId = ByteBufferUtil.getInt8(buffer);

        // 读取cipher
        int cipherLen = Short.toUnsignedInt(buffer.getShort()) / 2;
        this.cipherSuites = new ArrayList<>();
        for (int i = 0; i < cipherLen; i++) {
            this.cipherSuites.add(CipherSuite.getById(Short.toUnsignedInt(buffer.getShort())));
        }

        // compression_methods，不允许压缩
        byte compressionMethod = buffer.get();
        Assert.assertEquals((byte) 1, compressionMethod);
        Assert.assertEquals((byte) 0, buffer.get());

        this.extensions = ExtensionReader.read(buffer);

        // 最后计算下toString
        calcToString();
    }

    /**
     * 初始化一个默认的ClientHello
     */
    private void init() {
        // 随机数，前4byte需要是当前时间
        {
            secureRandom.nextBytes(clientRandom);

            long temp = System.currentTimeMillis() / 1000;
            int gmt_unix_time;
            if (temp < Integer.MAX_VALUE) {
                gmt_unix_time = (int) temp;
            } else {
                gmt_unix_time = Integer.MAX_VALUE;
            }

            clientRandom[0] = (byte) (gmt_unix_time >> 24);
            clientRandom[1] = (byte) (gmt_unix_time >> 16);
            clientRandom[2] = (byte) (gmt_unix_time >> 8);
            clientRandom[3] = (byte) gmt_unix_time;
        }

        this.tlsVersion = TlsVersion.TLS1_2;
        this.cipherSuites = CipherSuite.getAllSupports();

        this.extensions = new ArrayList<>();

        // 初始化扩展
        {
            // 判断加密套件是否包含ECC算法
            boolean containEc = this.cipherSuites.stream().filter(CipherSuite::isEc).findFirst()
                .map(CipherSuite::isEc).orElse(Boolean.FALSE);
            if (containEc) {
                extensions.add(new EllipticCurvesExtension(NamedCurve.getAllSupportCurve().stream()
                    .mapToInt(NamedCurve::getId).toArray()));
                extensions.add(EllipticPointFormatsExtension.DEFAULT);
            }

            // 大于等于TLS1.2需要写出本地支持的签名算法
            extensions.add(new SignatureAndHashAlgorithmExtension(
                new ArrayList<>(SignatureAndHashAlgorithm.getAllSupports().values())));

            // master secret扩展
            extensions.add(new ExtendedMasterSecretExtension());

            // server name扩展，type暂时写死0
            if (!StringUtils.isEmpty(serverName)) {
                extensions.add(new ServerNameExtension((byte) 0, serverName.getBytes()));
            }

            // 暂时不知道是做什么的
            extensions.add(new RenegotiationInfoExtension());
        }

        // 最后计算下toString
        calcToString();

    }

    public void calcToString() {
        this.toString = String
            .format("Handshake Type : %s\n" + "Len : %d\n" + "version: %s\n" + "random : %s\n"
                    + "sessionId : %s\n" + "cipherSuites: %s\n" + "extension : %s",
                type(), len(), tlsVersion, new String(Hex.encodeHex(clientRandom, false)),
                Arrays.toString(sessionId), cipherSuites, arrayToString(extensions));
    }

    private String arrayToString(List<?> list) {
        StringBuilder sb = new StringBuilder();
        list.forEach(entry -> {
            sb.append("\n\t").append(entry);
        });
        return sb.toString();
    }

    @Override
    public String toString() {
        return this.toString;
    }
}
