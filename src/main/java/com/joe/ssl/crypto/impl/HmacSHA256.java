package com.joe.ssl.crypto.impl;

/**
 * @author JoeKerouac
 * @version 2020年07月24日 16:52
 */
public class HmacSHA256 extends AbstractHmac {

    public HmacSHA256() {
        super(new DigestSHA256(), 32, 64);
    }

}
