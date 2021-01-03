package com.joe.http.ws.core;

/**
 * 资源解析
 *
 * @author joe
 * @version 2018.08.21 13:58
 */
public interface ResourceAnalyze {
    ResourceParam[] EMPTY = new ResourceParam[0];

    /**
     * 是否是资源
     * 
     * @return true表示是资源，false表示不是
     */
    boolean isResource();

    /**
     * path前缀
     * 
     * @return path前缀
     */
    String pathPrefix();

    /**
     * path结尾
     * 
     * @return path结尾
     */
    String pathLast();

    /**
     * 获取参数列表
     * 
     * @return 参数列表
     */
    ResourceParam[] getParams();

    /**
     * 获取请求方法
     * 
     * @return 请求方法
     */
    ResourceMethod getResourceMethod();

    /**
     * 获取请求contentType
     * 
     * @return 请求contentType
     */
    String[] getRequestContentTypes();

    /**
     * 获取响应contentType
     * 
     * @return 响应contentType
     */
    String[] getResponseContentTypes();
}
