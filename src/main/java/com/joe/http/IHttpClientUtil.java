package com.joe.http;

import java.io.IOException;
import java.io.InputStream;

import com.joe.http.client.IHttpClient;
import com.joe.http.config.IHttpConfig;
import com.joe.http.request.IHttpGet;
import com.joe.http.request.IHttpPost;
import com.joe.http.request.IHttpRequestBase;
import com.joe.http.response.IHttpResponse;
import com.joe.utils.common.string.StringUtils;

/**
 * Http请求工具类，方便发起请求
 *
 * @author joe
 */
public class IHttpClientUtil {
    private static final IHttpClient DEFAULT_CLIENT = IHttpClient.DEFAULT_CLIENT;
    private final IHttpClient        client;

    public IHttpClientUtil() {
        this(null);
    }

    public IHttpClientUtil(IHttpClient client) {
        this.client = client == null ? DEFAULT_CLIENT : client;
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
        return executeAsStream(IHttpGet.builder(url));
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
    public String executeGet(String url, String resultCharset,
                             String contentType) throws IOException {
        return execute(IHttpGet.builder(url).contentType(
            StringUtils.isEmpty(contentType) ? IHttpRequestBase.CONTENT_TYPE_JSON : contentType),
            resultCharset);
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
    public String executePost(String url, String data, String resultCharset) throws IOException {
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
    public String executePost(String url, String data, String resultCharset,
                              String requestCharset) throws IOException {
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
    public String executePost(String url, String data, String resultCharset, String requestCharset,
                              String contentType) throws IOException {
        IHttpRequestBase.Builder<IHttpPost> builder = IHttpPost.builder(url);
        builder.entity(data);
        builder.charset(
            StringUtils.isEmpty(requestCharset) ? IHttpRequestBase.CHARSET : requestCharset);
        builder.contentType(
            StringUtils.isEmpty(contentType) ? IHttpRequestBase.CONTENT_TYPE_JSON : contentType);
        return execute(builder, resultCharset);
    }

    /**
     * 执行HTTP请求
     *
     * @param builder 请求builder
     * @return 请求结果流
     * @throws IOException IO异常
     */
    private InputStream executeAsStream(IHttpRequestBase.Builder<? extends IHttpRequestBase> builder) throws IOException {
        builder.config(new IHttpConfig());
        return client.execute(builder.build()).getResultAsStream();
    }

    /**
     * 执行HTTP请求
     *
     * @param builder       请求builder
     * @param resultCharset 请求结果编码
     * @return 请求结果字符串
     * @throws IOException IO异常
     */
    public String execute(IHttpRequestBase.Builder<? extends IHttpRequestBase> builder,
                          String resultCharset) throws IOException {
        builder.config(new IHttpConfig());
        IHttpResponse response = client.execute(builder.build());
        return response.getResult(resultCharset);
    }

    /**
     * 关闭该工具类，同时会关闭传入该工具类中的Client，不会关闭默认client
     *
     * @throws IOException IOException
     */
    public void close() throws IOException {
        if (client != IHttpClient.DEFAULT_CLIENT) {
            this.client.close();
        }
    }
}
