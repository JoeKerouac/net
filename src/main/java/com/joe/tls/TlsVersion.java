package com.joe.tls;

/**
 * TLS版本枚举
 * 
 * @author JoeKerouac
 * @version 2020年06月17日 20:27
 */
public enum TlsVersion implements EnumInterface {

                                                 TLS1_0(0x0301, 0x03, 0x01, "TLS 1.0"),

                                                 TLS1_1(0x0302, 0x03, 0x02, "TLS 1.1"),

                                                 TLS1_2(0x0303, 0x03, 0x03, "TLS 1.2"),

    ;

    /**
     * 枚举code
     */
    private final int    code;

    private final int    majorVersion;

    private final int    minorVersion;

    /**
     * 枚举英文名
     */
    private final String englishName;

    /**
     * 枚举中文名
     */
    private final String chinesName;

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

    public static TlsVersion valueOf(int majorVersion, int minorVersion) {
        for (TlsVersion value : TlsVersion.values()) {
            if (value.majorVersion == majorVersion && value.minorVersion == minorVersion) {
                return value;
            }
        }

        return null;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getChinesName() {
        return chinesName;
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
