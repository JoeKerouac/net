package com.joe.tls.crypto.impl;

/**
 * @author JoeKerouac
 * @version 2020年07月24日 16:52
 */
public class HmacSHA1 extends AbstractHmac {

    public HmacSHA1() {
        super(new DigestSHA1(), 20, 64);
    }

}
