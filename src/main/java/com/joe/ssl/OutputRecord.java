package com.joe.ssl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.joe.ssl.crypto.DigestSpi;
import com.joe.ssl.message.WrapedOutputStream;

import lombok.Setter;

public class OutputRecord extends WrapedOutputStream {

    @Setter
    private DigestSpi             digestSpi;

    private ByteArrayOutputStream stream;

    public OutputRecord(OutputStream stream) {
        super(stream);
        this.stream = new ByteArrayOutputStream();
    }

    @Override
    public void write(int b) throws IOException {
        if (digestSpi == null) {
            stream.write(b);
        } else if (stream == null) {
            digestSpi.update((byte) b);
        } else {
            digestSpi.update(stream.toByteArray());
            digestSpi.update((byte) b);
            stream = null;
        }

        super.write(b);
    }

    /**
     * 获取当前的摘要，可重复调用
     * @return 当前摘要
     */
    public byte[] getDigest() {
        try {
            return digestSpi.copy().digest();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("摘要spi不支持copy方法", e);
        }
    }
}
