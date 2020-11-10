package com.joe.tls.crypto.impl;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-12 14:47
 */
public class PhashSHA384 extends AbstractPhash {

    public PhashSHA384() {
        super(new HmacSHA384());
    }

}
