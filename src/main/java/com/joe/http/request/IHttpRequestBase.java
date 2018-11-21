package com.joe.http.request;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.joe.http.client.IHttpClient;
import com.joe.http.config.IHttpConfig;
import com.joe.http.exception.NetException;
import com.joe.http.response.IHttpResponse;
import com.joe.utils.common.Assert;
import com.joe.utils.common.StringUtils;

import lombok.Getter;
import lombok.ToString;

/**
 * 默认content-type为json格式，如果调用addFormParam方法那么将会更改为form格式
 *
 * @author joe
 */
@ToString
@Getter
public abstract class IHttpRequestBase {
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    public static final String CHARSET           = "UTF8";
    /**
     * 发起请求的客户端
     */
    IHttpClient                client;
    /**
     *  Http配置
     */
    IHttpConfig                httpConfig;
    /**
     * contentType，默认json
     */
    String                     contentType;
    /**
     * 请求URL
     */
    String                     url;
    /**
     * 请求头
     */
    Map<String, String>        headers;
    /**
     * URL参数
     */
    Map<String, String>        queryParams;
    /**
     * 请求
     */
    String                     charset;
    /**
     * 请求body，如果请求方法是get的话自动忽略该字段
     */
    String                     entity;

    IHttpRequestBase(String url, IHttpClient client) {
        Assert.notNull(url, "url不能为空");
        this.headers = new HashMap<>();
        this.queryParams = new HashMap<>();
        int index;
        if ((index = url.indexOf("?")) > 0) {
            this.queryParams.putAll(parse(url));
            this.url = url.substring(0, index);
        } else {
            this.url = url;
        }
        this.contentType = CONTENT_TYPE_JSON;
        this.charset = Charset.defaultCharset().name();
        this.client = client == null ? IHttpClient.DEFAULT_CLIENT : client;
    }

    /**
     * 添加请求头
     *
     * @param key   键
     * @param value 值
     */
    protected void addHeader(String key, String value) {
        headers.put(key, value);
    }

    /**
     * 添加path param
     * @param key key（不包含{})
     * @param value value
     */
    protected void addPathParam(String key, String value) {
        this.url = url.replace("{" + key + "}", value);
    }

    /**
     * 添加URL参数
     *
     * @param key   参数键
     * @param value 参数值
     */
    protected void addQueryParam(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key 不能为null");
        }
        queryParams.put(key, value == null ? "" : value);
    }

    /**
     * 添加form数据，如果使用该方法，那么请求的content-type将自动设置为application/x-www-form-urlencoded
     *
     * @param key   key
     * @param value value
     * @return 请求本身
     */
    public IHttpRequestBase addFormParam(String key, String value) {
        String tag = key + "=" + value;
        if (entity == null || entity.isEmpty()) {
            entity = tag;
        } else {
            entity += "&" + tag;
        }
        this.contentType = CONTENT_TYPE_FORM;
        return this;
    }

    /**
     * 解析URL，从URL中解析参数
     *
     * @param url url
     * @return 解析出来的参数列表
     */
    public static Map<String, String> parse(String url) {
        int index = url.indexOf("?");
        if (index > 0) {
            String data = url.substring(index + 1);
            Map<String, String> map = new HashMap<>();
            Arrays.asList(data.split("&")).forEach(str -> {
                String[] params = str.split("=");
                if (params.length >= 2) {
                    map.put(params[0], params[1]);
                } else if (params.length == 1) {
                    map.put(params[0], "");
                }
            });
            return map;
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * 执行网络请求
     * @return 请求结果
     * @throws IOException 网络IO异常
     */
    public IHttpResponse exec() throws IOException {
        return client.execute(this);
    }

    /**
     * 请求构建器
     */
    public static abstract class Builder<T extends IHttpRequestBase> {
        /**
         * Http配置
         */
        IHttpConfig         httpConfig;
        /**
         * contentType，默认json
         */
        String              contentType;
        /**
         * 请求URL
         */
        String              url;
        /**
         * 请求头
         */
        Map<String, String> headers;
        /**
         * URL参数
         */
        Map<String, String> queryParams;
        /**
         * form参数
         */
        Map<String, String> formParam;
        /**
         * 请求
         */
        String              charset;
        /**
         * 请求body，如果请求方法是get的话自动忽略该字段
         */
        String              entity;
        /**
         * 请求client
         */
        IHttpClient         client;

        protected Builder(String url) {
            Assert.notNull(url, "url不能为null");
            this.url = url;
            this.headers = new HashMap<>();
            this.queryParams = new HashMap<>();
            this.formParam = new HashMap<>();
        }

        /**
         * 设置请求配置
         *
         * @param config 请求配置
         * @return builder
         */
        public Builder<T> config(IHttpConfig config) {
            this.httpConfig = config;
            return this;
        }

        /**
         * 设置content-type
         *
         * @param contentType content-type
         * @return builder
         */
        public Builder<T> contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * 添加path param
         * @param key 要替换的key
         * @param value 对应的value
         */
        public void pathParam(String key, String value) {
            this.url = url.replace("{" + key + "}", value);
        }

        /**
         * 增加header
         *
         * @param key   key
         * @param value value
         * @return builder
         */
        public Builder<T> header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        /**
         * 增加query param
         *
         * @param key   key
         * @param value value
         * @return builder
         */
        public Builder<T> queryParam(String key, String value) {
            queryParams.put(key, value);
            return this;
        }

        /**
         * 设置form param
         *
         * @param key   key
         * @param value value
         * @return builder
         */
        public Builder<T> formParam(String key, String value) {
            formParam.put(key, value);
            this.contentType = CONTENT_TYPE_FORM;
            return this;
        }

        /**
         * 设置编码
         *
         * @param charset 编码
         * @return builder
         */
        public Builder<T> charset(String charset) {
            Charset.forName(charset);
            this.charset = charset;
            return this;
        }

        /**
         * 设置请求body
         *
         * @param entity body
         * @return builder
         */
        public Builder<T> entity(String entity) {
            this.entity = entity;
            return this;
        }

        public Builder<T> client(IHttpClient client) {
            this.client = client;
            return this;
        }

        /**
         * 构建实际的request，由子类实现
         *
         * @return 构建好后的request
         */
        public abstract T build();

        /**
         * 配置request
         *
         * @param request request
         * @return 配置好后的request
         */
        protected T configure(T request) {
            request.client = client;
            request.httpConfig = httpConfig;
            request.contentType = StringUtils.isEmpty(contentType) ? CONTENT_TYPE_JSON
                : contentType;
            headers.forEach(request::addHeader);
            queryParams.forEach(request::addQueryParam);

            if (!formParam.isEmpty() && !StringUtils.isEmpty(entity)) {
                throw new NetException("form data 和 entity不能同时存在");
            }
            if (formParam.isEmpty()) {
                request.entity = entity == null ? "" : entity;
            } else {
                formParam.forEach(request::addFormParam);
            }

            request.charset = StringUtils.isEmpty(charset) ? Charset.defaultCharset().name()
                : charset;
            return request;
        }
    }
}
