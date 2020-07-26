package com.joe.ssl.crypto.exception;

/**
 * @author JoeKerouac
 * @version 2020年07月23日 16:49
 */
public class CryptoException extends RuntimeException {

    private static final long serialVersionUID = 4817590228113007040L;

    public CryptoException() {
        super();
    }

    public CryptoException(String message) {
        super(message);
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }

    public CryptoException(Throwable cause) {
        super(cause);
    }

    protected CryptoException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
