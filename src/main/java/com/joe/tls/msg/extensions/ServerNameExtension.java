package com.joe.tls.msg.extensions;

import java.nio.ByteBuffer;

import com.joe.tls.util.ByteBufferUtil;
import com.joe.utils.common.Assert;

import lombok.Getter;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-11 08:54
 */
public class ServerNameExtension implements HelloExtension {

    private final byte type;

    @Getter
    private final byte[] serverName;

    public ServerNameExtension(byte type, byte[] serverName) {
        Assert.notNull(serverName);
        this.type = type;
        this.serverName = serverName;
    }

    @Override
    public void write(ByteBuffer buffer) {
        ByteBufferUtil.writeInt16(getExtensionType().id, buffer);
        ByteBufferUtil.writeInt16(2 + 1 + 2 + serverName.length, buffer);
        ByteBufferUtil.writeInt16(1 + 2 + serverName.length, buffer);
        ByteBufferUtil.writeInt8(type, buffer);
        ByteBufferUtil.putBytes16(serverName, buffer);
    }

    @Override
    public int size() {
        return 2 + 1 + 2 + serverName.length + 4;
    }

    @Override
    public ExtensionType getExtensionType() {
        return ExtensionType.EXT_SERVER_NAME;
    }

    @Override
    public String toString() {
        return String.format("%s :\t[server name : %s]\t[server type : %d]", getExtensionType().name,
            new String(serverName), type);
    }
}
