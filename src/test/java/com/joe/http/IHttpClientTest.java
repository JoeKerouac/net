package com.joe.http;

import java.nio.charset.Charset;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.joe.http.client.IHttpClient;
import com.joe.http.config.IHttpClientConfig;
import com.joe.http.request.IHttpGet;
import com.joe.http.request.IHttpPost;

/**
 * @author joe
 * @version 2018.04.28 15:48
 */
public class IHttpClientTest {
    private static final String url = "http://baidu.com";
    private IHttpClient         client;

    @Before
    public void init() {
        IHttpClientConfig config = new IHttpClientConfig();
        config.setCharset(Charset.defaultCharset());
        config.setRcvBufSize(1024);
        config.setSndBufSize(1024);
        config.setConnectionRequestTimeout(1000 * 5);
        config.setSocketTimeout(1000 * 60);
        config.setConnectTimeout(1000 * 30);
        client = IHttpClient.builder().config(config).build();
    }

    @After
    public void destroy() throws Exception {
        client.close();
    }

    @Test
    public void executeGet() {
        IHttpGet get = IHttpGet.builder().url(url).charset("utf8").build();
        try {
            int status = client.execute(get).getStatus();
            Assert.assertEquals("请求异常", 200, status);
        } catch (Exception e) {
            Assert.assertNull("请求异常", e);
        }
    }

    @Test
    public void executePost() {
        IHttpPost post = IHttpPost.builder().url(url).charset("utf8").build();
        try {
            int status = client.execute(post).getStatus();
            Assert.assertEquals("请求异常", 302, status);
        } catch (Exception e) {
            Assert.assertNull("请求异常", e);
        }
    }

}
