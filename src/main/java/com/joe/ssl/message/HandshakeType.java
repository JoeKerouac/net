package com.joe.ssl.message;

/**
 * 握手类型
 *
 * @author JoeKerouac
 * @version 2020年06月13日 16:49
 */
public enum HandshakeType {

                           CLIENT_HELLO(1),

                           SERVER_HELLO(2),

                           CERTIFICATE(11),

                           SERVER_KEY_EXCHANGE(12),

                           SERVER_HELLO_DONE(14),

                           CLIENT_KEY_EXCHANGE(16),

    ;

    private byte code;

    HandshakeType(int code) {
        this.code = (byte) code;
    }

    /**
     * Getter method for property <tt>code</tt>
     *
     * @return property value of code
     */
    public byte getCode() {
        return code;
    }

    public static HandshakeType getByCode(int code) {
        System.out.println("code 是：" + code);
        for (HandshakeType type : HandshakeType.values()) {
            if (code == type.code) {
                return type;
            }
        }
        return null;
    }
}
