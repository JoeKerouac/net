package com.joe.tls.msg.extensions;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.joe.tls.util.ByteBufferUtil;

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
    public void write(ByteBuffer buffer) {
        ByteBufferUtil.writeInt16(getExtensionType().id, buffer);
        ByteBufferUtil.writeInt16(format.length + 1, buffer);
        ByteBufferUtil.writeInt8(format.length, buffer);

        for (byte b : format) {
            ByteBufferUtil.writeInt8(b, buffer);
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
