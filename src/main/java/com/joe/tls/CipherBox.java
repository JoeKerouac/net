package com.joe.tls;

import java.security.SecureRandom;
import java.util.Arrays;

import com.joe.tls.cipher.CipherSuite;
import com.joe.tls.crypto.CipherSpi;
import com.joe.tls.crypto.impl.AesCipher;
import com.sun.crypto.provider.SunJCE;

/**
 * 加密盒子
 * 
 * @author JoeKerouac
 * @data 2020-11-06 22:24
 */
public class CipherBox {

    /**
     * 加密key
     */
    private final byte[] cipherKey;

    /**
     * fixedIv，AEAD模式下有用
     */
    private final byte[] fixedIv;

    /**
     * 经过record传送的IV数据大小，AEAD模式有
     */
    private final int recordIvSize;

    /**
     * AEAD模式有该值
     */
    private final int tagSize;

    /**
     * 加密说明
     */
    private final CipherSuite.CipherDesc cipherDesc;

    /**
     * 安全随机数
     */
    private final SecureRandom secureRandom;

    /**
     * 加密模式
     */
    private final int mode;

    /**
     * 加密接口
     */
    private final CipherSpi cipherSpi;

    public CipherBox(SecureRandom secureRandom, CipherSuite.CipherDesc cipherDesc, byte[] cipherKey, byte[] iv,
        boolean encrypt) {
        this.secureRandom = secureRandom;
        this.cipherDesc = cipherDesc;
        this.cipherKey = cipherKey;

        if (encrypt) {
            this.mode = CipherSpi.ENCRYPT_MODE;
        } else {
            this.mode = CipherSpi.DECRYPT_MODE;
        }

        // this.cipherSpi = CipherSpi.getInstance(cipherDesc);
        this.cipherSpi = new AesCipher(cipherDesc, new SunJCE());
        if (cipherDesc.getCipherType() == CipherSuite.CipherType.AEAD) {
            this.tagSize = 16;
            this.fixedIv = iv;
            this.recordIvSize = cipherDesc.getIvLen() - cipherDesc.getFixedIvLen();
        } else {
            this.tagSize = 0;
            this.cipherSpi.init(cipherKey, iv, mode, secureRandom);
            this.fixedIv = new byte[0];
            this.recordIvSize = 0;
        }
    }

    /**
     * 获取该加密器的tagSize
     * 
     * @return tagSize
     */
    public int getTagSize() {
        return tagSize;
    }

    /**
     * 获取算法说明
     * 
     * @return 算法说明
     */
    public CipherSuite.CipherDesc getCipherDesc() {
        return cipherDesc;
    }

    /**
     * 对数据进行加密
     * 
     * @param data
     *            需要加密的数据
     * @param offset
     *            数据起始位置
     * @param len
     *            数据长度
     * @return 加密后的数据
     */
    public byte[] encrypt(byte[] data, int offset, int len) {
        if (cipherDesc.getCipherType() == CipherSuite.CipherType.BLOCK) {
            // 如果是block模式，要对齐
            int blockSize = cipherSpi.getBlockSize();
            data = addPadding(data, offset, len, blockSize);
        }

        if (cipherDesc.getCipherType() == CipherSuite.CipherType.BLOCK) {
            return cipherSpi.update(data, offset, len);
        } else if (cipherDesc.getCipherType() == CipherSuite.CipherType.AEAD) {
            return cipherSpi.doFinal(data, offset, len);
        } else {
            throw new RuntimeException("不支持的加密模式：" + cipherDesc);
        }
    }

    /**
     * 对数据进行解密
     * 
     * @param data
     *            需要加密的数据
     * @param offset
     *            数据起始位置
     * @param len
     *            数据长度
     * @return 加密后的数据
     */
    public byte[] decrypt(byte[] data, int offset, int len) {

        if (cipherDesc.getCipherType() == CipherSuite.CipherType.BLOCK) {
            byte[] result = cipherSpi.update(data, offset, len);
            return removePadding(result, 0, result.length);
        } else if (cipherDesc.getCipherType() == CipherSuite.CipherType.AEAD) {
            return cipherSpi.doFinal(data, offset, len);
        } else {
            throw new RuntimeException("不支持的加密模式：" + cipherDesc);
        }
    }

    /**
     * 写出数据时使用，创建一个随机数，AEAD模式下该随机数会放在record消息的最前边不加密写出
     * 
     * @param authenticator
     *            写出（加密）令牌
     * @param contentType
     *            contentType
     * @param fragmentLength
     *            要加密的数据的长度
     * @return 随机数
     */
    public byte[] createExplicitNonce(Authenticator authenticator, byte contentType, int fragmentLength) {
        byte[] nonce;

        if (cipherDesc.getCipherType() == CipherSuite.CipherType.BLOCK) {
            nonce = new byte[cipherSpi.getBlockSize()];
            secureRandom.nextBytes(nonce);
        } else if (cipherDesc.getCipherType() == CipherSuite.CipherType.AEAD) {
            nonce = authenticator.sequenceNumber();

            // AEAD模式下在这里初始化IV
            byte[] iv = Arrays.copyOf(fixedIv, fixedIv.length + nonce.length);
            System.arraycopy(nonce, 0, iv, fixedIv.length, nonce.length);
            cipherSpi.init(cipherKey, iv, mode, secureRandom);

            // 添加附加认证信息
            byte[] aad = authenticator.acquireAuthenticationBytes(contentType, fragmentLength);
            cipherSpi.updateAAD(aad);
        } else {
            throw new RuntimeException("不支持的加密模式：" + cipherDesc);
        }

        return nonce;
    }

