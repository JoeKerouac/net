package com.joe.ssl.message.extension;

import java.io.IOException;

import com.joe.ssl.message.WrapedOutputStream;
import com.joe.utils.common.Assert;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-11 08:54
 */
public class ServerNameExtension implements HelloExtension {

    private byte   type;

    private byte[] serverName;

    public ServerNameExtension(byte type, byte[] serverName) {
        Assert.notNull(serverName);
        this.type = type;
        this.serverName = serverName;
    }

    @Override
    public void write(WrapedOutputStream outputStream) throws IOException {
        outputStream.writeInt16(getExtensionType().id);
        outputStream.writeInt16(2 + 1 + 2 + serverName.length);
        outputStream.writeInt16(1 + 2 + serverName.length);
        // type字段，这里先写死host_name类型
        outputStream.writeInt8(type);
        outputStream.putBytes16(serverName);
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
        return String.format("%s :\t[server name : %s]\t[server type : %d]",
            getExtensionType().name, new String(serverName), type);
    }
}
