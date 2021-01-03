package com.joe.tls.msg.impl;

import java.nio.ByteBuffer;

import org.bouncycastle.crypto.params.ECDomainParameters;

import com.joe.tls.enums.HandshakeType;
import com.joe.tls.enums.NamedCurve;
import com.joe.tls.msg.HandshakeProtocol;
import com.joe.tls.util.ByteBufferUtil;
import com.joe.utils.common.Assert;

import lombok.Getter;

/**
 * 服务端ECDH密钥交换消息
 *
 * @author JoeKerouac
 * @data 2020-11-02 21:06
 */
public class ECDHServerKeyExchange implements HandshakeProtocol {

    /**
     * 曲线类型，目前必须时03
     */
    @Getter
    private final byte curveType;

    /**
     * 曲线ID
     */
    @Getter
    private final int curveId;

    /**
     * 服务端ECDH公钥
     */
    @Getter
    private final byte[] publicKey;

    /**
     * hash和签名算法
     */
    @Getter
    private final int hashAndSigAlg;

    /**
     * 签名数据，对clientRandom + serverRandom + curveType（byte） + curveId（byte） + publicKeyLen（byte） + publicKey进行签名 <br/>
     * publicKey指的是本消息中的服务端ECDH公钥
     */
    @Getter
    private final byte[] sig;

    public ECDHServerKeyExchange(byte curveType, int curveId, byte[] publicKey, int hashAndSigAlg, byte[] sig) {
        this.curveType = curveType;
        this.curveId = curveId;
        this.publicKey = publicKey;
        this.hashAndSigAlg = hashAndSigAlg;
        this.sig = sig;
    }

    public ECDHServerKeyExchange(ByteBuffer buffer) {
        // 先给type和len丢弃掉
        ByteBufferUtil.mergeReadInt32(buffer);
        curveType = buffer.get();
        Assert.isTrue(curveType == 3);
        curveId = ByteBufferUtil.mergeReadInt16(buffer);

        // 获取指定曲线相关参数
        ECDomainParameters domainParameters = NamedCurve.getECParameters(curveId);

        // 如果等于null表示不支持
        if (domainParameters == null) {
            throw new RuntimeException(String.format("不支持的椭圆曲线id：%d", curveId));
        }

        publicKey = ByteBufferUtil.getInt8(buffer);
        hashAndSigAlg = ByteBufferUtil.mergeReadInt16(buffer);
        sig = ByteBufferUtil.getInt16(buffer);
    }

    @Override
    public HandshakeType type() {
        return HandshakeType.SERVER_KEY_EXCHANGE;
    }

    @Override
    public int len() {
        // 1byte curve type + 2byte curve id + 1byte public key len + public key
        // + 2byte sigAndHashAlg + 2byte sig len + sig
        return 1 + 2 + 1 + publicKey.length + 2 + 2 + sig.length;
    }

    @Override
    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[4 + len()]);
        ByteBufferUtil.writeInt8(type().getCode(), buffer);
        ByteBufferUtil.writeInt24(len(), buffer);
        ByteBufferUtil.writeInt8(curveType, buffer);
        ByteBufferUtil.writeInt16(curveId, buffer);
        ByteBufferUtil.putBytes8(publicKey, buffer);
        ByteBufferUtil.writeInt16(hashAndSigAlg, buffer);
        ByteBufferUtil.putBytes16(sig, buffer);
        return buffer.array();
    }
}
