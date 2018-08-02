package com.joe.http.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.config.*;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpConnectionFactory;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultHttpResponseParserFactory;
import org.apache.http.impl.conn.ManagedHttpClientConnectionFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SystemDefaultDnsResolver;
import org.apache.http.impl.io.DefaultHttpRequestWriterFactory;
import org.apache.http.io.HttpMessageParserFactory;
import org.apache.http.io.HttpMessageWriterFactory;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joe.http.config.HttpBaseConfig;
import com.joe.http.config.HttpProxy;
import com.joe.http.config.IHttpClientConfig;
import com.joe.http.request.IHttpGet;
import com.joe.http.request.IHttpPost;
import com.joe.http.request.IHttpRequestBase;
import com.joe.http.response.IHttpResponse;
import com.joe.utils.common.StringUtils;

import lombok.Builder;

/**
 * HttpClient，默认用户代理为火狐
 *
 * @author joe
 */
public class IHttpClient implements AutoCloseable {
    private static Logger                 log = LoggerFactory.getLogger(IHttpClient.class);
    /**
     * 不自动重定向
     */
    private static final RedirectStrategy NOREDIRECT;
    /**
     * 默认客户端
     */
    public static final IHttpClient       DEFAULT_CLIENT;

    static {
        NOREDIRECT = new RedirectStrategy() {
            @Override
            public boolean isRedirected(HttpRequest request, HttpResponse response,
                                        HttpContext context) throws ProtocolException {
                return false;
            }

            @Override
            public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response,
                                              HttpContext context) throws ProtocolException {
                return null;
            }
        };
        DEFAULT_CLIENT = IHttpClient.builder().build();
    }

    /**
     * client ID，用来唯一区分HttpClient
     */
    private String              id;
    /**
     * HttpClient
     */
    private CloseableHttpClient httpClient;
    /**
     * cookie store ，存储cookie
     */
    private CookieStore         cookieStore;
    /**
     * 配置
     */
    private IHttpClientConfig   config;

    /**
     * 指定client配置和cookieStore
     *
     * @param config      client配置
     * @param cookieStore cookieStore
     * @param sslcontext  sslcontext
     * @param noRedirect  如果为true表示客户端将不会自动重定向
     */
    @Builder
    private IHttpClient(IHttpClientConfig config, CookieStore cookieStore, SSLContext sslcontext,
                        boolean noRedirect) {
        this.config = config == null ? new IHttpClientConfig() : config;
        cookieStore = cookieStore == null ? new BasicCookieStore() : cookieStore;
        sslcontext = sslcontext == null ? SSLContexts.createSystemDefault() : sslcontext;
        this.init(this.config, cookieStore, sslcontext, noRedirect);
    }

    /**
     * 执行HTTP请求
     *
     * @param request 请求体
     * @throws IOException IO异常
     */
    public IHttpResponse execute(IHttpRequestBase request) throws IOException {
        if (request == null) {
            log.error("request不能为null");
            return null;
        }
        HttpRequestBase requestBase = null;
        // 构建请求
        if (request instanceof IHttpGet) {
            requestBase = build((IHttpGet) request);
        } else if (request instanceof IHttpPost) {
            requestBase = build((IHttpPost) request);
        }
        // 配置请求
        configure(requestBase, request);
        // 发起请求
        CloseableHttpResponse closeableHttpResponse = this.httpClient.execute(requestBase);
        // 设置响应
        IHttpResponse response = new IHttpResponse(closeableHttpResponse);
        return response;
    }

    public CookieStore getCookieManager() {
        return this.cookieStore;
    }

    /**
     * 获取所有cookie
     *
     * @return 返回cookie列表
     */
    public List<Cookie> getCookies() {
        List<Cookie> cookies = cookieStore.getCookies();
        return cookies;
    }

    /**
     * 获取指定cookie
     *
     * @param name cookie名
     * @return cookie存在时返回cookie，不存在时返回null
     */
    public Cookie getCookie(String name) {
        List<Cookie> cookies = cookieStore.getCookies();
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return cookie;
            }
        }
        return null;
    }

    /**
     * 添加cookie
     *
     * @param cookie 要添加的cookie
     */
    public void addCookie(Cookie cookie) {
        cookieStore.addCookie(cookie);
    }

    /**
     * 获取httpClient的ID
     *
     * @return client的ID
     */
    public String getId() {
        return this.id;
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }

    @Override
    protected void finalize() throws Throwable {
        //如果用户没有正常关闭那么在finalize方法中关闭，由于该方法调用时机的不确定性，所以用户应该自己关闭
        //而不是依赖该方法关闭资源。
        close();
    }

    /**
     * 执行GET请求
     *
     * @param request 请求体
     */
    private HttpRequestBase build(IHttpGet request) {
        HttpGet get = new HttpGet(buildUrl(request));
        log.debug("要请求的地址为：{}", get.getURI());
        return get;

    }

    /**
     * 从请求中构建URL（因为请求可能包含query参数）
     *
     * @param request 请求
     * @return 构建完成的URL
     */
    private String buildUrl(IHttpRequestBase request) {
        Map<String, String> query = request.getQueryParams();
        if (query != null && !query.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            query.forEach((key, value) -> sb.append("&" + key + "=" + value));
            return request.getUrl() + "?" + sb.toString().substring(1);
        } else {
            return request.getUrl();
        }
    }

    /**
     * 执行POST请求
     *
     * @param request 请求体
     */
    private HttpRequestBase build(IHttpPost request) {
        HttpPost post = new HttpPost(buildUrl(request));
        StringEntity entity = new StringEntity(request.getEntity(), request.getCharset());
        if (!StringUtils.isEmpty(request.getContentType())) {
            entity.setContentType(request.getContentType());
        }
        post.setEntity(entity);
        log.debug("要请求的地址为：{}；要发送的内容为：{}", request.getUrl(), request.getEntity());
        return post;
    }

    /**
     * 配置HTTP请求（根据iRequest配置request）
     *
     * @param request  要配置的request
     * @param iRequest 配置来源
     */
    private void configure(HttpRequestBase request, IHttpRequestBase iRequest) {
        // 设置
        HttpBaseConfig config = iRequest.getHttpConfig() == null ? this.config
            : iRequest.getHttpConfig();

        //请求配置
        request.setConfig(buildRequestConfig(config.getSocketTimeout(), config.getConnectTimeout(),
            config.getConnectionRequestTimeout()));

        log.debug("请求socketTimeout为：{}；connectTimeout为：{}；connectionRequestTimeout为：{}",
            config.getSocketTimeout(), config.getConnectTimeout(),
            config.getConnectionRequestTimeout());
        // 设置请求头
        Map<String, String> headers = iRequest.getHeaders();
        for (Map.Entry<String, String> entity : headers.entrySet()) {
            request.addHeader(entity.getKey(), entity.getValue());
        }
        // 设置content-type
        if (!StringUtils.isEmpty(iRequest.getContentType())) {
            request.addHeader(HTTP.CONTENT_TYPE,
                ContentType.create(iRequest.getContentType(), iRequest.getCharset()).toString());
        }
        log.debug("请求content-type为：{}；请求头集合为：{}", iRequest.getContentType(), iRequest.getHeaders());
    }

    /**
     * 构建HttpClient请求配置
     *
     * @param socketTimeout            传输超时（单位：毫秒）
     * @param connectTimeout           连接超时（单位：毫秒）
     * @param connectionRequestTimeout 请求超时（单位：毫秒）
     * @return
     */
    private RequestConfig buildRequestConfig(int socketTimeout, int connectTimeout,
                                             int connectionRequestTimeout) {
        log.debug("构建请求配置");
        log.debug("请求socket超时时间为：" + socketTimeout + "ms；connect超时时间为：" + connectTimeout
                  + "ms；connectionRequest超时时间为：" + connectionRequestTimeout + "ms");
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(socketTimeout)
            .setConnectTimeout(connectTimeout).setConnectionRequestTimeout(connectionRequestTimeout)
            .build();
        log.debug("请求配置构建成功");
        return requestConfig;
    }

    /**
     * 初始化httpClient和CookieStore
     *
     * @param config client配置信息
     */
    private void init(IHttpClientConfig config, CookieStore cookieStore, SSLContext sslcontext,
                      boolean noRedirect) {
        log.debug("正在初始化HttpClient");
        CloseableHttpClient httpclient;
        // 自定义解析，选择默认解析
        HttpMessageParserFactory<HttpResponse> responseParserFactory = new DefaultHttpResponseParserFactory();
        HttpMessageWriterFactory<HttpRequest> requestWriterFactory = new DefaultHttpRequestWriterFactory();

        // 利用ParserFactory创建连接工厂
        HttpConnectionFactory<HttpRoute, ManagedHttpClientConnection> connFactory = new ManagedHttpClientConnectionFactory(
            requestWriterFactory, responseParserFactory);

        // 注册协议
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
            .<ConnectionSocketFactory> create()
            .register("http", PlainConnectionSocketFactory.INSTANCE)
            .register("https", new SSLConnectionSocketFactory(sslcontext)).build();

        // 自定义DNS
        DnsResolver dnsResolver = new SystemDefaultDnsResolver() {

            @Override
            public InetAddress[] resolve(final String host) throws UnknownHostException {
                if ("localhost".equalsIgnoreCase(host)) {
                    return new InetAddress[] { InetAddress
                        .getByAddress(new byte[] { 127, 0, 0, 1 }) };
                } else {
                    return super.resolve(host);
                }
            }

        };

        // 连接池管理
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(
            socketFactoryRegistry, connFactory, dnsResolver);

        int sndBufSize = config.getSndBufSize();
        int rcvBufSize = config.getRcvBufSize();
        // socket配置，不延迟发送
        SocketConfig socketConfig = SocketConfig.custom().setTcpNoDelay(true)
            .setSndBufSize(sndBufSize).setSoKeepAlive(true).setRcvBufSize(rcvBufSize).build();
        log.debug("soket默认设置：sendBufferSize:{};receiveBufferSize:{}", sndBufSize, rcvBufSize);
        // 将socket配置设置为连接池默认配置
        connManager.setDefaultSocketConfig(socketConfig);
        // 暂停活动1S后验证连接
        connManager.setValidateAfterInactivity(1000);

        // 消息容器，初始化消息容器以及消息容器的配置，设置最多200个请求头，请求行长度最大为2000
        MessageConstraints messageConstraints = MessageConstraints.custom().setMaxHeaderCount(200)
            .setMaxLineLength(2000).build();

        Charset charset = config.getCharset();

        // Create connection configuration
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
            .setMalformedInputAction(CodingErrorAction.IGNORE)
            .setUnmappableInputAction(CodingErrorAction.IGNORE).setCharset(charset)
            .setMessageConstraints(messageConstraints).build();
        log.debug("默认连接编码配置为：{}", charset);
        // 设置默认的连接配置
        connManager.setDefaultConnectionConfig(connectionConfig);

        // 设置连接池能够保存的最大连接数量以及对每个站点保持最大的连接数量
        // 当前设置：每个站点最大保持300个连接，连接池总共可以保持3000个连接
        connManager.setMaxTotal(config.getMaxTotal());
        connManager.setDefaultMaxPerRoute(config.getDefaultMaxPerRoute());

        // Create global request configuration
        RequestConfig defaultRequestConfig = RequestConfig.custom()
            .setCookieSpec(CookieSpecs.DEFAULT).setExpectContinueEnabled(true)
            .setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
            .setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC)).build();

        // 根据配置构建httpClient
        HttpClientBuilder builder = HttpClients.custom();
        HttpProxy proxy = config.getProxy();
        if (proxy != null) {
            builder.setProxy(new HttpHost(proxy.getHost(), proxy.getPort()));
        }

        if (noRedirect) {
            log.debug("用户设置不重定向，客户端将不会重定向");
            builder.setRedirectStrategy(NOREDIRECT);
        }

        httpclient = builder.setConnectionManager(connManager).setDefaultCookieStore(cookieStore)
            .setDefaultRequestConfig(defaultRequestConfig).setUserAgent(config.getUserAgent())
            .build();
        log.debug("用户代理为：{}", config.getUserAgent());
        this.httpClient = httpclient;
        this.cookieStore = cookieStore;
        this.id = String.valueOf(System.currentTimeMillis());
        log.debug("HttpClient初始化完毕");
    }
}
