package com.joe.tls.msg;

import java.util.HashMap;
import java.util.Map;

import com.joe.tls.enums.HandshakeType;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-08 17:06
 */
public class HandshakeMsgReaderUtil {

    private static Map<HandshakeType, HandshakeMsgReader> READER_MAP = new HashMap<>();

    /**
     * 读取握手数据
     * @param data 握手数据
     * @param <T> 握手数据实际类型
     * @return 握手数据实体
     */
    public static <T extends HandshakeProtocol> T read(byte[] data) {
        HandshakeType type = HandshakeType.getByCode(data[0]);
        HandshakeMsgReader reader = null;
        if (type == null || (reader = READER_MAP.get(type)) == null) {
            throw new RuntimeException("不支持读取的handshake类型：" + data[0]);
        }
        return reader.read(data);
    }

}