    /**
     * 读取数据时使用，获取随机数的长度，同时如果是AEAD模式，会顺便初始化cipher，把令牌中的sequenceNumber+1
     * 
     * @param authenticator
     *            读取（解密）令牌
     * @param contentType
     *            contentType
     * @param data
     *            record层消息体，主要是AEAD模式为了从里边读取iv使用
     * @return record消息中随机数的长度
     */
    public int applyExplicitNonce(Authenticator authenticator, byte contentType, byte[] data) {
        if (cipherDesc.getCipherType() == CipherSuite.CipherType.BLOCK) {
            return cipherSpi.getBlockSize();
        } else if (cipherDesc.getCipherType() == CipherSuite.CipherType.AEAD) {
            byte[] iv = Arrays.copyOf(fixedIv, fixedIv.length + recordIvSize);
            System.arraycopy(data, 0, iv, fixedIv.length, recordIvSize);
            cipherSpi.init(cipherKey, iv, mode, secureRandom);

            // 添加附加认证信息
            // 注意，这里使用data.length - recordIvSize - tagSize作为长度，是因为我们只需要解密后的数据长度，而解密后的数据长度就是
            // record消息长度 - record层iv（nonce）长度 - AEAD模式增加的认证消息长度（tagSize）
            byte[] aad = authenticator.acquireAuthenticationBytes(contentType, data.length - recordIvSize - tagSize);
            cipherSpi.updateAAD(aad);
            return recordIvSize;
        } else {
            throw new RuntimeException("不支持的加密模式：" + cipherDesc);
        }
    }

    /**
     * 对要加密的数据padding，padding规则：最少增加1byte的pad，padding后需要满足长度是blockSize的整数倍
     * 
     * @param buf
     *            要填充的数据
     * @param offset
     *            真实数据起始位置
     * @param len
     *            数据长度
     * @param blockSize
     *            cipher的blockSize
     * @return 填充后的数据
     */
    private static byte[] addPadding(byte[] buf, int offset, int len, int blockSize) {
        int newLen = len + 1;

        if ((newLen % blockSize) != 0) {
            newLen += blockSize - 1;
            newLen -= newLen % blockSize;
        }

        byte[] paddingResult = Arrays.copyOf(buf, newLen);
        byte pad = (byte)(newLen - len);

        int i;
        for (i = 0, offset += len; i < pad; i++) {
            paddingResult[offset++] = (byte)(pad - 1);
        }

        return paddingResult;
    }

    /**
     * 移除padding
     * 
     * @param buf
     *            数据
     * @param offset
     *            数据的起始位置
     * @param len
     *            数据长度
     * @return 移除padding后的数据
     */
    private static byte[] removePadding(byte[] buf, int offset, int len) {

        // 上边的padding算法保证我们数据的最后一位肯定是pad，只需要取出即可
        int padOffset = offset + len - 1;
        int padLen = buf[padOffset] & 0xFF;

        // 减去pad后的长度
        int newLen = len - (padLen + 1);

        // 检查padding
        int[] results = checkPadding(buf, offset + newLen, padLen + 1, (byte)(padLen & 0xFF));
        if (results[0] != 0) { // padding data has invalid bytes
            throw new RuntimeException("Invalid TLS padding data");
        }

        byte[] result = new byte[newLen];
        System.arraycopy(buf, 0, result, 0, result.length);
        return result;
    }

    /**
     * 检查padding是否正确
     * 
     * @param buf
     *            缓冲数据
     * @param offset
     *            pad起始位置，注意：这里是pad的起始位置
     * @param len
     *            pad的长度
     * @param pad
     *            pad
     * @return 长度为2的数组，数组第一个数字表示不匹配pad的数据量，单位byte，第二个数字表示匹配上pad的数据量，单位byte，正常情况第一个应该为0
     */
    private static int[] checkPadding(byte[] buf, int offset, int len, byte pad) {

        if (len <= 0) {
            throw new RuntimeException("padding len must be positive");
        }

        int[] results = {0, 0}; // {missed #, matched #}
        // 这里代码表示pad长度最长不能超过256
        for (int j = 0, i = 0; j < len && i <= 256; j++, i++) { // j <= i
            if (buf[offset + j] != pad) {
                results[0]++; // mismatched padding data
            } else {
                results[1]++; // matched padding data
            }
        }

        return results;
    }

}
