package com.joe.ssl.message.extension;

import com.joe.ssl.message.WrapedOutputStream;

import java.io.IOException;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-11 08:54
 */
public class ServerNameExtension implements HelloExtension {

    private byte[] serverName;

    public ServerNameExtension(String serverName) {
        this.serverName = serverName.getBytes();
    }

    @Override
    public void write(WrapedOutputStream outputStream) throws IOException {
        outputStream.writeInt16(getExtensionType().id);
        outputStream.writeInt16(2 +1 + 2 + serverName.length);
        outputStream.writeInt16(1 + 2 + serverName.length);
        // type字段，这里先写死host_name类型
        outputStream.writeInt8(0);
        outputStream.putBytes16(serverName);
    }

    @Override
    public int size() {
        return 2 +1 + 2 + serverName.length + 4;
    }

    @Override
    public ExtensionType getExtensionType() {
        return ExtensionType.EXT_SERVER_NAME;
    }
}
