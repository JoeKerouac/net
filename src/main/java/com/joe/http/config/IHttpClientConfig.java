package com.joe.http.config;

import java.nio.charset.Charset;

import lombok.Data;

@Data
public final class IHttpClientConfig extends HttpBaseConfig{
    /**
     * 总共可以保持的连接数
     */
    private int maxTotal = 200;
    /**
     * 每个站点可以保持的最大连接数
     */
    private int defaultMaxPerRoute = 20;
    /**
     * socket发送缓冲
     */
    private int sndBufSize = 256;
    /**
     * socket接收缓冲
     */
    private int rcvBufSize = 256;
    /**
     * 默认连接编码字符集
     */
    private Charset charset = Charset.defaultCharset();
    /**
     * 用户代理
     */
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:49.0) Gecko/20100101 Firefox/49.0";
    /**
     * http代理
     */
    private HttpProxy proxy;
}
