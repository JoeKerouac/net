package com.joe.http.ws;

import static com.joe.utils.parse.json.JsonParser.getInstance;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.joe.http.client.IHttpClient;
import com.joe.http.request.IHttpGet;
import com.joe.http.request.IHttpPost;
import com.joe.http.request.IHttpRequestBase;
import com.joe.http.response.IHttpResponse;
import com.joe.utils.common.Assert;
import com.joe.utils.common.StringUtils;
import com.joe.utils.parse.json.JsonParser;

import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * cglib的http代理
 *
 * @author joe
 * @version 2018.08.21 14:15
 */
@Slf4j
public class CglibHTTPProxy implements MethodInterceptor {
    private static final JsonParser                  JSON_PARSER = getInstance();
    private static final IHttpClient                 CLIENT      = IHttpClient.builder().build();
    private static final Map<String, CglibHTTPProxy> CACHE       = new HashMap<>();
    private String                                   baseUrl;
    private ResourceAnalyze                          analyze;
    private Constructor<? extends ResourceAnalyze>   constructor;

    private CglibHTTPProxy(String baseUrl, ResourceType resourceType) {
        this.baseUrl = baseUrl;
        String className;
        switch (resourceType) {
            case SPRING:
                className = "com.joe.http.ws.SpringResourceAnalyze";
                break;
            case JERSEY:
                className = "com.joe.http.ws.JerseyResourceAnalyze";
                break;
            default:
                throw new RuntimeException("系统异常");
        }
        try {
            this.constructor = ((Class<? extends ResourceAnalyze>) Class.forName(className))
                .getDeclaredConstructor(Class.class, Object.class, Method.class, Object[].class);
            this.constructor.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException("系统异常", e);
        }
    }

    /**
     * 构建代理，主要为了缓存
     *
     * @param baseUrl 代理的根目录
     * @return 代理
     */
    static CglibHTTPProxy build(String baseUrl, ResourceType resourceType) {
        if (!CglibHTTPProxy.CACHE.containsKey(baseUrl)) {
            synchronized (CACHE) {
                if (!CACHE.containsKey(baseUrl)) {
                    CACHE.put(baseUrl, new CglibHTTPProxy(baseUrl, resourceType));
                }
            }
        }
        return CACHE.get(baseUrl);
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args,
                            MethodProxy proxy) throws Throwable {
        log.debug("开始代理方法");
        analyze = constructor.newInstance(obj.getClass().getSuperclass(), obj, method, args);

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
}
