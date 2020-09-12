package com.joe.ssl.message.extension;

import java.io.IOException;

import com.joe.ssl.message.WrapedOutputStream;

/**
 * 暂时不知道该扩展是做什么的，先用空处理（目前抓包的正常的也是空）
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-11 09:04
 */
public class RenegotiationInfoExtension implements HelloExtension {

    @Override
    public void write(WrapedOutputStream outputStream) throws IOException {
        outputStream.writeInt16(getExtensionType().id);
        outputStream.writeInt16(1);
        outputStream.writeInt8(0);
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
