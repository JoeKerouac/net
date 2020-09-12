package com.joe.ssl.message.extension;

import java.io.IOException;
import java.util.Arrays;

import com.joe.ssl.message.WrapedOutputStream;

/**
 *
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-10 10:16
 */
public class EllipticPointFormatsExtension implements HelloExtension {

    final static int                                  FMT_UNCOMPRESSED = 0;

    public static final EllipticPointFormatsExtension DEFAULT          = new EllipticPointFormatsExtension(
        new byte[] { FMT_UNCOMPRESSED });

    private byte[]                                    format;

    public EllipticPointFormatsExtension(byte[] format) {
        this.format = format;
    }

    @Override
    public void write(WrapedOutputStream outputStream) throws IOException {
        outputStream.writeInt16(getExtensionType().id);
        outputStream.writeInt16(format.length + 1);
        outputStream.writeInt8(format.length);
        for (byte b : format) {
            outputStream.writeInt8(b);
        }
    }

    @Override
    public int size() {
        return format.length + 1 + 4;
    }

    @Override
    public ExtensionType getExtensionType() {
        return ExtensionType.EXT_EC_POINT_FORMATS;
    }

    @Override
    public String toString() {
        return String.format("%s :\t[supports format : %s]", getExtensionType().name,
                Arrays.toString(format));
    }
}
