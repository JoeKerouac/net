package com.joe.http;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.joe.http.client.IHttpClient;
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
        client = IHttpClient.builder().build();
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
