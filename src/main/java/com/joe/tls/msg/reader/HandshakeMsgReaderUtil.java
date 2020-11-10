package com.joe.tls.msg.reader;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.joe.tls.enums.HandshakeType;
import com.joe.tls.msg.HandshakeProtocol;
import com.joe.tls.msg.impl.CertificateMsg;
import com.joe.tls.msg.impl.ECDHServerKeyExchange;
import com.joe.tls.msg.impl.ServerHello;
import com.joe.tls.msg.impl.ServerHelloDone;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-08 17:06
 */
public class HandshakeMsgReaderUtil {

    /**
     * 各种消息的读取器
     */
    private static final Map<HandshakeType, HandshakeMsgReader> READER_MAP = new HashMap<>();

    static {
        READER_MAP.put(HandshakeType.CERTIFICATE,
            data -> new CertificateMsg(ByteBuffer.wrap(data)));
        READER_MAP.put(HandshakeType.SERVER_KEY_EXCHANGE,
            data -> new ECDHServerKeyExchange(ByteBuffer.wrap(data)));
        READER_MAP.put(HandshakeType.SERVER_HELLO, data -> new ServerHello(ByteBuffer.wrap(data)));
        READER_MAP.put(HandshakeType.SERVER_HELLO_DONE, data -> new ServerHelloDone());
    }

    /**
     * 读取握手数据
     * @param data 握手数据
     * @param <T> 握手数据实际类型
     * @return 握手数据实体
     */
    @SuppressWarnings("unchecked")
    public static <T extends HandshakeProtocol> T read(byte[] data) {
        HandshakeType type = HandshakeType.getByCode(data[0]);
        HandshakeMsgReader reader;
        if (type == null || (reader = READER_MAP.get(type)) == null) {
            throw new RuntimeException("不支持读取的handshake类型：" + data[0]);
        }
        return (T) reader.read(data);
    }

}
