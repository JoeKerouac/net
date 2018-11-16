package com.joe.http.ws.core;

import static com.joe.utils.parse.json.JsonParser.getInstance;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.joe.http.client.IHttpClient;
import com.joe.http.request.IHttpGet;
import com.joe.http.request.IHttpPost;
import com.joe.http.request.IHttpRequestBase;
import com.joe.http.response.IHttpResponse;
import com.joe.http.ws.exception.NotResourceException;
import com.joe.http.ws.exception.WsException;
import com.joe.utils.common.Assert;
import com.joe.utils.common.StringUtils;
import com.joe.utils.parse.json.JsonParser;
import com.joe.utils.proxy.Interception;
import com.joe.utils.proxy.Invoker;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author JoeKerouac
 * @version $Id: joe, v 0.1 2018年11月13日 22:55 JoeKerouac Exp $
 */
@Slf4j
public class HTTPProxy implements Interception {

    private static final JsonParser                JSON_PARSER = getInstance();
    private static final IHttpClient               CLIENT      = IHttpClient.builder().build();
    private static final Map<Key, HTTPProxy>       CACHE       = new ConcurrentHashMap<>();
    private String                                 baseUrl;
    private ResourceAnalyze analyze;
    private Constructor<? extends ResourceAnalyze> constructor;

    private HTTPProxy(String baseUrl, ResourceType resourceType) {
        Assert.notNull(baseUrl, "baseUrl不能为null");
        Assert.notNull(resourceType, "resourceType不能为null");
        this.baseUrl = baseUrl;
        try {
            this.constructor = resourceType.getResourceAnalyzeClass()
                .getDeclaredConstructor(Class.class, Method.class, Object[].class);
            this.constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("系统异常", e);
        }
    }

    /**
     * 构建代理，主要为了缓存
     *
     * @param baseUrl 代理的根目录
     * @return 代理
     */
    public static HTTPProxy build(String baseUrl, ResourceType resourceType) {
        return CACHE.compute(new Key(baseUrl, resourceType), (k, v) -> {
            if (v == null) {
                return new HTTPProxy(baseUrl, resourceType);
            } else {
                return v;
            }
        });
    }

    @Override
    public Object invoke(Object[] params, Invoker invoker, Method method) throws Throwable {
        log.debug("开始代理方法");
        analyze = constructor.newInstance(method.getDeclaringClass(), method, params);

        if (!analyze.isResource()) {
            log.error("方法{}不是资源方法，不能调用", method);
            throw new NotResourceException(method);
        }
        log.debug("开始构建HTTP请求");
        IHttpRequestBase request = build();
        log.debug("开始发送HTTP请求");
        IHttpResponse response = CLIENT.execute(request);
        log.debug("HTTP请求发送完成，HTTP请求状态码为：{}", response.getStatus());
        String result = response.getResult();
        log.debug("HTTP请求结果为：{}", result);
        return JSON_PARSER.readAsObject(result, method.getReturnType());
    }

    /**
     * 构建http请求
     * @return http请求
     */
    private IHttpRequestBase build() {
        ResourceMethod method = analyze.getResourceMethod();
        String prefix = analyze.pathPrefix();
        String name = analyze.pathLast();
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        if (name.startsWith("/")) {
            name = name.replaceFirst("/", "");
        }

        String url = baseUrl + prefix + name;
        IHttpRequestBase request;

        switch (method) {
            case GET:
                request = new IHttpGet(url);
                break;
            case POST:
                request = new IHttpPost(url);
                break;
            default:
                throw new WsException(StringUtils.format("当前不支持的请求类型：[{}]", method));
        }

        ResourceParam[] params = analyze.getParams();

        for (ResourceParam param : params) {
            ResourceParam.Type type = param.getType();
            String value = JSON_PARSER.toJson(param.getParam());
            if (value == null) {
                continue;
            }

            switch (type) {
                case PATH:
                    request.addPathParam(param.getName(), value);
                    break;
                case FORM:
                    request.addFormParam(param.getName(), value);
                    break;
                case QUERY:
                    request.addQueryParam(param.getName(), value);
                    break;
                case HEADER:
                    request.addHeader(param.getName(), value);
                    break;
                case CONTEXT:
                    break;
                case JSON:
                    request.setEntity(value);
                    break;
                default:
                    throw new WsException(String.format("未知参数类型[{}]", type));
            }
        }

        return request;
    }

    @Data
    @AllArgsConstructor
    private static class Key {
        private String       baseUrl;
        private ResourceType type;
    }
}