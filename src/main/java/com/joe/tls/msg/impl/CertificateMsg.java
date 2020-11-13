package com.joe.tls.msg.impl;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

import com.joe.tls.enums.HandshakeType;
import com.joe.tls.msg.HandshakeProtocol;
import com.joe.tls.util.ByteBufferUtil;

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
public class CertificateMsg implements HandshakeProtocol {

    /**
     * 要发送/接受的证书信息，最后一个是根证书，第一个是服务器证书
     */
    private Certificate[] chain;

    /**
     * 证书链{@link #chain}编码后的信息，顺序与{@link #chain}一致
     */
    private List<byte[]>  encodedChain  = new ArrayList<>();

    /**
     * 该消息总长度，包含header
     */
    private int           messageLength = -1;

    public CertificateMsg(ByteBuffer buffer) {
        // 跳过类型和长度，刚好是4byte
        ByteBufferUtil.mergeReadInt32(buffer);
        // 跳过证书总长度
        ByteBufferUtil.mergeReadInt24(buffer);

        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new RuntimeException(e);
        }

        List<Certificate> chain = new ArrayList<>();
        // 只要还有数据就继续
        while (buffer.position() < buffer.limit()) {
            byte[] data = ByteBufferUtil.getInt24(buffer);
            this.encodedChain.add(data);

            try {
                chain.add(cf.generateCertificate(new ByteArrayInputStream(data)));
            } catch (CertificateException e) {
                throw new RuntimeException(e);
            }
        }

        this.chain = chain.toArray(new Certificate[0]);
    }

    public CertificateMsg(Certificate[] certificates) {
        this.encodedChain = new ArrayList<>(certificates.length / 3 * 4 + 1);
        this.chain = certificates.clone();

        // 1byte type + 3byte len + 3byte certificate len
        this.messageLength += 7;
        try {
            for (int i = 0; i < certificates.length; i++) {
                Certificate certificate = certificates[i];
                byte[] encode = certificate.getEncoded();
                this.encodedChain.add(encode);
                // 这里加3是因为Certificate消息中证书链里边除了放每个证书外，还有个单独的3byte的长度信息
                this.messageLength += encode.length + 3;
            }
        } catch (CertificateEncodingException exception) {
            this.encodedChain = null;
            this.chain = null;
            throw new RuntimeException("Could not encode certificates", exception);
        }
    }

    /**
     * 获取服务端证书公钥
     * @return 服务端证书公钥
     */
    public PublicKey getPublicKey() {
        return chain[0].getPublicKey();
    }

    @Override
    public HandshakeType type() {
        return HandshakeType.CERTIFICATE;
    }

    @Override
    public int len() {
        return 3 + 3 * encodedChain.size()
               + encodedChain.stream().mapToInt(arr -> arr.length).sum();
    }

    @Override
    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[4 + len()]);
        ByteBufferUtil.writeInt8(type().getCode(), buffer);
        ByteBufferUtil.writeInt24(len(), buffer);
        ByteBufferUtil.writeInt24(len() - 3, buffer);
        encodedChain.forEach(cert -> ByteBufferUtil.putBytes24(cert, buffer));
        return buffer.array();
    }
}
