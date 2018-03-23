package com.joe.http;

import com.joe.http.client.IHttpClient;
import com.joe.http.config.IHttpConfig;
import com.joe.http.request.IHttpGet;
import com.joe.http.request.IHttpPost;
import com.joe.http.request.IHttpRequestBase;
import com.joe.http.response.IHttpResponse;

import java.io.IOException;
import java.io.InputStream;

/**
 * Http请求工具类，方便发起请求
 *
 * @author joe
 */
public class IHttpClientUtil {
    private static final IHttpClient defaultClient = IHttpClient.builder().build();
    private final IHttpClient client;

    public IHttpClientUtil() {
        this(null);
    }

    public IHttpClientUtil(IHttpClient client) {
        this.client = client == null ? defaultClient : client;
    }

    /**
     * 获取cookie
     *
     * @param name cookie名
     * @return cookie的值
     */
    public String getCookie(String name) {
        return this.client.getCookie(name).getValue();
    }

    /**
     * 获取该IHttpClientUtil对应的IHttpClient
     *
     * @return 该IHttpClientUtil对应的IHttpClient
     */
    public IHttpClient getClient() {
        return this.client;
    }

    /**
     * 执行GET请求
     *
     * @param url 请求地址
     * @return 请求结果字符串
     * @throws IOException IO异常
     */
    public String executeGet(String url) throws IOException {
        return executeGet(url, null);
    }

    /**
     * 执行GET请求获取一个输入流
     *
     * @param url 请求地址
     * @return 结果流
     * @throws IOException IO异常
     */
    public InputStream executeGetAsStream(String url) throws IOException {
        IHttpGet get = new IHttpGet(url);
        return executeAsStream(get);
    }

    /**
     * 执行GET请求
     *
     * @param url           请求地址
     * @param resultCharset 请求结果编码
     * @return 请求结果字符串
     * @throws IOException IO异常
     */
    public String executeGet(String url, String resultCharset) throws IOException {
        return executeGet(url, resultCharset, null);
    }

    /**
     * 执行GET请求
     *
     * @param url           请求地址
     * @param resultCharset 请求结果编码
     * @param contentType   请求content-type
     * @return 请求结果字符串
     * @throws IOException IO异常
     */
    public String executeGet(String url, String resultCharset, String contentType)
            throws IOException {
        IHttpGet get = new IHttpGet(url);
        get.setContentType(contentType == null ? IHttpRequestBase.CONTENT_TYPE_JSON : contentType);
        return execute(get, resultCharset);
    }

    /**
     * 执行POST请求
     *
     * @param url  请求地址
     * @param data 请求body数据
     * @return 请求结果字符串
     * @throws IOException IO异常
     */
    public String executePost(String url, String data) throws IOException {
        return executePost(url, data, null);
    }

    /**
     * 执行POST请求
     *
     * @param url           请求地址
     * @param data          请求body数据
     * @param resultCharset 请求结果编码
     * @return 请求结果字符串
     * @throws IOException IO异常
     */
    public String executePost(String url, String data, String resultCharset)
            throws IOException {
        return executePost(url, data, resultCharset, null);
    }

    /**
     * 执行POST请求
     *
     * @param url            请求地址
     * @param data           请求body数据
     * @param resultCharset  请求body数据编码
     * @param requestCharset 请求结果编码
     * @return 请求结果字符串
     * @throws IOException IO异常
     */
    public String executePost(String url, String data, String resultCharset, String requestCharset)
            throws IOException {
        return executePost(url, data, resultCharset, requestCharset, null);
    }

    /**
     * 执行POST请求
     *
     * @param url            请求地址
     * @param data           请求body数据
     * @param resultCharset  请求body数据编码
     * @param requestCharset 请求结果编码
     * @param contentType    请求content-type
     * @return 请求结果字符串
     * @throws IOException IO异常
     */
    public String executePost(String url, String data, String resultCharset, String requestCharset, String contentType)
            throws IOException {
        IHttpPost post = new IHttpPost(url);
        post.setEntity(data);
        post.setCharset(requestCharset == null ? IHttpRequestBase.CHARSET : requestCharset);
        post.setContentType(contentType == null ? IHttpRequestBase.CONTENT_TYPE_JSON : contentType);
        return execute(post, resultCharset);
    }

    /**
     * 执行HTTP请求
     *
     * @param request 请求
     * @return 请求结果字符串
     * @throws IOException IO异常
     */
    private InputStream executeAsStream(IHttpRequestBase request) throws IOException {
        IHttpConfig config = new IHttpConfig();
        request.setHttpConfig(config);
        return client.execute(request).getResultAsStream();
    }

    /**
     * 执行HTTP请求
     *
     * @param request       请求
     * @param resultCharset 请求结果编码
     * @return 请求结果字符串
     * @throws IOException IO异常
     */
    public String execute(IHttpRequestBase request, String resultCharset) throws IOException {
        IHttpConfig config = new IHttpConfig();
        request.setHttpConfig(config);
        IHttpResponse response = client.execute(request);
        return response.getResult(resultCharset);
    }
}
