package com.joe.ssl.message;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.joe.ssl.cipher.CipherSuite;
import com.joe.ssl.message.extension.ExtensionReader;
import com.joe.ssl.message.extension.ExtensionType;
import com.joe.ssl.message.extension.HelloExtension;

import lombok.Getter;
import sun.security.ssl.ProtocolVersion;

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
 * <li>extensions：边长，长度等于上边定义的长度</li>
 *
 * @author JoeKerouac
 * @version 2020年06月12日 23:48
 */
@Getter
public class ServerHello implements HandshakeMessage {

    private int                                version;

    /**
     * 消息长度
     */
    private int                                len;

    private byte[]                             serverRandom;

    private ProtocolVersion                    protocolVersion;

    private ClientHello                        clientHello;

    @Getter
    private CipherSuite                        cipherSuite;

    private Map<ExtensionType, HelloExtension> extensions;

    @Override
    public void init(int bodyLen, WrapedInputStream inputStream) {
        try {
            this.extensions = new HashMap<>();
            this.version = inputStream.readInt16();
            this.serverRandom = inputStream.read(32);
            int sessionIdLen = inputStream.readInt8();
            // 跳过session
            inputStream.skip(sessionIdLen);
            // 密码套件
            int cipherSuiteId = inputStream.readInt16();
            this.cipherSuite = CipherSuite.getById(cipherSuiteId);
            System.out.println("密码套件是：" + cipherSuiteId);
            System.out.println("密码套件是：" + cipherSuite);
            int compressionMethodLen = inputStream.readInt8();
            if (compressionMethodLen > 0) {
                throw new RuntimeException("不支持压缩算法，该算法已经不安全了");
            }
            // 读取扩展数据
            List<HelloExtension> extensionList = ExtensionReader.read(inputStream);
            extensionList
                .forEach(extension -> extensions.put(extension.getExtensionType(), extension));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
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
    public int size() {
        throw new RuntimeException("未实现");
    }

    @Override
    public HandshakeType type() {
        return HandshakeType.SERVER_HELLO;
    }

    @Override
    public void write(WrapedOutputStream stream) throws IOException {

    }
}
