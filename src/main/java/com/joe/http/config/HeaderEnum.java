package com.joe.http.config;

/**
 * @author JoeKerouac
 * @version $Id: joe, v 0.1 2019年04月08日 18:43 JoeKerouac Exp $
 */
public enum HeaderEnum {

                        /**
                         * Content_Type
                         */
                        CONTENT_TYPE("Content_Type");

    private String contentType;

    HeaderEnum(String contentType) {
        this.contentType = contentType;
    }

    /**
     * 获取header
     * @return header
     */
    public String getHeader() {
        return this.contentType;
    }
}
