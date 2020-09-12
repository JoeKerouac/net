package com.joe.ssl.message.extension;

import java.io.IOException;
import java.util.List;

import com.joe.ssl.message.SignatureAndHashAlgorithm;
import com.joe.ssl.message.WrapedOutputStream;
import com.joe.utils.common.Assert;

/**
 * 签名算法支持
 *
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-10 13:57
 */
public class SignatureAndHashAlgorithmExtension implements HelloExtension {

    private List<SignatureAndHashAlgorithm> supports;

    public SignatureAndHashAlgorithmExtension(List<SignatureAndHashAlgorithm> supports) {
        Assert.notEmpty(supports);
        this.supports = supports;
    }

    @Override
    public void write(WrapedOutputStream outputStream) throws IOException {
        outputStream.writeInt16(getExtensionType().id);
        outputStream.writeInt16(supports.size() * 2 + 2);
        outputStream.writeInt16(supports.size() * 2);
        for (SignatureAndHashAlgorithm support : supports) {
            outputStream.writeInt16(support.getId());
        }
    }

    @Override
    public int size() {
        return supports.size() * 2 + 2 + 4;
    }

    @Override
    public ExtensionType getExtensionType() {
        return ExtensionType.EXT_SIGNATURE_ALGORITHMS;
    }

    @Override
    public String toString() {
        return String.format("%s :\t[supports SignatureAndHashAlgorithm : %s]",
            getExtensionType().name, supports);
    }
}
