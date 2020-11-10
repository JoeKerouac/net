package com.joe.tls.crypto.exception;

/**
 * @author JoeKerouac
 * @version 2020年07月23日 17:00
 */
public class InvalidKeyException extends CryptoException {

    private static final long serialVersionUID = -7766591968410079272L;

    public InvalidKeyException(String message) {
        super(message);
    }
}
