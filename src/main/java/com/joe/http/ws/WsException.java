package com.joe.http.ws;

/**
 * @author joe
 * @version 2018.08.21 14:46
 */
public class WsException extends RuntimeException {
    public WsException() {
        super();
    }

    public WsException(String message) {
        super(message);
    }

    public WsException(String message, Throwable cause) {
        super(message, cause);
    }

    public WsException(Throwable cause) {
        super(cause);
    }

    protected WsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
