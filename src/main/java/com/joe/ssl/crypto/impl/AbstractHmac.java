package com.joe.ssl.crypto.impl;

import java.util.Arrays;

import com.joe.ssl.crypto.DigestSpi;
import com.joe.ssl.crypto.HmacSpi;
import com.joe.ssl.crypto.exception.InvalidKeyException;

import sun.plugin.dom.exception.InvalidStateException;

/**
 * @author JoeKerouac
 * @version 2020年07月23日 16:46
 */
public abstract class AbstractHmac implements HmacSpi {

    /**
     * 初始化标志，true表示已经初始化
     */
    private boolean   init;

    /**
     * 摘要算法实现
     */
    private DigestSpi digestSpi;

    /**
     * 当前是否是第一次更新
     */
    private boolean   first;

    /**
     * 块长度
     */
    private int       blockLen;

    /**
     * 对应算法中的K XOR ipad
     */
    private byte[]    k_ipad;

    /**
     * 对应算法中的K XOR opad
     */
    private byte[]    k_opad;

    protected AbstractHmac(DigestSpi digestSpi, int blockLen) {
        this.digestSpi = digestSpi;
        this.first = true;
        this.blockLen = blockLen;
        this.k_ipad = new byte[blockLen];
        this.k_opad = new byte[blockLen];
    }

    @Override
    public void init(byte[] key) {
        reset();

        if (key == null) {
            throw new InvalidKeyException("Missing key data");
        }

        byte[] keyClone = key.clone();

        // 对key生成摘要
        if (keyClone.length > this.blockLen) {
            byte[] digest = this.digestSpi.digest(keyClone);
            // 尽快将内存中的key清空，防止密钥泄漏
            Arrays.fill(keyClone, (byte) 0);
            keyClone = digest;
        }

        // 根据rfc2104生成k_ipad和k_opad
        for (int i = 0; i < this.blockLen; ++i) {
            byte var5 = i < keyClone.length ? keyClone[i] : 0;
            this.k_ipad[i] = (byte) (var5 ^ 0x36);
            this.k_opad[i] = (byte) (var5 ^ 0x5C);
        }

        // 将内存中的数据尽快清空
        Arrays.fill(keyClone, (byte) 0);
        this.init = true;
    }

    @Override
    public void update(byte[] data) {
        if (!init) {
            throw new InvalidStateException("HMAC未初始化，请先初始化");
        }

        if (this.first) {
            this.digestSpi.update(this.k_ipad);
            this.first = false;
        }
        this.digestSpi.update(data);
    }

    @Override
    public byte[] doFinal() {
        if (!init) {
            throw new InvalidStateException("HMAC未初始化，请先初始化");
        }

        if (this.first) {
            this.digestSpi.update(this.k_ipad);
        } else {
            this.first = true;
        }

        byte[] result = this.digestSpi.digest();
        this.digestSpi.update(this.k_opad);
        this.digestSpi.update(result);
        this.digestSpi.digest(result, 0);
        return result;
    }

    @Override
    public void reset() {
        if (!this.first) {
            this.digestSpi.reset();
            this.first = true;
        }
    }

    @Override
    public String hashAlgorithm() {
        return digestSpi.name();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        /**
         * 初始化标志，true表示已经初始化
         */
        private boolean   init;

        /**
         * 摘要算法实现
         */
        private DigestSpi digestSpi;

        /**
         * 当前是否是第一次更新
         */
        private boolean   first;

        /**
         * 块长度
         */
        private int       blockLen;

        /**
         * 对应算法中的K XOR ipad
         */
        private byte[]    k_ipad;

        /**
         * 对应算法中的K XOR opad
         */
        private byte[]    k_opad;

        try {
            AbstractHmac hmac = (AbstractHmac)super.clone();
            hmac.digestSpi = (DigestSpi)this.digestSpi.clone();
            hmac.init = this.init;
            hmac.k_ipad = this.k_ipad.clone();
            hmac.k_opad = this.k_opad.clone();
        } catch (InstantiationException |IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
