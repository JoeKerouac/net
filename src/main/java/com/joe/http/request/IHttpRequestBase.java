package com.joe.http.request;

import com.joe.http.config.IHttpConfig;
import lombok.Data;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 默认content-type为json格式，如果调用addFormParam方法那么将会更改为form格式
 *
 * @author joe
 */
@Data
public abstract class IHttpRequestBase {
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    public static final String CHARSET = "UTF8";
    // Http配置
    private IHttpConfig httpConfig;
    // contentType，默认json
    private String contentType;
    // 请求URL
    private String url;
    // 请求头
    private Map<String, String> headers;
    // URL参数
    private Map<String, String> queryParams;
    private Map<String, Object> formParam;
    // 请求
    private String charset;
    // 请求body，如果请求方法是get的话自动忽略该字段
    private String entity;

    public IHttpRequestBase(String url) {
        this.url = url;
        this.headers = new HashMap<>();
        this.headers.putAll(parse(url));
        this.queryParams = new HashMap<>();
        this.contentType = CONTENT_TYPE_JSON;
        this.httpConfig = new IHttpConfig();
        this.charset = Charset.defaultCharset().name();
    }

    /**
     * 添加请求头
     *
     * @param key   键
     * @param value 值
     */
    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    /**
     * 添加URL参数
     *
     * @param key   参数键
     * @param value 参数值
     */
    public void addQueryParam(String key, String value) {
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
            entity += tag;
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
    public Map<String, String> parse(String url) {
        int index = url.indexOf("?");
        if (index > -1) {
            String data = url.substring(index);
            Map<String, String> map = new HashMap<>();
            Arrays.asList(data.split("&")).stream().forEach(str -> {
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
}
