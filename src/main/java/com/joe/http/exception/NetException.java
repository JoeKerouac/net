package com.joe.http.exception;

/**
 * @author JoeKerouac
 * @version $Id: joe, v 0.1 2018年11月21日 13:44 JoeKerouac Exp $
 */
public class NetException extends RuntimeException {

    private static final long serialVersionUID = -3749005762505354775L;

    public NetException() {
        super();
    }

    public NetException(String message) {
        super(message);
    }

    public NetException(String message, Throwable cause) {
        super(message, cause);
    }

    public NetException(Throwable cause) {
        super(cause);
    }

    protected NetException(String message, Throwable cause, boolean enableSuppression,
                           boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
