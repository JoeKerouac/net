package com.joe.tls.crypto.impl;

/**
 * 基于MD5的HMAC算法
 * 
 * @author JoeKerouac
 * @version 2020年07月23日 16:07
 */
public class HmacMD5 extends AbstractHmac {

    public HmacMD5() {
        super(new DigestMD5(), 16, 64);
    }

    @Override
    public String name() {
        return "HmacMD5";
    }
}
