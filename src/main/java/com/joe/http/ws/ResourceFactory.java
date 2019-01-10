package com.joe.http.ws;

import com.joe.http.ws.core.HTTPProxy;
import com.joe.http.ws.core.ResourceType;
import com.joe.http.ws.exception.NotResourceException;
import com.joe.utils.common.Assert;
import com.joe.utils.proxy.ProxyClient;

import lombok.extern.slf4j.Slf4j;

/**
 * resource工厂，对于springresource，方法参数必须加上@RequestParam等注解声明参数从哪儿取得，否则框架没办法像spring那样读取class文件
 * 获取参数名
 *
 * @author joe
 * @version 2018.08.21 13:38
 */
@Slf4j
public class ResourceFactory {
    private String       baseUrl;
    private ResourceType resourceType;
    private ProxyClient  client;

    /**
     * 构造器
     * @param baseUrl 基础URL，例如http://localhost:8080
     * @param resourceType 代理的resource类型
     */
    public ResourceFactory(String baseUrl, ResourceType resourceType) {
        Assert.notNull(baseUrl, "baseUrl不能为null");
        Assert.notNull(resourceType, "resourceType不能为null");
        this.baseUrl = baseUrl;
        this.resourceType = resourceType;
        this.client = ProxyClient.getInstance(ProxyClient.ClientType.CGLIB);
    }

    /**
     * 构建指定resource的代理
     * @param t resource的class对象
     * @param <T> resource实际类型
     * @return resource代理
     * @throws NotResourceException 如果class对象不是一个resource那么抛出该异常
     */
    public <T> T build(Class<T> t) throws NotResourceException {
        return client.create(t, (target, params[], invoker, method) -> HTTPProxy.build(baseUrl, resourceType));
    }
}
