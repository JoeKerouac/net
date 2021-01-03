package com.joe.http.config;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Http请求配置，一个请求一个，全局配置可以通过{@link com.joe.http.config.IHttpClientConfig IHttpClientConfig}来配置
 *
 * @author joe
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IHttpConfig extends HttpBaseConfig {}
