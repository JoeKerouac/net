package com.joe.ssl.crypto.impl;

import com.joe.ssl.crypto.AlgorithmRegistry;

/**
 * @author JoeKerouac
 * @version 2020年07月24日 16:52
 */
public class HmacSHA1 extends AbstractHmac {

    public HmacSHA1() {
        super(AlgorithmRegistry.newInstance("SHA-1"), 64);
    }

    @Override
    public String name() {
        return "JoeHmacSHA1";
    }
}
