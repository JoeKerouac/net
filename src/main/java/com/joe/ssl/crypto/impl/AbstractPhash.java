package com.joe.ssl.crypto.impl;

import com.joe.ssl.crypto.HmacSpi;
import com.joe.ssl.crypto.PhashSpi;

import sun.plugin.dom.exception.InvalidStateException;

/**
 * Phash算法抽象，非线程安全
 *
 * @author JoeKerouac
 * @version 2020年07月27日 15:55
 */
public abstract class AbstractPhash implements PhashSpi {

    /**
     * 初始化标志，true表示已经初始化
     */
    private boolean init;

    /**
     * hmac算法实现
     */
    private HmacSpi hmacSpi;


    protected AbstractPhash(HmacSpi hmacSpi) {
        this.hmacSpi = hmacSpi;
    }

    @Override
    public String hmacAlgorithm() {
        return hmacSpi.name();
    }

    @Override
    public void init(byte[] key) {
        this.init = true;
        this.hmacSpi.init(key);
    }

    @Override
    public void phash(byte[] seed, byte[] output) {
        if (!init) {
            throw new InvalidStateException("PHASH未初始化，请先初始化");
        }

        int hmacSize = hmacSpi.macSize();
        byte[] tmp;
        byte[] aBytes = null;

        /*
         * compute:
         *
         *     P_hash(secret, seed) = HMAC_hash(secret, A(1) + seed) +
         *                            HMAC_hash(secret, A(2) + seed) +
         *                            HMAC_hash(secret, A(3) + seed) + ...
         * A() is defined as:
         *
         *     A(0) = seed
         *     A(i) = HMAC_hash(secret, A(i-1))
         */
        int remaining = output.length;
        int ofs = 0;

        while (remaining > 0) {

            /*
             * 计算A()
             */
            if (aBytes == null) {
                hmacSpi.update(seed);
            } else {
                hmacSpi.update(aBytes);
            }

            aBytes = hmacSpi.hmac();

            /*
             * 计算HMAC_hash()
             */
            hmacSpi.update(aBytes);
            hmacSpi.update(seed);
            tmp = hmacSpi.hmac();

            int k = Math.min(hmacSize, remaining);
            for (int i = 0; i < k; i++) {
                output[ofs++] ^= tmp[i];
            }

            remaining -= k;
        }
    }
}
