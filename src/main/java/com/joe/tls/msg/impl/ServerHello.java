package com.joe.tls.msg.impl;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.joe.ssl.cipher.CipherSuite;
import com.joe.ssl.message.TlsVersion;
import com.joe.tls.enums.HandshakeType;
import com.joe.tls.msg.HandshakeProtocol;
import com.joe.tls.msg.extensions.ExtensionReader;
import com.joe.tls.msg.extensions.ExtensionType;
import com.joe.tls.msg.extensions.HelloExtension;
import com.joe.tls.util.ByteBufferUtil;

import lombok.Getter;

/**
 *
 * ServerHello消息包含如下内容：
 * <li>消息类型，1byte，ServerHello固定是02</li>
 * <li>消息长度，3byte，不包含消息类型和消息长度本身，仅包含之后的内容</li>
 * <li>TLS version，2byte</li>
 * <li>random：32byte</li>
 * <li>sessionID长度：1byte</li>
 * <li>sessionId：边长，长度需要等于上边的长度</li>
 * <li>Cipher Suite：加密套件，2byte</li>
 * <li>compression method：压缩方法，1byte，一般为null（即0）</li>
 * <li>extensions 长度：2byte</li>
 * <li>extensions：变长，长度等于上边定义的长度</li>
 *
 * @author JoeKerouac
 * @version 2020年06月12日 23:48
 */
public class ServerHello implements HandshakeProtocol {

    @Getter
    private TlsVersion                               version;

    private final byte[]                             serverRandom;

    private final byte[]                             sessionId;

    @Getter
    private final CipherSuite                        cipherSuite;

    private final Map<ExtensionType, HelloExtension> extensions;

    /**
     * 从ByteBuffer中构造serverHello，buffer的起始位置应该是server_hello_protocol协议的起始位置
     * @param buffer buffer
     */
    public ServerHello(ByteBuffer buffer) {
        this.extensions = new HashMap<>();
        // 跳过类型和长度，刚好是4byte
        ByteBufferUtil.mergeReadInt32(buffer);
        // 服务端实际选择的版本号
        this.version = TlsVersion.valueOf(ByteBufferUtil.mergeReadInt8(buffer),
            ByteBufferUtil.mergeReadInt8(buffer));
        this.serverRandom = ByteBufferUtil.get(buffer, 32);
        this.sessionId = ByteBufferUtil.getInt8(buffer);

        // 密码套件
        int cipherSuiteId = ByteBufferUtil.mergeReadInt16(buffer);
        this.cipherSuite = CipherSuite.getById(cipherSuiteId);

        int compressionMethodLen = ByteBufferUtil.mergeReadInt8(buffer);
        if (compressionMethodLen > 0) {
            throw new RuntimeException("不支持压缩算法，该算法已经不安全了");
        }
        // 如果还有数据，说明还有扩展数据，读取扩展数据
        if (buffer.limit() != buffer.position()) {
            List<HelloExtension> extensionList = ExtensionReader.read(buffer);
            extensionList.forEach(extension -> extensions.put(extension.getExtensionType(), extension));
        }
    }

    /**
     * 获取服务端随机数
     * @return 服务端随机数
     */
    public byte[] getServerRandom() {
        return serverRandom;
    }

    /**
     * 获取指定extension
     * @param type extension类型
     * @return 对应的extension，如果没有则返回null
     */
    public HelloExtension getExtension(ExtensionType type) {
        if (type == null) {
            return null;
        }
        return extensions.get(type);
    }

    @Override
    public HandshakeType type() {
        return HandshakeType.SERVER_HELLO;
    }

    @Override
    public int len() {
        throw new RuntimeException("暂不支持");
    }

    @Override
    public byte[] serialize() {
        throw new RuntimeException("暂不支持");
    }

}
