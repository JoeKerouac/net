package com.joe.http.config;

import lombok.Data;

/**
 * 代理服务器
 * 
 * @author joe
 *
 */
@Data
public final class HttpProxy {
    // 主机
    private String host;
    // 端口
    private int port;

    public HttpProxy(String host, int port) {
        this.host = host;
        this.port = port;
    }
}
