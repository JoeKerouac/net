package com.joe.tls.crypto.impl;

/**
 * @author JoeKerouac
 * @version 2020年07月24日 16:52
 */
public class HmacSHA384 extends AbstractHmac {

    public HmacSHA384() {
        super(new DigestSHA384(), 48, 128);
    }

}
