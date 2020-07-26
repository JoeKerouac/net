package com.joe.ssl.crypto.impl;

import com.joe.ssl.crypto.AlgorithmRegistry;

/**
 * @author JoeKerouac
 * @version 2020年07月24日 16:52
 */
public class HmacSHA256 extends AbstractHmac {

    public HmacSHA256() {
        super(AlgorithmRegistry.newInstance("SHA-256"), 64);
    }

    @Override
    public String name() {
        return "JoeHmacSHA256";
    }
}
