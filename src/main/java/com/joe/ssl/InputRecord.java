package com.joe.ssl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.joe.ssl.crypto.DigestSpi;
import com.joe.ssl.message.WrapedInputStream;

import lombok.Setter;

public class InputRecord extends WrapedInputStream {

    @Setter
    private DigestSpi             digestSpi;

    private ByteArrayOutputStream stream;

    public InputRecord(InputStream stream) {
        super(stream);
        this.stream = new ByteArrayOutputStream();
    }

    @Override
    public int read() throws IOException {
        return super.read();
    }

//    @Override
//    public int read() throws IOException {
//        int data = super.read();
//        if (digestSpi == null) {
//            stream.write(data);
//        } else if (stream == null) {
//            digestSpi.update((byte) data);
//        } else {
//            digestSpi.update(stream.toByteArray());
//            digestSpi.update((byte) data);
//            stream = null;
//        }
//        return data;
//    }

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
