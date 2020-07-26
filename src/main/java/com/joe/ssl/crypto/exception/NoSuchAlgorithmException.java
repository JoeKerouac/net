package com.joe.ssl.crypto.exception;

/**
 * @author JoeKerouac
 * @version 2020年07月23日 16:50
 */
public class NoSuchAlgorithmException extends CryptoException {

    private static final long serialVersionUID = -8414620513152606510L;

    public NoSuchAlgorithmException(String algorithm) {
        super(String.format("指定算法[%s]不存在", algorithm));
    }
}
