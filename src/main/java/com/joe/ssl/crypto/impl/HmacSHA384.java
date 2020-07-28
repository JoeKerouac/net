package com.joe.ssl.crypto.impl;

import com.joe.ssl.crypto.AlgorithmRegistry;

/**
 * @author JoeKerouac
 * @version 2020年07月24日 16:52
 */
public class HmacSHA384 extends AbstractHmac {

    public HmacSHA384() {
        super(AlgorithmRegistry.newInstance("SHA-384"), 48, 128);
    }

    @Override
    public String name() {
        return "JoeHmacSHA384";
    }
}
