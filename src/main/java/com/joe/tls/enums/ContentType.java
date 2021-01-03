package com.joe.tls.enums;

import com.joe.tls.EnumInterface;

/**
 * @author JoeKerouac
 * @version 2020年06月17日 20:25
 */
public enum ContentType implements EnumInterface {

    CHANGE_CIPHER_SPEC(0x14, "CHANGE_CIPHER_SPEC", "变更到加密通道消息"),

    ALTER(0x15, "ALTER", "警告信息"),

    HANDSHAKE(0x16, "HANDSHAKE", "握手类型"),

    APPLICATION_DATA(0x17, "APPLICATION_DATA", "应用数据"),

    ;

    /**
     * 枚举code
     */
    private int code;

    /**
     * 枚举英文名
     */
    private String englishName;

    /**
     * 枚举中文名
     */
    private String chinesName;

    static {
        EnumInterface.duplicateCheck(ContentType.class);
    }

    ContentType(int code, String englishName, String chinesName) {
        this.code = code;
        this.englishName = englishName;
        this.chinesName = chinesName;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String englishName() {
        return englishName;
    }

    @Override
    public String chinesName() {
        return chinesName;
    }
}
