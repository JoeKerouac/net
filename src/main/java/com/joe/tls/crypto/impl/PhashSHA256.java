package com.joe.tls.crypto.impl;

/**
 * @author JoeKerouac
 * @version 1.0
 * @date 2020-09-12 14:47
 */
public class PhashSHA256 extends AbstractPhash {

    public PhashSHA256() {
        super(new HmacSHA256());
    }

}
