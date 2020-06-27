package com.joe.ssl.message;

import java.util.stream.Stream;

/**
 * 枚举接口
 *
 * @author JoeKerouac
 * @version 2020年06月17日 20:44
 */
public interface EnumInterface {

    /**
     * 枚举code，必须唯一
     * @return 枚举code
     */
    int getCode();

    /**
     * 枚举英文名
     * @return 英文名
     */
    String englishName();

    /**
     * 枚举中文名
     * @return 中文名
     */
    String chinesName();

    /**
     * 枚举说明
     * @return 枚举说明
     */
    default String description() {
        return chinesName();
    }

    /**
     * 根据code获取指定枚举
     * @param code 枚举code
     * @param clazz 枚举类型
     * @param <T> 枚举实际类型
     * @return 对应的枚举
     */
    static <T extends EnumInterface> T getByCode(int code, Class<T> clazz) {
        if (!clazz.isEnum()) {
            throw new IllegalArgumentException(String.format("类型[%s]不是枚举", clazz.getName()));
        }

        T[] enumInterfaces = clazz.getEnumConstants();
        for (T enumInterface : enumInterfaces) {
            if (enumInterface.getCode() == code) {
                return enumInterface;
            }
        }

        return null;
    }

    /**
     * 重复检查，检查枚举是否有重复值
     */
    static void duplicateCheck(Class<? extends EnumInterface> clazz) {
        if (!clazz.isEnum()) {
            throw new IllegalArgumentException(String.format("类型[%s]不是枚举", clazz.getName()));
        }

        EnumInterface[] enumInterfaces = clazz.getEnumConstants();
        if (enumInterfaces.length <= 1) {
            return;
        }
        int[] codes = Stream.of(enumInterfaces).mapToInt(EnumInterface::getCode).sorted().toArray();
        for (int i = 1; i < codes.length; i++) {
            if (codes[i - 1] == codes[i]) {
                throw new RuntimeException(String.format("枚举code[%d]重复", codes[i]));
            }
        }
    }
}
