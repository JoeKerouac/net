package com.joe.http.ws;

import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.Enhancer;

/**
 * resource工厂
 *
 * @author joe
 * @version 2018.08.21 13:38
 */
@Slf4j
public class ResourceFactory {
    private String       baseUrl;
    private ResourceType resourceType;

    /**
     * 构造器
     * @param baseUrl 基础URL，例如http://localhost:8080
     * @param resourceType 代理的resource类型
     */
    public ResourceFactory(String baseUrl, ResourceType resourceType) {
        this.baseUrl = baseUrl;
        this.resourceType = resourceType;
    }

    /**
     * 构建指定resource的代理
     * @param t resource的class对象
     * @param <T> resource实际类型
     * @return resource代理
     * @throws NotResourceException 如果class对象不是一个resource那么抛出该异常
     */
    public <T> T build(Class<T> t) throws NotResourceException {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(t);
        enhancer.setCallback(CglibHTTPProxy.build(baseUrl, resourceType));
        @SuppressWarnings("unchecked")
        T resource = (T) enhancer.create();
        return resource;
    }
}
