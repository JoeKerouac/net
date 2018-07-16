package com.joe.http.request;

/**
 * Http请求方法
 * @author joe
 *
 */
public enum HttpMethod {
                        GET("GET"), POST("POST"), PUT("PUT"), DELETE("DELETE");
    private String method;

    private HttpMethod(String method) {
        this.method = method;
    }

    public String getMethod() {
        return this.method;
    }
}
