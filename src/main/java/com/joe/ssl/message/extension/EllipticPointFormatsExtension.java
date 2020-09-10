package com.joe.ssl.message.extension;

import com.joe.ssl.message.WrapedOutputStream;

import java.io.IOException;

public class EllipticPointFormatsExtension implements HelloExtension {

    final static int FMT_UNCOMPRESSED = 0;

    public static final EllipticPointFormatsExtension DEFAULT =
            new EllipticPointFormatsExtension(
                    new byte[]{FMT_UNCOMPRESSED});

    private byte[] format;

    public EllipticPointFormatsExtension(byte[] format) {
        this.format = format;
    }

    @Override
    public void write(WrapedOutputStream outputStream) throws IOException {
        outputStream.writeInt16(getExtensionType().id);
        outputStream.writeInt16(1 + format.length);
        outputStream.writeInt8(format.length);
        for (byte b : format) {
            outputStream.writeInt8(b);
        }
    }

    @Override
    public int size() {
        return format.length + 5;
    }

    @Override
    public ExtensionType getExtensionType() {
        return ExtensionType.EXT_EC_POINT_FORMATS;
    }
}
