package com.joe.tls;

import java.util.Arrays;

/**
 * TLS消息身份认证令牌，该令牌封装序列号并确保可以检测到删除或重新排序消息的尝试。该令牌针对读取和写入分别维护一个，序列号必须从0开始；
 *
 * @author JoeKerouac
 * @data 2020-11-05 23:34
 */
public class Authenticator {

    /**
     * 令牌数据块
     */
    private final byte[] block;

    public Authenticator(TlsVersion version) {
        //sequence number + record type + protocol version + record length
        block = new byte[8 + 1 + 2 + 2];
        block[9] = (byte) version.getMajorVersion();
        block[10] = (byte) version.getMinorVersion();
    }

    /**
     * 获取sequence number数据，总共8byte
     * @return sequence number数据，数组长度8
     */
    public final byte[] sequenceNumber() {
        return Arrays.copyOf(block, 8);
    }

    /**
     * 获取一个认证数据，在数据做mac/认证（AEAD模式）的时候需要用
     * @param type content type
     * @param length 要写出record数据的长度，加密前的的，不是加密后的，同时不包括record的header部分
     * @return
     */
    public final byte[] acquireAuthenticationBytes(byte type, int length) {
        // 这里长度是原始数据长度，不是加密后的，不带IV
        // 读取record和写出record的时候各会调用两次，分别是加密/解密的时候生成/读取nonce的时候会调用一次，然后在mac计算的时候
        byte[] copy = block.clone();

        if (block.length != 0) {
            copy[8] = type;
            // 最后两位设置为长度字段
            copy[copy.length - 2] = (byte) (length >> 8);
            copy[copy.length - 1] = (byte) (length);

            int k = 7;
            // 这里就是将sequence number + 1，等于0的情况是需要进位了，所以需要再执行
            while ((k >= 0) && (++block[k] == 0)) {
                k--;
            }
        }

        return copy;
    }

}
