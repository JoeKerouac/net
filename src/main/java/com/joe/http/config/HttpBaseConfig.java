package com.joe.http.config;

import lombok.Data;

/**
 * 网络请求基本配置
 *
 * @author joe
 * @version 2018.04.28 14:55
 * @since 1.5
 */
@Data
public abstract class HttpBaseConfig {
    /**
     * 全局数据传输超时时间，单位毫秒，默认一分钟
     */
    private int socketTimeout            = 1000 * 60;
    /**
     * 全局连接超时，单位毫秒，默认5秒
     */
    private int connectTimeout           = 1000 * 30;
    /**
     * 全局连接请求超时，单位毫秒，默认5秒
     */
    private int connectionRequestTimeout = 1000 * 5;
}
