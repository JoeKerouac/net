package com.joe.http.config;

import lombok.Data;

/**
 * Http配置
 * 
 * @author joe
 *
 */
@Data
public class IHttpConfig {
	// 数据传输超时时间，单位毫秒，默认一分钟
	private int socketTimeout;
	// 连接超时，单位毫秒，默认5秒
	private int connectTimeout;
	// 连接请求超时，单位毫秒，默认10秒
	private int connectionRequestTimeout;

	public IHttpConfig() {
		this.socketTimeout = 1000 * 60;
		this.connectTimeout = 1000 * 30;
		this.connectionRequestTimeout = 1000 * 30;
	}
}
