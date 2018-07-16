package com.joe.http.response;

import org.apache.http.Header;

/**
 * 请求头
 *
 * @author joe
 */
public class IHeader {
    private Header header;

    IHeader(Header header) {
        this.header = header;
    }

    public String getName() {
        return header.getName();
    }

    public String getValue() {
        return header.getValue();
    }
}
