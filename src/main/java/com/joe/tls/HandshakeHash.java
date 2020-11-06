package com.joe.tls;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import com.joe.ssl.crypto.DigestSpi;

/**
 * @author JoeKerouac
 * @data 2020-11-06 22:26
 */
public class HandshakeHash {

    private DigestSpi             digest;

    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    /**
     * 更新数据到摘要中
     * @param data 数据
     * @param offset 起始位置
     * @param len 长度
     */
    public void update(byte[] data, int offset, int len) {
        if (digest == null) {
            buffer.write(data, offset, len);
        } else {
            digest.update(Arrays.copyOfRange(data, offset, offset + len));
        }
    }

    /**
     * 设置hash算法
     * @param alg hash算法
     */
    public void setFinishedAlg(String alg) {
        if (digest != null) {
            return;
        }
        digest = DigestSpi.getInstance(alg);
        digest.update(buffer.toByteArray());
        buffer = null;
    }

    /**
     * 获取当前摘要
     * @return 当前摘要
     */
    public byte[] getFinishedHash() {
        try {
            return digest.copy().digest();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
