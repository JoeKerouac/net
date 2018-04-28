package com.joe.http.config;

import lombok.Data;

/**
 * Http请求配置，一个请求一个，全局配置可以通过{@link com.joe.http.config.IHttpClientConfig IHttpClientConfig}来配置
 *
 * @author joe
 */
@Data
public class IHttpConfig extends HttpBaseConfig {
}
