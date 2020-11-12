package com.joe.tls.msg.extensions;

import java.nio.ByteBuffer;

import com.joe.tls.util.ByteBufferUtil;

/**
 * 安全重新协商时避免中间人攻击使用的，具体用法有时间再调研
 *
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-11 09:04
 */
public class RenegotiationInfoExtension implements HelloExtension {

    @Override
    public void write(ByteBuffer buffer) {
        ByteBufferUtil.writeInt16(getExtensionType().id, buffer);
        ByteBufferUtil.writeInt16(1, buffer);
        ByteBufferUtil.writeInt8(0, buffer);
    }

    @Override
    public int size() {
        return 1 + 4;
    }

    @Override
    public ExtensionType getExtensionType() {
        return ExtensionType.EXT_RENEGOTIATION_INFO;
    }

    @Override
    public String toString() {
        return String.format("%s", getExtensionType().name);
    }
}
