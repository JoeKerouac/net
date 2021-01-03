package com.joe.http.ws.core;

import lombok.Data;

/**
 * 参数
 *
 * @author joe
 * @version 2018.08.21 14:00
 */
@Data
public class ResourceParam {
    /**
     * 参数值
     */
    private Object param;
    /**
     * 参数类型
     */
    private Type type;
    /**
     * 参数名
     */
    private String name;
    /**
     * 参数在method声明的下标，从0开始
     */
    private int index;

    public enum Type {
        PATH, FORM, QUERY, HEADER, CONTEXT, JSON
    }
}
