package com.joe.tls;

import com.joe.tls.crypto.HmacSpi;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-11-08 16:17
 */
public class MacAuthenticator extends Authenticator {

    private HmacSpi hmacSpi;

    private Authenticator authenticator;

    public MacAuthenticator(TlsVersion version, Authenticator authenticator, HmacSpi hmacSpi) {
        super(version);
        this.authenticator = authenticator;
        this.hmacSpi = hmacSpi;
    }

    public int macLen() {
        return hmacSpi.macSize();
    }

    public int hashBlockLen() {
        return hmacSpi.blockLen();
    }

    /**
     * 计算认证信息
     * 
     * @param type
     *            content type
     * @param data
     *            要写出的数据
     * @param offset
     *            数据起始位置
     * @param len
     *            要写出的数据长度
     * @param isSimulated
     *            是否是模拟计算，写出数据时不时模拟计算，读取数据时是模拟计算，区别就是如果是false，表示不是模拟计算，则会从令牌生 成器中获取一个令牌，同时将当前的sequenceNumber+1
     * @return 认证信息
     */
    public byte[] compute(byte type, byte[] data, int offset, int len, boolean isSimulated) {
        if (isSimulated) {
            hmacSpi.update(authenticator.acquireAuthenticationBytes(type, len));
        }
        hmacSpi.update(data, offset, len);
        return hmacSpi.doFinal();
    }
}
