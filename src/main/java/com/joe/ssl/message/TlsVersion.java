package com.joe.ssl.message;

/**
 * TLS版本枚举
 * 
 * @author JoeKerouac
 * @version 2020年06月17日 20:27
 */
public enum TlsVersion implements EnumInterface {

                                                 TLS1_2(0x303, "TLS 1.2"),

    ;

    /**
     * 枚举code
     */
    private int    code;

    /**
     * 枚举英文名
     */
    private String englishName;

    /**
     * 枚举中文名
     */
    private String chinesName;

    static {
        EnumInterface.duplicateCheck(TlsVersion.class);
    }

    TlsVersion(int code, String englishName) {
        this.code = code;
        this.englishName = englishName;
        this.chinesName = englishName;
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
