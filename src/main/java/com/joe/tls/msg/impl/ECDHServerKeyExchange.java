package com.joe.tls.msg.impl;

import java.nio.ByteBuffer;

import org.bouncycastle.crypto.params.ECDomainParameters;

import com.joe.tls.enums.HandshakeType;
import com.joe.tls.enums.NamedCurve;
import com.joe.tls.msg.HandshakeProtocol;
import com.joe.tls.util.ByteBufferUtil;
import com.joe.utils.common.Assert;

/**
 * 服务端ECDH密钥交换消息
 *
 * @author JoeKerouac
 * @data 2020-11-02 21:06
 */
public class ECDHServerKeyExchange implements HandshakeProtocol {

    /**
     * 消息总长度，包含header
     */
    private int    msgLen;

    /**
     * 曲线类型，目前必须时03
     */
    private byte   curveType;

    /**
     * 曲线ID
     */
    private int    curveId;

    /**
     * 服务端ECDH公钥
     */
    private byte[] publicKey;

    /**
     * hash和签名算法
     */
    private int    hashAndSigAlg;

    /**
     * 签名
     */
    private byte[] sig;

    public ECDHServerKeyExchange(ByteBuffer buffer) {
        // 先给type和len丢弃掉
        ByteBufferUtil.mergeReadInt24(buffer);
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
        // 4byte header + 1byte curve type + 2byte curve id + 1byte public key len + public key 
        // + 2byte sigAndHashAlg + 2byte sig len + sig
        return 4 + 1 + 2 + 1 + publicKey.length + 2 + 2 + sig.length;
    }

    @Override
    public byte[] serialize() {
        throw new RuntimeException("为实现");
    }
}
