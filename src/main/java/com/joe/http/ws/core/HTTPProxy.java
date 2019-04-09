package com.joe.http.ws.core;

import static com.joe.utils.serialize.json.JsonParser.getInstance;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.joe.http.client.IHttpClient;
import com.joe.http.exception.NetException;
import com.joe.http.request.IHttpGet;
import com.joe.http.request.IHttpPost;
import com.joe.http.request.IHttpRequestBase;
import com.joe.http.response.IHttpResponse;
import com.joe.http.ws.exception.NotResourceException;
import com.joe.http.ws.exception.WsException;
import com.joe.utils.common.Assert;
import com.joe.utils.common.StringUtils;
import com.joe.utils.exception.NoSupportException;
import com.joe.utils.proxy.Interception;
import com.joe.utils.proxy.Invoker;
import com.joe.utils.serialize.Serializer;
import com.joe.utils.serialize.SerializerEnum;
import com.joe.utils.serialize.SerializerFactory;
import com.joe.utils.serialize.json.JsonParser;

import lombok.extern.slf4j.Slf4j;

/**
 * @author JoeKerouac
 * @version $Id: joe, v 0.1 2018年11月13日 22:55 JoeKerouac Exp $
 */
@Slf4j
public class HTTPProxy implements Interception {

    private static final JsonParser                JSON_PARSER = getInstance();

    /**
     * 网站根URL，例如http://127.0.0.1，注意不能以/结尾
     */
    private final String                           baseUrl;

    /**
     * 资源分析
     */
    private volatile ResourceAnalyze               analyze;

    /**
     * 资源分析对应的构造器
     */
    private Constructor<? extends ResourceAnalyze> constructor;

    /**
     * http客户端
     */
    private final IHttpClient                      client;

    /**
     * 接收响应的默认编码集，如果响应指定的有编码集那么将优先使用服务器响应的编码集
     */
    private final String                           responseCharset;

    /**
     * 请求服务器使用的字符集，请求服务器时使用该字符集
     */
    private final String                           requestCharset;

    /**
     * 构造器
     * @param baseUrl 服务器根URL
     * @param resourceType 资源类型
     * @param responseCharset 接收响应的默认编码集，如果响应指定的有编码集那么将优先使用服务器响应的编码集
     * @param requestCharset 请求服务器使用的字符集，请求服务器时使用该字符集
     */
    public HTTPProxy(String baseUrl, ResourceType resourceType, String responseCharset,
                     String requestCharset) {
        Assert.notNull(baseUrl, "baseUrl不能为null");
        Assert.notNull(resourceType, "resourceType不能为null");

        try {
            this.constructor = resourceType.getResourceAnalyzeClass()
                .getDeclaredConstructor(Class.class, Method.class, Object[].class);
            this.constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new NetException("系统异常", e);
        }

        this.baseUrl = baseUrl;
        this.client = IHttpClient.builder().build();
        this.responseCharset = StringUtils.isEmpty(responseCharset)
            ? Charset.defaultCharset().name()
            : responseCharset;
        this.requestCharset = StringUtils.isEmpty(requestCharset) ? Charset.defaultCharset().name()
            : requestCharset;
    }

    @Override
    public Object invoke(Object target, Object[] params, Invoker invoker,
                         Method method) throws Throwable {
        log.debug("开始构建资源分析");

        analyze = constructor.newInstance(method.getDeclaringClass(), method, params);

        if (!analyze.isResource()) {
            log.error("方法{}不是资源方法，不能调用", method);
            throw new NotResourceException(method);
        }
        log.debug("开始构建HTTP请求");
        IHttpRequestBase request = build();

        log.debug("开始发送HTTP请求");
        IHttpResponse response = client.execute(request);
        log.debug("HTTP请求发送完成，HTTP请求状态码为：{}", response.getStatus());
        String result = response.getResult(responseCharset, false);

        log.debug("HTTP请求结果为：{}", result);
        return parseResponse(result, method.getReturnType());
    }

    /**
     * 解析响应
     * @param result 响应结果
     * @param returnType 响应类型
     * @return 响应对象
     */
    private Object parseResponse(String result, Class<?> returnType) {
        String[] responseContentTypes = analyze.getResponseContentTypes();
        if (responseContentTypes != null && responseContentTypes.length > 0) {
            if (responseContentTypes.length > 1) {
                throw new NetException("不支持多种响应content_type");
            }
            String contentType = responseContentTypes[0];
            Serializer serializer;
            if (contentType.startsWith(IHttpRequestBase.CONTENT_TYPE_FORM)) {
                serializer = SerializerFactory.getInstance(SerializerEnum.FORM);
            } else if (contentType.startsWith(IHttpRequestBase.CONTENT_TYPE_JSON)) {
                serializer = SerializerFactory.getInstance(SerializerEnum.JSON);
            } else {
                throw new NoSupportException("不支持的content_type:" + contentType);
            }
            return serializer.read(result, returnType);
        } else {
            return JSON_PARSER.readAsObject(result, returnType);
        }
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
                throw new WsException(StringUtils.format("当前不支持的请求类型：[{0}]", method));
        }

        {
            // 先解析content_type，必须把这个放第一步，因为后续设置参数时可能会更改
            String[] requestContentTypes = analyze.getRequestContentTypes();
            if (requestContentTypes != null && requestContentTypes.length > 0) {
                if (requestContentTypes.length > 1) {
                    throw new NetException("不支持多种请求content_type");
                }
                requestBuilder.contentType(requestContentTypes[0].trim());
            }
        }

        {
            // 设置数据
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
        }

        // 设置字符集
        requestBuilder.charset(requestCharset);

        return requestBuilder.build();
    }
}
