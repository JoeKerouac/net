package com.joe.ssl.message.extension;

import com.joe.ssl.NamedCurve;
import com.joe.ssl.message.WrapedOutputStream;

import java.io.IOException;

/**
 * ECC椭圆曲线扩展
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-10 10:16
 */
public class EllipticCurvesExtension implements Extension {

    private static final ExtensionType TYPE = ExtensionType.EXT_ELLIPTIC_CURVES;

    private int[] curveIds;

    public EllipticCurvesExtension() {
        this.curveIds = NamedCurve.getAllSupportCurve().stream().mapToInt(NamedCurve::getId).toArray();
    }

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
        return TYPE;
    }

    @Override
    public int size() {
        return curveIds.length * 2 + 4;
    }
}
