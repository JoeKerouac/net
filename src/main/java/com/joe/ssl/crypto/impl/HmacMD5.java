package com.joe.ssl.crypto.impl;

import com.joe.ssl.crypto.AlgorithmRegistry;

/**
 * 基于MD5的HMAC算法
 * 
 * @author JoeKerouac
 * @version 2020年07月23日 16:07
 */
public class HmacMD5 extends AbstractHmac {

    public HmacMD5() {
        super(AlgorithmRegistry.newInstance("MD5"), 64);
    }

    @Override
    public String name() {
        return "JoeHmacMD5";
    }
}
