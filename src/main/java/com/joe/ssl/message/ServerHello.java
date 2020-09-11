package com.joe.ssl.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import com.joe.ssl.message.extension.ExtensionType;
import com.joe.ssl.message.extension.HelloExtension;
import com.joe.utils.common.Assert;

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

    private Map<ExtensionType, HelloExtension> extensions;

    public ServerHello(byte[] data) {
        try {
            WrapedInputStream inputStream = new WrapedInputStream(new ByteArrayInputStream(data));
            Assert.isTrue(type().getCode() == inputStream.readInt8());
            this.version = inputStream.readInt16();
            this.serverRandom = inputStream.read(32);
            int sessionIdLen = inputStream.readInt8();
            // 跳过session
            //            Assert.isTrue(sessionIdLen == 0);
            inputStream.read(sessionIdLen);
            // 密码套件
            int cipherSuite = inputStream.readInt16();
            System.out.println(String.format("当前密码套件：%x", cipherSuite));
            // 其他的数据先不管
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
