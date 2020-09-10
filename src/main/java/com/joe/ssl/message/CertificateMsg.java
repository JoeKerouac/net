package com.joe.ssl.message;

import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * certificate握手消息格式：
 * <li>消息类型，1byte，Certificate固定是11</li>
 * <li>消息长度，3byte，不包含消息类型和消息长度本身，仅包含之后的内容</li>
 * <li>Certificates len：3byte，下面是list列表：</li>
 * <ul>
 * <li>certificate len：3byte，注意与上边Certificates len的区别，certificate是一个列表，上边是列表数据总长度，这里是单个的</li>
 * <li>Certificate信息，长度与上边定义的一致</li>
 * </ul>
 *
 * @author JoeKerouac
 * @version 2020年06月13日 17:56
 */
public class CertificateMsg implements HandshakeMessage {


    /**
     * 要发送/接受的证书信息，最后一个是根证书，第一个是服务器证书
     */
    private X509Certificate[]   chain;

    /**
     * 证书链{@link #chain}编码后的信息，顺序与{@link #chain}一致
     */
    private List<byte[]>        encodedChain;

    /**
     * 消息长度，对应Certificates len
     */
    private int                 messageLength = -1;

    /**
     * 将X509Certificate[]解析为传输的数据List<byte[]>
     */
    private void doEncode() {
        this.encodedChain = new ArrayList<>(this.chain.length);

        try {
            X509Certificate[] certChain = this.chain;

            for (int i = 0; i < certChain.length; i++) {
                X509Certificate certificate = certChain[i];
                byte[] encode = certificate.getEncoded();
                this.encodedChain.add(encode);
                // 这里加3是因为Certificate消息中证书链里边除了放每个证书外，还有个单独的3byte的长度信息
                this.messageLength += encode.length + 3;
            }
        } catch (CertificateEncodingException exception) {
            this.encodedChain = null;
            throw new RuntimeException("Could not encode certificates", exception);
        }
    }

    @Override
    public int size() {
        throw new RuntimeException("未实现");
    }

    @Override
    public void write(WrapedOutputStream stream) throws IOException {
        stream.write(type().getCode());
        stream.writeInt24(messageLength);
        for (byte[] certData : encodedChain) {
            stream.writeInt24(certData.length);
            stream.write(certData);
        }
    }

    @Override
    public HandshakeType type() {
        return HandshakeType.CERTIFICATE;
    }
}
