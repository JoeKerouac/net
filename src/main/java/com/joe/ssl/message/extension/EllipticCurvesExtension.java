package com.joe.ssl.message.extension;

import java.io.IOException;

import com.joe.ssl.NamedCurve;
import com.joe.ssl.message.WrapedOutputStream;

/**
 * ECC椭圆曲线扩展
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-10 10:16
 */
public class EllipticCurvesExtension implements HelloExtension {

    private final int[] curveIds = NamedCurve.getAllSupportCurve().stream()
        .mapToInt(NamedCurve::getId).toArray();

    @Override
    public void write(WrapedOutputStream outputStream) throws IOException {
        outputStream.writeInt16(getExtensionType().id);
        outputStream.writeInt16(curveIds.length * 2 + 2);
        outputStream.writeInt16(curveIds.length * 2);
        for (int curveId : curveIds) {
            outputStream.writeInt16(curveId);
        }
    }

    @Override
    public ExtensionType getExtensionType() {
        return ExtensionType.EXT_ELLIPTIC_CURVES;
    }

    @Override
    public int size() {
        return curveIds.length * 2 + 2 + 4;
    }
}
