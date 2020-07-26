package com.joe.ssl.crypto.impl;

import com.joe.ssl.crypto.AlgorithmRegistry;

/**
 * @author JoeKerouac
 * @version 2020年07月24日 16:52
 */
public class HmacSHA224 extends AbstractHmac {

    public HmacSHA224() {
        super(AlgorithmRegistry.newInstance("SHA-224"), 64);
    }

    @Override
    public String name() {
        return "JoeHmacSHA224";
    }
}
