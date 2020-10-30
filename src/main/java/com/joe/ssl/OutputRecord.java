package com.joe.ssl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.joe.ssl.crypto.DigestSpi;
import com.joe.ssl.message.ContentType;
import com.joe.ssl.message.HandshakeMessage;
import com.joe.ssl.message.TlsVersion;
import com.joe.ssl.message.WrapedOutputStream;

import lombok.Getter;
import lombok.Setter;

public class OutputRecord extends WrapedOutputStream {

    @Setter
    private DigestSpi             digestSpi;

    @Getter
    @Setter
    private ByteArrayOutputStream stream;

    public OutputRecord(OutputStream stream) {
        super(stream);
        this.stream = new ByteArrayOutputStream();
    }

    public void write(HandshakeMessage handshakeMessage) throws IOException {
        // 写出header
        writeInt8(ContentType.HANDSHAKE.getCode());
        TlsVersion.TLS1_2.write(this);
        this.writeInt16(handshakeMessage.size());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        handshakeMessage.write(new WrapedOutputStream(outputStream));
        write(outputStream.toByteArray());
        // client_hello发送的时候还没有digestSpi
        if (digestSpi != null) {
            digestSpi.update(outputStream.toByteArray());
        } else {
            stream.write(outputStream.toByteArray());
        }
    }

    @Override
    public void write(int b) throws IOException {
        super.write(b);
    }

    //    @Override
    //    public void write(int b) throws IOException {
    //        if (digestSpi == null) {
    //            stream.write(b);
    //        } else if (stream == null) {
    //            digestSpi.update((byte) b);
    //        } else {
    //            digestSpi.update(stream.toByteArray());
    //            digestSpi.update((byte) b);
    //            stream = null;
    //        }
    //
    //        super.write(b);
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
