package com.joe.ssl.message.extension;

import com.joe.ssl.message.WrapedOutputStream;

import java.io.IOException;

/**
 *
 * https://tools.ietf.org/html/rfc7627
 */
public class ExtendedMasterSecretExtension implements HelloExtension{

    @Override
    public void write(WrapedOutputStream outputStream) throws IOException {
        outputStream.writeInt16(getExtensionType().id);
        outputStream.writeInt16(0);
    }

    @Override
    public int size() {
        return 4;
    }

    @Override
    public ExtensionType getExtensionType() {
        return ExtensionType.EXT_EXTENDED_MASTER_SECRET;
    }
}
