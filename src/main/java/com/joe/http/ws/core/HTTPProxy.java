package com.joe.http.ws.core;

import static com.joe.utils.serialize.json.JsonParser.getInstance;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
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
import com.joe.utils.proxy.Interception;
import com.joe.utils.proxy.Invoker;
import com.joe.utils.serialize.json.JsonParser;

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
    private ResourceAnalyze                        analyze;
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
    public Object invoke(Object target, Object[] params, Invoker invoker,
                         Method method) throws Throwable {
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
        IHttpRequestBase.Builder<? extends IHttpRequestBase> requestBuilder;

        switch (method) {
            case GET:
                requestBuilder = IHttpGet.builder(url);
                break;
            case POST:
                requestBuilder = IHttpPost.builder(url);
                break;
            default:
                throw new WsException(StringUtils.format("当前不支持的请求类型：[{}]", method));
        }

        ResourceParam[] params = analyze.getParams();

        Map<String, Object> datas = new HashMap<>();
        Object data = null;

        for (ResourceParam param : params) {
            ResourceParam.Type type = param.getType();
            String value = String.valueOf(param.getParam());
            if (value == null) {
                continue;
            }

            switch (type) {
                case PATH:
                    requestBuilder.pathParam(param.getName(), value);
                    break;
                case FORM:
                    requestBuilder.formParam(param.getName(), value);
                    break;
                case QUERY:
                    requestBuilder.queryParam(param.getName(), value);
                    break;
                case HEADER:
                    requestBuilder.header(param.getName(), value);
                    break;
                case CONTEXT:
                    break;
                case JSON:
                    datas.put(param.getName(), param.getParam());
                    data = param.getParam();
                    break;
                default:
                    throw new WsException(StringUtils.format("未知参数类型[{}]", type));
            }
        }

        if (datas.size() == 1) {
            requestBuilder.entity(JSON_PARSER.toJson(data));
        } else if (datas.size() > 1) {
            requestBuilder.entity(JSON_PARSER.toJson(datas));
        }

        return requestBuilder.build();
    }

    @Data
    @AllArgsConstructor
    private static class Key {
        private String       baseUrl;
        private ResourceType type;
    }
}
