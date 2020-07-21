package com.joe.ssl.message;

/**
 * TLS版本枚举
 * 
 * @author JoeKerouac
 * @version 2020年06月17日 20:27
 */
public enum TlsVersion implements EnumInterface {

                                                 TLS1_2(0x0303, 0x03, 0x03, "TLS 1.2"),

    ;

    /**
     * 枚举code
     */
    private int    code;

    private int majorVersion;

    private int minorVersion;

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

    TlsVersion(int code, int majorVersion, int minorVersion, String englishName) {
        this.code = code;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
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
