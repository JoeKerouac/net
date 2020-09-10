package com.joe.ssl.message.extension;

import com.joe.ssl.message.SignatureAndHashAlgorithm;
import com.joe.ssl.message.WrapedOutputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 签名算法支持
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-10 13:57
 */
public class SignatureAndHashAlgorithmExtension implements HelloExtension {

    private static final List<SignatureAndHashAlgorithm> ALL_SUPPORTS = new ArrayList<>(SignatureAndHashAlgorithm.getAllSupports().values());

    @Override
    public void write(WrapedOutputStream outputStream) throws IOException {
        outputStream.writeInt16(getExtensionType().id);
        outputStream.writeInt16(ALL_SUPPORTS.size() + 2);
        for (SignatureAndHashAlgorithm allSupport : ALL_SUPPORTS) {
            outputStream.writeInt16(allSupport.getId());
        }
    }

    @Override
    public int size() {
        return ALL_SUPPORTS.size() + 6;
    }

    @Override
    public ExtensionType getExtensionType() {
        return ExtensionType.EXT_SIGNATURE_ALGORITHMS;
    }
}
