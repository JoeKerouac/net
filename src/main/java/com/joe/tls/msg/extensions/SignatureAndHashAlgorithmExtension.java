package com.joe.tls.msg.extensions;

import java.nio.ByteBuffer;
import java.util.List;

import com.joe.tls.SignatureAndHashAlgorithm;
import com.joe.tls.util.ByteBufferUtil;
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
    public void write(ByteBuffer buffer) {
        ByteBufferUtil.writeInt16(getExtensionType().id, buffer);
        ByteBufferUtil.writeInt16(supports.size() * 2 + 2, buffer);
        ByteBufferUtil.writeInt16(supports.size() * 2, buffer);
        for (SignatureAndHashAlgorithm support : supports) {
            ByteBufferUtil.writeInt16(support.getId(), buffer);
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
