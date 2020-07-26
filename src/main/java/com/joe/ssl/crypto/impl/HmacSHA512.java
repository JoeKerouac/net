package com.joe.ssl.crypto.impl;

import com.joe.ssl.crypto.AlgorithmRegistry;

/**
 * @author JoeKerouac
 * @version 2020年07月24日 16:52
 */
public class HmacSHA512 extends AbstractHmac {

    public HmacSHA512() {
        super(AlgorithmRegistry.newInstance("SHA-512"), 128);
    }

    @Override
    public String name() {
        return "JoeHmacSHA512";
    }
}
